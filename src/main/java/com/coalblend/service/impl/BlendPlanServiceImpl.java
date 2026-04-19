package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.dto.BlendGenerateDTO;
import com.coalblend.entity.BlendPlan;
import com.coalblend.entity.BlendPlanDetail;
import com.coalblend.entity.CaseSample;
import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.Inventory;
import com.coalblend.entity.Orders;
import com.coalblend.entity.RuleKnowledge;
import com.coalblend.mapper.BlendPlanDetailMapper;
import com.coalblend.mapper.BlendPlanMapper;
import com.coalblend.mapper.CaseSampleMapper;
import com.coalblend.mapper.CoalQualityMapper;
import com.coalblend.mapper.CoalTypeMapper;
import com.coalblend.mapper.InventoryMapper;
import com.coalblend.mapper.OrdersMapper;
import com.coalblend.mapper.RuleKnowledgeMapper;
import com.coalblend.service.BlendPlanService;
import com.coalblend.vo.blend.BlendGenerateResultVO;
import com.coalblend.vo.blend.PlanDetailVO;
import com.coalblend.vo.blend.PlanWithDetailsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlendPlanServiceImpl implements BlendPlanService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BlendPlanMapper blendPlanMapper;
    private final BlendPlanDetailMapper blendPlanDetailMapper;
    private final OrdersMapper ordersMapper;
    private final InventoryMapper inventoryMapper;
    private final CoalTypeMapper coalTypeMapper;
    private final CoalQualityMapper coalQualityMapper;
    private final RuleKnowledgeMapper ruleKnowledgeMapper;
    private final CaseSampleMapper caseSampleMapper;

    @Override
    public IPage<BlendPlan> page(long current, long size, Long orderId, String planStatus, String planCode,
                                 String orderCode, String createTimeBegin, String createTimeEnd) {
        Page<BlendPlan> page = new Page<>(current, size);
        LambdaQueryWrapper<BlendPlan> w = new LambdaQueryWrapper<>();
        if (orderId != null) {
            w.eq(BlendPlan::getOrderId, orderId);
        }
        if (StringUtils.hasText(planStatus)) {
            w.eq(BlendPlan::getPlanStatus, planStatus);
        }
        if (StringUtils.hasText(planCode)) {
            w.like(BlendPlan::getPlanCode, planCode);
        }
        if (StringUtils.hasText(orderCode)) {
            List<Orders> os = ordersMapper.selectList(
                    new LambdaQueryWrapper<Orders>().eq(Orders::getOrderCode, orderCode));
            if (os.isEmpty()) {
                w.eq(BlendPlan::getOrderId, -1L);
            } else {
                w.eq(BlendPlan::getOrderId, os.get(0).getId());
            }
        }
        if (StringUtils.hasText(createTimeBegin)) {
            w.ge(BlendPlan::getCreateTime, LocalDate.parse(createTimeBegin).atStartOfDay());
        }
        if (StringUtils.hasText(createTimeEnd)) {
            w.le(BlendPlan::getCreateTime, LocalDate.parse(createTimeEnd).atTime(23, 59, 59));
        }
        w.orderByDesc(BlendPlan::getCreateTime);
        return blendPlanMapper.selectPage(page, w);
    }

    @Override
    public BlendPlan getById(Long id) {
        BlendPlan row = blendPlanMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "方案不存在");
        }
        return row;
    }

    @Override
    public List<BlendPlanDetail> listDetails(Long planId) {
        return blendPlanDetailMapper.selectList(new LambdaQueryWrapper<BlendPlanDetail>()
                .eq(BlendPlanDetail::getPlanId, planId)
                .orderByAsc(BlendPlanDetail::getId));
    }

    @Override
    public List<BlendPlan> listByOrder(Long orderId) {
        return blendPlanMapper.selectList(new LambdaQueryWrapper<BlendPlan>()
                .eq(BlendPlan::getOrderId, orderId)
                .orderByDesc(BlendPlan::getCreateTime));
    }

    @Override
    public void selectPlan(Long planId) {
        BlendPlan plan = getById(planId);
        Long orderId = plan.getOrderId();
        blendPlanMapper.update(null, new LambdaUpdateWrapper<BlendPlan>()
                .eq(BlendPlan::getOrderId, orderId)
                .set(BlendPlan::getPlanStatus, "generated"));
        BlendPlan sel = new BlendPlan();
        sel.setId(planId);
        sel.setPlanStatus("selected");
        blendPlanMapper.updateById(sel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlendGenerateResultVO generate(BlendGenerateDTO dto) {
        Orders order = ordersMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        Map<String, Object> constraints = new LinkedHashMap<>();
        constraints.put("demandQuantity", order.getDemandQuantity());
        constraints.put("maxAsh", order.getTargetAsh());
        constraints.put("maxSulfur", order.getTargetSulfur());
        constraints.put("maxMoisture", order.getTargetMoisture());
        constraints.put("referenceVolatile", order.getTargetVolatile());
        constraints.put("minCalorific", order.getTargetCalorific());
        constraints.put("priorityLevel", order.getPriorityLevel());

        List<Inventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getStatus, 1)
                .gt(Inventory::getAvailableQuantity, BigDecimal.ZERO));

        Map<Long, Inventory> bestInv = new LinkedHashMap<>();
        for (Inventory inv : inventories) {
            bestInv.merge(inv.getCoalId(), inv, (a, b) ->
                    a.getAvailableQuantity().compareTo(b.getAvailableQuantity()) >= 0 ? a : b);
        }

        Map<Long, CoalType> typeMap = coalTypeMapper.selectList(new LambdaQueryWrapper<CoalType>())
                .stream()
                .filter(c -> c.getBlendableFlag() != null && c.getBlendableFlag() == 1)
                .collect(Collectors.toMap(CoalType::getId, c -> c, (a, b) -> a));

        List<Long> candidateCoalIds = bestInv.keySet().stream()
                .filter(typeMap::containsKey)
                .collect(Collectors.toList());

        Map<Long, CoalQuality> qualityMap = new LinkedHashMap<>();
        for (Long coalId : candidateCoalIds) {
            CoalQuality q = coalQualityMapper.selectOne(new LambdaQueryWrapper<CoalQuality>()
                    .eq(CoalQuality::getCoalId, coalId)
                    .eq(CoalQuality::getStatus, 1)
                    .orderByDesc(CoalQuality::getSampleTime)
                    .last("limit 1"));
            if (q != null) {
                qualityMap.put(coalId, q);
            }
        }

        List<Long> ranked = candidateCoalIds.stream()
                .filter(qualityMap::containsKey)
                .sorted(Comparator.comparing(cid -> nz(qualityMap.get(cid).getSulfurContent())))
                .limit(3)
                .collect(Collectors.toList());

        if (ranked.size() < 2) {
            throw new BusinessException(400, "可用煤种或煤质数据不足，无法生成方案");
        }

        long ts = System.currentTimeMillis();
        List<BigDecimal> r1 = baseRatios(ranked.size(), true);
        List<BigDecimal> r2 = baseRatios(ranked.size(), false);

        ScoredPlan sp1 = buildAndPersist(order, ranked, r1, ts + "A", "推荐方案-A", dto.getCreateBy(), typeMap, qualityMap, bestInv);
        ScoredPlan sp2 = buildAndPersist(order, ranked, r2, ts + "B", "候选方案-B", dto.getCreateBy(), typeMap, qualityMap, bestInv);

        ScoredPlan best = sp1.overall.compareTo(sp2.overall) >= 0 ? sp1 : sp2;
        ScoredPlan other = best == sp1 ? sp2 : sp1;

        List<RuleKnowledge> rules = ruleKnowledgeMapper.selectList(new LambdaQueryWrapper<RuleKnowledge>()
                .eq(RuleKnowledge::getStatus, 1)
                .orderByDesc(RuleKnowledge::getPriorityLevel)
                .last("limit 8"));
        List<CaseSample> cases = caseSampleMapper.selectList(new LambdaQueryWrapper<CaseSample>()
                .eq(CaseSample::getStatus, 1)
                .orderByDesc(CaseSample::getId)
                .last("limit 5"));

        Orders orderPatch = new Orders();
        orderPatch.setId(order.getId());
        orderPatch.setOrderStatus("generated");
        ordersMapper.updateById(orderPatch);

        BlendGenerateResultVO vo = new BlendGenerateResultVO();
        vo.setOrder(ordersMapper.selectById(order.getId()));
        vo.setConstraints(constraints);
        vo.setRecommendedPlan(toVo(best.planId, typeMap));
        vo.setCandidatePlans(List.of(toVo(other.planId, typeMap)));
        vo.setMatchedRules(rules);
        vo.setMatchedCases(cases);
        vo.setExplainSummary("本结果为规则筛选与线性加权评分后的简化方案，用于毕设演示；后续可接入优化算法或大模型解释。");
        return vo;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.valueOf(Double.MAX_VALUE) : v;
    }

    private List<BigDecimal> baseRatios(int n, boolean first) {
        List<BigDecimal> raw = new ArrayList<>();
        if (n >= 3) {
            if (first) {
                raw.add(new BigDecimal("0.5"));
                raw.add(new BigDecimal("0.3"));
                raw.add(new BigDecimal("0.2"));
            } else {
                raw.add(new BigDecimal("0.4"));
                raw.add(new BigDecimal("0.4"));
                raw.add(new BigDecimal("0.2"));
            }
        } else {
            if (first) {
                raw.add(new BigDecimal("0.6"));
                raw.add(new BigDecimal("0.4"));
            } else {
                raw.add(new BigDecimal("0.5"));
                raw.add(new BigDecimal("0.5"));
            }
        }
        BigDecimal sum = raw.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return raw.stream().map(b -> b.divide(sum, 6, RoundingMode.HALF_UP)).collect(Collectors.toList());
    }

    private ScoredPlan buildAndPersist(Orders order, List<Long> coalIds, List<BigDecimal> ratios, String planCode,
                                       String planName, Long createBy, Map<Long, CoalType> typeMap,
                                       Map<Long, CoalQuality> qualityMap, Map<Long, Inventory> invMap) {
        BigDecimal wAsh = BigDecimal.ZERO;
        BigDecimal wS = BigDecimal.ZERO;
        BigDecimal wM = BigDecimal.ZERO;
        BigDecimal wV = BigDecimal.ZERO;
        BigDecimal wCal = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (int i = 0; i < coalIds.size(); i++) {
            Long cid = coalIds.get(i);
            BigDecimal r = ratios.get(i);
            CoalQuality q = qualityMap.get(cid);
            CoalType t = typeMap.get(cid);
            BigDecimal useQty = order.getDemandQuantity().multiply(r).setScale(2, RoundingMode.HALF_UP);
            BigDecimal price = t.getPurchasePrice() == null ? BigDecimal.ZERO : t.getPurchasePrice();
            totalCost = totalCost.add(useQty.multiply(price));
            wAsh = wAsh.add(nz(q.getAshContent()).multiply(r));
            wS = wS.add(nz(q.getSulfurContent()).multiply(r));
            wM = wM.add(nz(q.getMoistureContent()).multiply(r));
            wV = wV.add(nz(q.getVolatileContent()).multiply(r));
            wCal = wCal.add(nz(q.getCalorificValue()).multiply(r));
        }

        BigDecimal pAsh = wAsh.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pS = wS.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pM = wM.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pV = wV.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pCal = wCal.setScale(2, RoundingMode.HALF_UP);

        List<BlendPlanDetail> details = new ArrayList<>();
        for (int i = 0; i < coalIds.size(); i++) {
            Long cid = coalIds.get(i);
            BigDecimal r = ratios.get(i);
            CoalType t = typeMap.get(cid);
            BigDecimal useQty = order.getDemandQuantity().multiply(r).setScale(2, RoundingMode.HALF_UP);
            BigDecimal price = t.getPurchasePrice() == null ? BigDecimal.ZERO : t.getPurchasePrice();
            BlendPlanDetail d = new BlendPlanDetail();
            d.setCoalId(cid);
            d.setBlendRatio(r);
            d.setUseQuantity(useQty);
            d.setPredictedAsh(pAsh);
            d.setPredictedSulfur(pS);
            d.setPredictedMoisture(pM);
            d.setPredictedVolatile(pV);
            d.setPredictedCalorific(pCal);
            d.setUnitCost(price);
            details.add(d);
        }

        BigDecimal qualityScore = scoreQuality(order, pAsh, pS, pM, pCal);
        BigDecimal costScore = scoreCost(totalCost, order.getDemandQuantity());
        BigDecimal stabilityScore = new BigDecimal("78.0");
        BigDecimal overall = qualityScore.add(costScore).add(stabilityScore)
                .divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);

        BlendPlan plan = new BlendPlan();
        plan.setPlanCode("P" + planCode);
        plan.setOrderId(order.getId());
        plan.setPlanName(planName);
        plan.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));
        plan.setQualityScore(qualityScore);
        plan.setCostScore(costScore);
        plan.setStabilityScore(stabilityScore);
        plan.setOverallScore(overall);
        plan.setPlanStatus("generated");
        plan.setExplanation("加权预测灰分 " + pAsh + "%，硫分 " + pS + "%，发热量 " + pCal.setScale(0, RoundingMode.HALF_UP) + "。");
        plan.setRiskTip(invMap.values().stream().anyMatch(v -> v.getAvailableQuantity().compareTo(new BigDecimal("3000")) < 0)
                ? "部分煤种库存偏紧，请关注执行风险。" : null);
        plan.setCreateBy(createBy);
        blendPlanMapper.insert(plan);
        for (BlendPlanDetail d : details) {
            d.setPlanId(plan.getId());
            blendPlanDetailMapper.insert(d);
        }
        return new ScoredPlan(plan.getId(), overall);
    }

    private BigDecimal scoreQuality(Orders order, BigDecimal ash, BigDecimal s, BigDecimal m, BigDecimal cal) {
        BigDecimal score = HUNDRED;
        if (order.getTargetSulfur() != null && s.compareTo(order.getTargetSulfur()) > 0) {
            score = score.subtract(new BigDecimal("25"));
        }
        if (order.getTargetAsh() != null && ash.compareTo(order.getTargetAsh()) > 0) {
            score = score.subtract(new BigDecimal("15"));
        }
        if (order.getTargetCalorific() != null && cal.compareTo(order.getTargetCalorific()) < 0) {
            score = score.subtract(new BigDecimal("20"));
        }
        if (order.getTargetMoisture() != null && m.compareTo(order.getTargetMoisture()) > 0) {
            score = score.subtract(new BigDecimal("10"));
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scoreCost(BigDecimal totalCost, BigDecimal demand) {
        if (demand == null || demand.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("70.0");
        }
        BigDecimal unit = totalCost.divide(demand, 4, RoundingMode.HALF_UP);
        BigDecimal ref = new BigDecimal("500");
        BigDecimal ratio = ref.divide(unit.max(new BigDecimal("1")), 4, RoundingMode.HALF_UP);
        BigDecimal s = ratio.multiply(new BigDecimal("80")).min(HUNDRED).max(new BigDecimal("40"));
        return s.setScale(2, RoundingMode.HALF_UP);
    }

    private PlanWithDetailsVO toVo(Long planId, Map<Long, CoalType> typeMap) {
        BlendPlan p = blendPlanMapper.selectById(planId);
        List<BlendPlanDetail> raw = blendPlanDetailMapper.selectList(new LambdaQueryWrapper<BlendPlanDetail>()
                .eq(BlendPlanDetail::getPlanId, planId));
        List<PlanDetailVO> rows = new ArrayList<>();
        for (BlendPlanDetail d : raw) {
            PlanDetailVO v = new PlanDetailVO();
            v.setId(d.getId());
            v.setPlanId(d.getPlanId());
            v.setCoalId(d.getCoalId());
            CoalType t = typeMap.get(d.getCoalId());
            v.setCoalName(t == null ? null : t.getCoalName());
            v.setBlendRatio(d.getBlendRatio());
            v.setUseQuantity(d.getUseQuantity());
            v.setPredictedAsh(d.getPredictedAsh());
            v.setPredictedSulfur(d.getPredictedSulfur());
            v.setPredictedMoisture(d.getPredictedMoisture());
            v.setPredictedVolatile(d.getPredictedVolatile());
            v.setPredictedCalorific(d.getPredictedCalorific());
            v.setUnitCost(d.getUnitCost());
            v.setRemark(d.getRemark());
            rows.add(v);
        }
        PlanWithDetailsVO vo = new PlanWithDetailsVO();
        vo.setPlan(p);
        vo.setDetails(rows);
        return vo;
    }

    private record ScoredPlan(Long planId, BigDecimal overall) {
    }
}
