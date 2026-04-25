package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.dto.BlendGenerateDTO;
import com.coalblend.entity.BlendPlan;
import com.coalblend.entity.BlendPlanDetail;
import com.coalblend.dto.knowledge.KnowledgeContextDTO;
import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.Inventory;
import com.coalblend.entity.Orders;
import com.coalblend.mapper.BlendPlanDetailMapper;
import com.coalblend.mapper.BlendPlanMapper;
import com.coalblend.mapper.CoalQualityMapper;
import com.coalblend.mapper.CoalTypeMapper;
import com.coalblend.mapper.InventoryMapper;
import com.coalblend.mapper.OrdersMapper;
import com.coalblend.service.BlendPlanService;
import com.coalblend.service.intelligent.ModelInferenceService;
import com.coalblend.service.intelligent.PlanScoreService;
import com.coalblend.service.intelligent.model.ConstraintResult;
import com.coalblend.service.intelligent.model.EvaluatedPlanDraft;
import com.coalblend.service.intelligent.model.PlanCoalSnapshot;
import com.coalblend.service.intelligent.model.ScoreDetail;
import com.coalblend.service.knowledge.CaseMatchService;
import com.coalblend.service.knowledge.KnowledgeAssembleService;
import com.coalblend.service.knowledge.RuleMatchService;
import com.coalblend.service.rag.RagRetrieveService;
import com.coalblend.service.rag.RagTraceService;
import com.coalblend.vo.AiExplainResultVO;
import com.coalblend.vo.blend.BlendGenerateResultVO;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;
import com.coalblend.vo.blend.PlanDetailVO;
import com.coalblend.vo.blend.PlanWithDetailsVO;
import com.coalblend.vo.rag.RagRetrieveResultVO;
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

    private static final BigDecimal RATIO_STEP = new BigDecimal("0.10");
    private static final int MAX_SHORTLIST_COALS = 5;
    private static final int MAX_RETURN_PLANS = 4;

    private final BlendPlanMapper blendPlanMapper;
    private final BlendPlanDetailMapper blendPlanDetailMapper;
    private final OrdersMapper ordersMapper;
    private final InventoryMapper inventoryMapper;
    private final CoalTypeMapper coalTypeMapper;
    private final CoalQualityMapper coalQualityMapper;
    private final RuleMatchService ruleMatchService;
    private final CaseMatchService caseMatchService;
    private final KnowledgeAssembleService knowledgeAssembleService;
    private final ModelInferenceService modelInferenceService;
    private final PlanScoreService planScoreService;
    private final RagRetrieveService ragRetrieveService;
    private final RagTraceService ragTraceService;

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

        List<PlanCoalSnapshot> shortlisted = candidateCoalIds.stream()
                .filter(qualityMap::containsKey)
                .map(cid -> new PlanCoalSnapshot(cid, typeMap.get(cid), qualityMap.get(cid), bestInv.get(cid)))
                .filter(s -> s.getType() != null && s.getQuality() != null && s.getInventory() != null)
                .sorted(Comparator
                        .comparing((PlanCoalSnapshot s) -> shortlistScore(order, s)).reversed()
                        .thenComparing(s -> nzSort(s.getQuality().getSulfurContent()))
                        .thenComparing(s -> nzSort(s.getType().getPurchasePrice())))
                .limit(MAX_SHORTLIST_COALS)
                .collect(Collectors.toList());

        if (shortlisted.size() < 2) {
            throw new BusinessException(400, "可用煤种或煤质数据不足，无法生成方案");
        }

        List<Long> shortlistedCoalIds = shortlisted.stream()
                .map(PlanCoalSnapshot::getCoalId)
                .collect(Collectors.toList());
        List<MatchedRuleVO> matchedRules = ruleMatchService.match(order, shortlistedCoalIds, bestInv);
        List<MatchedCaseVO> matchedCases = caseMatchService.match(order, shortlistedCoalIds, matchedRules);

        List<EvaluatedPlanDraft> drafts = buildCandidateDrafts(order, shortlisted);
        List<EvaluatedPlanDraft> preferred = drafts.stream()
                .filter(d -> d.getConstraintResult().isFeasible())
                .limit(MAX_RETURN_PLANS)
                .collect(Collectors.toList());
        if (preferred.isEmpty()) {
            preferred = drafts.stream().limit(MAX_RETURN_PLANS).collect(Collectors.toList());
        }
        if (preferred.isEmpty()) {
            throw new BusinessException(400, "未能生成可用配煤方案，请检查煤质和库存数据");
        }

        List<ScoredPlan> persistedPlans = persistPlans(order, preferred, dto.getCreateBy(), typeMap);
        ScoredPlan best = persistedPlans.get(0);
        List<ScoredPlan> others = persistedPlans.subList(1, persistedPlans.size());

        Orders orderPatch = new Orders();
        orderPatch.setId(order.getId());
        orderPatch.setOrderStatus("generated");
        ordersMapper.updateById(orderPatch);

        BlendGenerateResultVO vo = new BlendGenerateResultVO();
        vo.setOrder(ordersMapper.selectById(order.getId()));
        vo.setConstraints(constraints);
        constraints.put("shortlistedCoalCount", shortlisted.size());
        constraints.put("generatedPlanCount", persistedPlans.size());
        constraints.put("feasiblePlanCount", drafts.stream().filter(d -> d.getConstraintResult().isFeasible()).count());
        PlanWithDetailsVO recommended = toVo(best.planId, typeMap);
        vo.setRecommendedPlan(recommended);
        vo.setCandidatePlans(others.stream().map(p -> toVo(p.planId, typeMap)).collect(Collectors.toList()));
        vo.setMatchedRules(matchedRules);
        vo.setMatchedCases(matchedCases);
        KnowledgeContextDTO knowledgeContext = knowledgeAssembleService.assemble(
                order, constraints, typeMap, bestInv, shortlistedCoalIds, matchedRules, matchedCases,
                recommended, order.getDemandQuantity());
        RagRetrieveResultVO ragRetrieveResult = ragRetrieveService.retrieveByOrder(order, 5);
        knowledgeContext.setRagRetrieveResult(ragRetrieveResult);
        knowledgeContext.setRagKnowledgeText(ragRetrieveService.buildKnowledgeText(ragRetrieveResult));
        vo.setKnowledgeContext(knowledgeContext);
        vo.setKnowledgeSummary(knowledgeAssembleService.summarize(knowledgeContext));
        vo.setRagRetrieveResult(ragRetrieveResult);

        AiExplainResultVO ai = modelInferenceService.enrichRecommendedPlan(
                best.planId, order, recommended, matchedRules, matchedCases, knowledgeContext);
        ragTraceService.saveBlendGenerateTrace(best.planId, ragRetrieveResult, ai);
        vo.setRecommendedPlan(toVo(best.planId, typeMap));
        vo.setRagExplanation(ai);
        vo.setExplainSummary(buildAiSummaryLine(ai));
        return vo;
    }

    /**
     * 接口中的摘要字段：完整拼接 AI 三段输出，不做长度截断（前端以滚动区域展示）。
     */
    private static String buildAiSummaryLine(AiExplainResultVO air) {
        if (air == null) {
            return "方案已生成。";
        }
        String prefix = air.isAiGenerated() ? "【大模型已生成解释】" : "【使用兜底说明】";
        String model = StringUtils.hasText(air.getModelNameUsed()) ? ("（" + air.getModelNameUsed() + "）") : "";
        String ex = air.getExplanation() == null ? "" : air.getExplanation().trim();
        String rb = air.getRuleBasis() == null ? "" : air.getRuleBasis().trim();
        String cr = air.getCaseReference() == null ? "" : air.getCaseReference().trim();
        String rr = air.getRecommendReason() == null ? "" : air.getRecommendReason().trim();
        String risk = air.getRiskTip() == null ? "" : air.getRiskTip().trim();
        String opt = air.getOptimizeSuggestion() == null ? "" : air.getOptimizeSuggestion().trim();
        StringBuilder body = new StringBuilder();
        if (StringUtils.hasText(rb)) {
            if (!body.isEmpty()) {
                body.append("\n\n");
            }
            body.append("【规则依据】\n").append(rb);
        }
        if (StringUtils.hasText(cr)) {
            if (!body.isEmpty()) {
                body.append("\n\n");
            }
            body.append("【案例参考】\n").append(cr);
        }
        if (StringUtils.hasText(rr)) {
            if (!body.isEmpty()) {
                body.append("\n\n");
            }
            body.append("【推荐理由】\n").append(rr);
        }
        if (StringUtils.hasText(risk)) {
            if (!body.isEmpty()) {
                body.append("\n\n");
            }
            body.append("【风险提示】\n").append(risk);
        }
        if (StringUtils.hasText(opt)) {
            if (!body.isEmpty()) {
                body.append("\n\n");
            }
            body.append("【优化建议】\n").append(opt);
        }
        if (StringUtils.hasText(ex)) {
            if (!body.isEmpty()) {
                body.append("\n\n");
            }
            body.append("【最终解释】\n").append(ex);
        }
        if (body.isEmpty()) {
            body.append("方案已生成。");
        }
        return prefix + model + "\n" + body;
    }

    private static BigDecimal nzSort(BigDecimal v) {
        return v == null ? BigDecimal.valueOf(Double.MAX_VALUE) : v;
    }

    private BigDecimal shortlistScore(Orders order, PlanCoalSnapshot s) {
        BigDecimal score = BigDecimal.ZERO;
        CoalQuality q = s.getQuality();
        Inventory inv = s.getInventory();
        CoalType t = s.getType();
        if (order.getTargetSulfur() != null && q.getSulfurContent() != null) {
            score = score.add(order.getTargetSulfur().subtract(q.getSulfurContent()).multiply(new BigDecimal("25")));
        }
        if (order.getTargetCalorific() != null && q.getCalorificValue() != null) {
            score = score.add(q.getCalorificValue().subtract(order.getTargetCalorific()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        }
        if (inv.getAvailableQuantity() != null) {
            score = score.add(inv.getAvailableQuantity().divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP));
        }
        if (t.getPurchasePrice() != null) {
            score = score.subtract(t.getPurchasePrice().divide(new BigDecimal("50"), 4, RoundingMode.HALF_UP));
        }
        return score;
    }

    private List<EvaluatedPlanDraft> buildCandidateDrafts(Orders order, List<PlanCoalSnapshot> shortlisted) {
        List<List<PlanCoalSnapshot>> combinations = buildCombinations(shortlisted);
        List<EvaluatedPlanDraft> drafts = new ArrayList<>();
        for (List<PlanCoalSnapshot> combo : combinations) {
            for (List<BigDecimal> ratios : buildRatioTemplates(combo.size())) {
                drafts.add(planScoreService.evaluate(order, combo, ratios));
            }
        }
        drafts.sort(Comparator
                .comparing((EvaluatedPlanDraft d) -> d.getConstraintResult().isFeasible(), Comparator.reverseOrder())
                .thenComparing(d -> d.getScoreDetail().getOverallScore(), Comparator.reverseOrder())
                .thenComparing(EvaluatedPlanDraft::getTotalCost));
        return drafts;
    }

    private List<List<PlanCoalSnapshot>> buildCombinations(List<PlanCoalSnapshot> shortlisted) {
        List<List<PlanCoalSnapshot>> combinations = new ArrayList<>();
        for (int i = 0; i < shortlisted.size(); i++) {
            for (int j = i + 1; j < shortlisted.size(); j++) {
                combinations.add(List.of(shortlisted.get(i), shortlisted.get(j)));
                for (int k = j + 1; k < shortlisted.size(); k++) {
                    combinations.add(List.of(shortlisted.get(i), shortlisted.get(j), shortlisted.get(k)));
                }
            }
        }
        return combinations;
    }

    private List<List<BigDecimal>> buildRatioTemplates(int size) {
        List<List<BigDecimal>> ratios = new ArrayList<>();
        if (size == 2) {
            for (int a = 2; a <= 8; a++) {
                int b = 10 - a;
                ratios.add(List.of(scaleRatio(a), scaleRatio(b)));
            }
            return ratios;
        }
        if (size == 3) {
            for (int a = 2; a <= 6; a++) {
                for (int b = 2; b <= 6; b++) {
                    int c = 10 - a - b;
                    if (c < 2 || c > 6) {
                        continue;
                    }
                    ratios.add(List.of(scaleRatio(a), scaleRatio(b), scaleRatio(c)));
                }
            }
            return ratios;
        }
        return List.of(List.of(BigDecimal.ONE));
    }

    private BigDecimal scaleRatio(int tenths) {
        return new BigDecimal(tenths).multiply(RATIO_STEP).setScale(4, RoundingMode.HALF_UP);
    }

    private List<ScoredPlan> persistPlans(Orders order, List<EvaluatedPlanDraft> drafts, Long createBy,
                                          Map<Long, CoalType> typeMap) {
        long ts = System.currentTimeMillis();
        List<ScoredPlan> persisted = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            EvaluatedPlanDraft draft = drafts.get(i);
            ConstraintResult constraint = draft.getConstraintResult();
            ScoreDetail score = draft.getScoreDetail();
            String suffix = String.valueOf((char) ('A' + i));
            String planName = i == 0 ? "推荐方案-" + suffix : "候选方案-" + suffix;
            BlendPlan plan = new BlendPlan();
            plan.setPlanCode("P" + ts + suffix);
            plan.setOrderId(order.getId());
            plan.setPlanName(planName);
            plan.setTotalCost(draft.getTotalCost());
            plan.setQualityScore(score.getQualityScore());
            plan.setCostScore(score.getCostScore());
            plan.setStabilityScore(score.getStabilityScore());
            plan.setOverallScore(score.getOverallScore());
            plan.setPlanStatus("generated");
            plan.setExplanation(draft.getExplanation());
            plan.setRiskTip(draft.getRiskTip());
            plan.setFeasibleFlag(constraint.isFeasible() ? 1 : 0);
            plan.setConstraintSummary(buildConstraintSummary(constraint));
            plan.setScoreDetail(buildScoreDetailSummary(score));
            plan.setRiskLevel(constraint.riskLevel());
            plan.setCreateBy(createBy);
            blendPlanMapper.insert(plan);
            for (BlendPlanDetail d : draft.getDetails()) {
                d.setPlanId(plan.getId());
                blendPlanDetailMapper.insert(d);
            }
            persisted.add(new ScoredPlan(plan.getId(), score.getOverallScore()));
        }
        return persisted;
    }

    private String buildConstraintSummary(ConstraintResult c) {
        String violations = c.getViolations().isEmpty() ? "无" : String.join("；", c.getViolations());
        String warnings = c.getWarnings().isEmpty() ? "无" : String.join("；", c.getWarnings());
        return "可行性：" + (c.isFeasible() ? "可行" : "不可行")
                + "；预测灰分：" + fmt(c.getPredictedAsh())
                + "%；预测硫分：" + fmt(c.getPredictedSulfur())
                + "%；预测水分：" + fmt(c.getPredictedMoisture())
                + "%；预测热值：" + fmt(c.getPredictedCalorific())
                + "；违反项：" + violations
                + "；风险提示：" + warnings;
    }

    private String buildScoreDetailSummary(ScoreDetail s) {
        return "质量评分：" + fmt(s.getQualityScore()) + "（" + s.getQualityReason() + "）"
                + "；成本评分：" + fmt(s.getCostScore()) + "（" + s.getCostReason() + "）"
                + "；稳定性评分：" + fmt(s.getStabilityScore()) + "（" + s.getStabilityReason() + "）"
                + "；综合评分：" + fmt(s.getOverallScore()) + "（" + s.getOverallReason() + "）";
    }

    private String fmt(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
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
