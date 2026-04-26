package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.common.config.CoalBlendProperties;
import com.coalblend.dto.BlendGenerateDTO;
import com.coalblend.dto.BlendPlanExecuteDTO;
import com.coalblend.entity.BlendPlan;
import com.coalblend.entity.BlendPlanDetail;
import com.coalblend.dto.knowledge.KnowledgeContextDTO;
import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.ExperimentRecord;
import com.coalblend.entity.Inventory;
import com.coalblend.entity.Orders;
import com.coalblend.entity.ProductBatch;
import com.coalblend.mapper.BlendPlanDetailMapper;
import com.coalblend.mapper.BlendPlanMapper;
import com.coalblend.mapper.CoalQualityMapper;
import com.coalblend.mapper.CoalTypeMapper;
import com.coalblend.mapper.InventoryMapper;
import com.coalblend.mapper.OrdersMapper;
import com.coalblend.mapper.ProductBatchMapper;
import com.coalblend.service.BlendPlanService;
import com.coalblend.service.ExperimentRecordService;
import com.coalblend.service.chain.BatchLineageService;
import com.coalblend.service.chain.BatchNoGenerator;
import com.coalblend.service.intelligent.AiBlendCandidateService;
import com.coalblend.service.intelligent.ModelInferenceService;
import com.coalblend.service.intelligent.PlanScoreService;
import com.coalblend.service.intelligent.model.AiBlendCandidateItem;
import com.coalblend.service.intelligent.model.AiBlendCandidatePlan;
import com.coalblend.service.intelligent.model.AiBlendCandidateResult;
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
import com.coalblend.vo.blend.BlendPlanExecuteResultVO;
import com.coalblend.vo.blend.BlendGenerateResultVO;
import com.coalblend.vo.blend.CandidateEvaluationItemVO;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlendPlanServiceImpl implements BlendPlanService {

    private static final BigDecimal RATIO_STEP = new BigDecimal("0.10");
    private static final int MAX_SHORTLIST_COALS = 5;
    private static final int MAX_RETURN_PLANS = 4;
    private static final int MAX_EVALUATED_CANDIDATES_RETURN = 30;

    private final BlendPlanMapper blendPlanMapper;
    private final BlendPlanDetailMapper blendPlanDetailMapper;
    private final OrdersMapper ordersMapper;
    private final InventoryMapper inventoryMapper;
    private final CoalTypeMapper coalTypeMapper;
    private final CoalQualityMapper coalQualityMapper;
    private final ProductBatchMapper productBatchMapper;
    private final RuleMatchService ruleMatchService;
    private final CaseMatchService caseMatchService;
    private final KnowledgeAssembleService knowledgeAssembleService;
    private final AiBlendCandidateService aiBlendCandidateService;
    private final ModelInferenceService modelInferenceService;
    private final PlanScoreService planScoreService;
    private final RagRetrieveService ragRetrieveService;
    private final RagTraceService ragTraceService;
    private final BatchNoGenerator batchNoGenerator;
    private final BatchLineageService batchLineageService;
    private final CoalBlendProperties coalBlendProperties;
    private final ExperimentRecordService experimentRecordService;

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
    public BlendPlanExecuteResultVO execute(BlendPlanExecuteDTO dto) {
        if (dto == null || dto.getPlanId() == null) {
            throw new BusinessException("planId 不能为空");
        }
        BlendPlan plan = getById(dto.getPlanId());
        List<BlendPlanDetail> details = listDetails(plan.getId());
        if (details.isEmpty()) {
            throw new BusinessException("方案明细为空，无法执行");
        }
        BigDecimal totalQty = details.stream()
                .map(BlendPlanDetail::getUseQuantity)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalQty.compareTo(BigDecimal.ZERO) <= 0) {
            Orders order = ordersMapper.selectById(plan.getOrderId());
            totalQty = order == null || order.getDemandQuantity() == null ? BigDecimal.ZERO : order.getDemandQuantity();
        }
        ProductBatch finalProduct = new ProductBatch();
        finalProduct.setProductBatchNo(batchNoGenerator.productBatchNo("mixed_product"));
        finalProduct.setOrderId(plan.getOrderId());
        finalProduct.setPlanId(plan.getId());
        finalProduct.setProductType("mixed_product");
        finalProduct.setProductName(plan.getPlanName() == null ? "最终混配产品" : plan.getPlanName());
        finalProduct.setQuantity(totalQty);
        finalProduct.setAvailableQuantity(totalQty);
        finalProduct.setWarehouseCode(dto.getWarehouseCode());
        finalProduct.setAshContent(weighted(details, BlendPlanDetail::getPredictedAsh));
        finalProduct.setSulfurContent(weighted(details, BlendPlanDetail::getPredictedSulfur));
        finalProduct.setMoistureContent(weighted(details, BlendPlanDetail::getPredictedMoisture));
        finalProduct.setVolatileContent(weighted(details, BlendPlanDetail::getPredictedVolatile));
        finalProduct.setCalorificValue(weighted(details, BlendPlanDetail::getPredictedCalorific));
        finalProduct.setStatus("available");
        finalProduct.setRemark(dto.getRemark());
        productBatchMapper.insert(finalProduct);

        for (BlendPlanDetail d : details) {
            if (d.getProductBatchId() == null && !StringUtils.hasText(d.getProductBatchNo())) {
                continue;
            }
            ProductBatch source = d.getProductBatchId() == null
                    ? productBatchMapper.selectOne(new LambdaQueryWrapper<ProductBatch>()
                    .eq(ProductBatch::getProductBatchNo, d.getProductBatchNo()).last("LIMIT 1"))
                    : productBatchMapper.selectById(d.getProductBatchId());
            if (source == null) {
                continue;
            }
            BigDecimal useQty = d.getUseQuantity() == null ? BigDecimal.ZERO : d.getUseQuantity();
            if (source.getAvailableQuantity() != null && source.getAvailableQuantity().compareTo(useQty) < 0) {
                throw new BusinessException("产品批次库存不足：" + source.getProductBatchNo());
            }
            ProductBatch sourcePatch = new ProductBatch();
            sourcePatch.setId(source.getId());
            sourcePatch.setAvailableQuantity((source.getAvailableQuantity() == null ? BigDecimal.ZERO : source.getAvailableQuantity()).subtract(useQty));
            productBatchMapper.updateById(sourcePatch);
            batchLineageService.record(source.getProductBatchNo(), "product_batch", finalProduct.getProductBatchNo(),
                    "final_product", "blending", useQty, d.getBlendRatio(), dto.getOperatorName(), "产品批次参与最终产品混配");
        }

        BlendPlan patch = new BlendPlan();
        patch.setId(plan.getId());
        patch.setFinalProductBatchNo(finalProduct.getProductBatchNo());
        patch.setTraceStatus("executed");
        patch.setPlanStatus("executed");
        blendPlanMapper.updateById(patch);

        BlendPlanExecuteResultVO vo = new BlendPlanExecuteResultVO();
        vo.setFinalProductBatchNo(finalProduct.getProductBatchNo());
        vo.setOrderId(plan.getOrderId());
        vo.setPlanId(plan.getId());
        vo.setQuantity(totalQty);
        vo.getPredictedQuality().put("ashContent", finalProduct.getAshContent());
        vo.getPredictedQuality().put("sulfurContent", finalProduct.getSulfurContent());
        vo.getPredictedQuality().put("moistureContent", finalProduct.getMoistureContent());
        vo.getPredictedQuality().put("volatileContent", finalProduct.getVolatileContent());
        vo.getPredictedQuality().put("calorificValue", finalProduct.getCalorificValue());
        return vo;
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
        String candidateScope = StringUtils.hasText(dto.getCandidateScope()) ? dto.getCandidateScope() : "coal_type";
        constraints.put("candidateScope", candidateScope);

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

        List<PlanCoalSnapshot> shortlisted = "product_batch".equalsIgnoreCase(candidateScope)
                ? buildProductBatchShortlist(order, typeMap)
                : buildCoalTypeShortlist(order, bestInv, typeMap);

        if (shortlisted.size() < 2) {
            throw new BusinessException(400, "可用候选物料或煤质数据不足，无法生成方案");
        }

        List<Long> shortlistedCoalIds = shortlisted.stream()
                .map(PlanCoalSnapshot::getCoalId)
                .collect(Collectors.toList());
        List<MatchedRuleVO> matchedRules = ruleMatchService.match(order, shortlistedCoalIds, bestInv);
        List<MatchedCaseVO> matchedCases = caseMatchService.match(order, shortlistedCoalIds, matchedRules);
        RagRetrieveResultVO ragRetrieveResult = ragRetrieveService.retrieveByOrder(order, 5);

        AiBlendCandidateResult aiCandidateResult = aiBlendCandidateService.generateCandidates(
                order, shortlisted, matchedRules, matchedCases, ragRetrieveResult, candidateScope);
        List<EvaluatedPlanDraft> aiDrafts = buildAiCandidateDrafts(order, shortlisted, aiCandidateResult);
        List<EvaluatedPlanDraft> systemDrafts = coalBlendProperties.isEnableSystemEnumeration()
                ? buildCandidateDrafts(order, shortlisted)
                : List.of();
        List<EvaluatedPlanDraft> drafts = mergeAndRankDrafts(aiDrafts, systemDrafts);
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

        String experimentCode = StringUtils.hasText(dto.getExperimentCode())
                ? dto.getExperimentCode().trim()
                : buildExperimentCode(order, aiCandidateResult);
        List<ScoredPlan> persistedPlans = persistPlans(order, preferred, dto.getCreateBy(), typeMap,
                experimentCode, aiCandidateResult.getModelName());
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
        constraints.put("experimentCode", experimentCode);
        constraints.put("experimentModelName", aiCandidateResult.getModelName());
        constraints.put("systemEnumerationEnabled", coalBlendProperties.isEnableSystemEnumeration());
        constraints.put("aiCandidatePlanCount", aiCandidateResult.getPlans().size());
        constraints.put("acceptedAiCandidateCount", aiDrafts.size());
        if (StringUtils.hasText(aiCandidateResult.getErrorMessage())) {
            constraints.put("aiCandidateError", aiCandidateResult.getErrorMessage());
        }
        constraints.put("feasiblePlanCount", drafts.stream().filter(d -> d.getConstraintResult().isFeasible()).count());
        PlanWithDetailsVO recommended = toVo(best.planId, typeMap);
        vo.setRecommendedPlan(recommended);
        vo.setCandidatePlans(others.stream().map(p -> toVo(p.planId, typeMap)).collect(Collectors.toList()));
        vo.setAiEvaluatedCandidates(toCandidateEvaluationVos(aiDrafts, typeMap));
        vo.setSystemEvaluatedCandidates(toCandidateEvaluationVos(systemDrafts, typeMap));
        vo.setMatchedRules(matchedRules);
        vo.setMatchedCases(matchedCases);
        vo.setAiCandidateResult(aiCandidateResult);
        KnowledgeContextDTO knowledgeContext = knowledgeAssembleService.assemble(
                order, constraints, typeMap, bestInv, shortlistedCoalIds, matchedRules, matchedCases,
                recommended, order.getDemandQuantity());
        knowledgeContext.setRagRetrieveResult(ragRetrieveResult);
        String chainContext = buildPlanChainContext(best.planId);
        knowledgeContext.setRagKnowledgeText(ragRetrieveService.buildKnowledgeText(ragRetrieveResult)
                + (StringUtils.hasText(chainContext) ? "\n\n【方案批次来源追溯】\n" + chainContext : ""));
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

    private List<PlanCoalSnapshot> buildCoalTypeShortlist(Orders order, Map<Long, Inventory> bestInv,
                                                          Map<Long, CoalType> typeMap) {
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

        return candidateCoalIds.stream()
                .filter(qualityMap::containsKey)
                .map(cid -> new PlanCoalSnapshot(cid, typeMap.get(cid), qualityMap.get(cid), bestInv.get(cid)))
                .filter(s -> s.getType() != null && s.getQuality() != null && s.getInventory() != null)
                .sorted(Comparator
                        .comparing((PlanCoalSnapshot s) -> shortlistScore(order, s)).reversed()
                        .thenComparing(s -> nzSort(s.getQuality().getSulfurContent()))
                        .thenComparing(s -> nzSort(s.getType().getPurchasePrice())))
                .limit(MAX_SHORTLIST_COALS)
                .collect(Collectors.toList());
    }

    private List<PlanCoalSnapshot> buildProductBatchShortlist(Orders order, Map<Long, CoalType> typeMap) {
        List<ProductBatch> products = productBatchMapper.selectList(new LambdaQueryWrapper<ProductBatch>()
                .eq(ProductBatch::getStatus, "available")
                .gt(ProductBatch::getAvailableQuantity, BigDecimal.ZERO)
                .in(ProductBatch::getProductType, List.of("clean_coal", "mixed_product"))
                .orderByDesc(ProductBatch::getAvailableQuantity)
                .orderByDesc(ProductBatch::getId)
                .last("LIMIT 30"));
        return products.stream()
                .filter(p -> p.getCoalId() != null && typeMap.containsKey(p.getCoalId()))
                .filter(p -> productQualityBoundary(order, p))
                .map(p -> toProductSnapshot(p, typeMap.get(p.getCoalId())))
                .filter(s -> s.getType() != null && s.getQuality() != null && s.getInventory() != null)
                .sorted(Comparator
                        .comparing((PlanCoalSnapshot s) -> shortlistScore(order, s)).reversed()
                        .thenComparing(s -> nzSort(s.getQuality().getSulfurContent()))
                        .thenComparing(s -> nzSort(s.getType().getPurchasePrice())))
                .limit(MAX_SHORTLIST_COALS)
                .collect(Collectors.toList());
    }

    private boolean productQualityBoundary(Orders order, ProductBatch p) {
        if (order.getTargetSulfur() != null && p.getSulfurContent() != null
                && p.getSulfurContent().compareTo(order.getTargetSulfur().multiply(new BigDecimal("1.5"))) > 0) {
            return false;
        }
        if (order.getTargetAsh() != null && p.getAshContent() != null
                && p.getAshContent().compareTo(order.getTargetAsh().multiply(new BigDecimal("1.5"))) > 0) {
            return false;
        }
        return true;
    }

    private PlanCoalSnapshot toProductSnapshot(ProductBatch p, CoalType baseType) {
        CoalType t = new CoalType();
        t.setId(baseType.getId());
        t.setCoalCode(baseType.getCoalCode());
        t.setCoalName((StringUtils.hasText(p.getProductName()) ? p.getProductName() : baseType.getCoalName())
                + "（" + p.getProductBatchNo() + "）");
        t.setCoalCategory(StringUtils.hasText(baseType.getCoalCategory()) ? baseType.getCoalCategory() : p.getProductType());
        t.setPurchasePrice(baseType.getPurchasePrice());
        t.setBlendableFlag(1);

        CoalQuality q = new CoalQuality();
        q.setCoalId(p.getCoalId());
        q.setBatchNo(p.getProductBatchNo());
        q.setSampleStage(p.getProductType());
        q.setRelatedBatchNo(p.getProductBatchNo());
        q.setAshContent(p.getAshContent());
        q.setSulfurContent(p.getSulfurContent());
        q.setMoistureContent(p.getMoistureContent());
        q.setVolatileContent(p.getVolatileContent());
        q.setCalorificValue(p.getCalorificValue());
        q.setStatus(1);

        Inventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getProductBatchNo, p.getProductBatchNo())
                .eq(Inventory::getStatus, 1)
                .orderByAsc(Inventory::getId)
                .last("LIMIT 1"));
        if (inv == null) {
            inv = new Inventory();
            inv.setCoalId(p.getCoalId());
            inv.setProductBatchNo(p.getProductBatchNo());
            inv.setWarehouseCode(p.getWarehouseCode());
            inv.setMaterialStage("product_batch");
            inv.setStockQuantity(p.getQuantity());
            inv.setAvailableQuantity(p.getAvailableQuantity());
            inv.setStatus(1);
        }

        PlanCoalSnapshot s = new PlanCoalSnapshot(p.getCoalId(), t, q, inv);
        s.setProductBatchId(p.getId());
        s.setProductBatchNo(p.getProductBatchNo());
        s.setProductBatchName(p.getProductName());
        s.setQualitySnapshotJson(buildQualitySnapshotJson(p));
        return s;
    }

    private String buildQualitySnapshotJson(ProductBatch p) {
        return "{"
                + "\"productBatchNo\":\"" + safeJson(p.getProductBatchNo()) + "\","
                + "\"productType\":\"" + safeJson(p.getProductType()) + "\","
                + "\"ashContent\":" + jsonNum(p.getAshContent()) + ","
                + "\"sulfurContent\":" + jsonNum(p.getSulfurContent()) + ","
                + "\"moistureContent\":" + jsonNum(p.getMoistureContent()) + ","
                + "\"volatileContent\":" + jsonNum(p.getVolatileContent()) + ","
                + "\"calorificValue\":" + jsonNum(p.getCalorificValue())
                + "}";
    }

    private String buildPlanChainContext(Long planId) {
        List<BlendPlanDetail> details = listDetails(planId);
        StringBuilder sb = new StringBuilder();
        for (BlendPlanDetail d : details) {
            if (!StringUtils.hasText(d.getProductBatchNo())) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("- 产品批次 ").append(d.getProductBatchNo())
                    .append("，配比 ").append(d.getBlendRatio() == null ? "—" : d.getBlendRatio().multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP) + "%")
                    .append("，用量 ").append(d.getUseQuantity() == null ? "—" : d.getUseQuantity().stripTrailingZeros().toPlainString()).append(" 吨。");
            List<com.coalblend.entity.BatchLineage> upstream = batchLineageService.upstream(d.getProductBatchNo());
            if (!upstream.isEmpty()) {
                sb.append("上游链路：");
                sb.append(upstream.stream()
                        .map(l -> l.getParentBatchNo() + " -> " + l.getChildBatchNo() + "(" + l.getProcessStage() + ")")
                        .collect(Collectors.joining("；")));
            } else {
                sb.append("暂无上游血缘记录。");
            }
        }
        return sb.toString();
    }

    private String safeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String jsonNum(BigDecimal n) {
        return n == null ? "null" : n.stripTrailingZeros().toPlainString();
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

    private BigDecimal weighted(List<BlendPlanDetail> details, Function<BlendPlanDetail, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal ratioTotal = BigDecimal.ZERO;
        for (BlendPlanDetail d : details) {
            BigDecimal v = getter.apply(d);
            BigDecimal r = d.getBlendRatio();
            if (v == null || r == null) {
                continue;
            }
            total = total.add(v.multiply(r));
            ratioTotal = ratioTotal.add(r);
        }
        if (ratioTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return total.divide(ratioTotal, 2, RoundingMode.HALF_UP);
    }

    private List<EvaluatedPlanDraft> buildAiCandidateDrafts(Orders order, List<PlanCoalSnapshot> shortlisted,
                                                            AiBlendCandidateResult aiResult) {
        if (aiResult == null || aiResult.getPlans() == null || aiResult.getPlans().isEmpty()) {
            return List.of();
        }
        List<EvaluatedPlanDraft> drafts = new ArrayList<>();
        for (AiBlendCandidatePlan aiPlan : aiResult.getPlans()) {
            AiDraftInput input = toAiDraftInput(shortlisted, aiPlan);
            if (input.snapshots().size() < 2 || input.ratios().size() != input.snapshots().size()) {
                continue;
            }
            EvaluatedPlanDraft draft = planScoreService.evaluate(order, input.snapshots(), input.ratios());
            draft.setCandidateSource("ai");
            draft.setAiCandidateReason(buildAiCandidateReason(aiPlan));
            drafts.add(draft);
        }
        return drafts.stream()
                .sorted(Comparator
                        .comparing((EvaluatedPlanDraft d) -> d.getConstraintResult().isFeasible(), Comparator.reverseOrder())
                        .thenComparing(d -> d.getScoreDetail().getOverallScore(), Comparator.reverseOrder())
                        .thenComparing(EvaluatedPlanDraft::getTotalCost))
                .limit(MAX_RETURN_PLANS)
                .collect(Collectors.toList());
    }

    private AiDraftInput toAiDraftInput(List<PlanCoalSnapshot> shortlisted, AiBlendCandidatePlan aiPlan) {
        if (aiPlan == null || aiPlan.getItems() == null || aiPlan.getItems().size() < 2 || aiPlan.getItems().size() > 4) {
            return new AiDraftInput(List.of(), List.of());
        }
        List<PlanCoalSnapshot> snapshots = new ArrayList<>();
        List<BigDecimal> ratios = new ArrayList<>();
        Set<String> usedMaterialKeys = new HashSet<>();
        Set<Long> usedCoalIds = new HashSet<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (AiBlendCandidateItem item : aiPlan.getItems()) {
            if (item == null || item.getRatio() == null || item.getRatio().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PlanCoalSnapshot snapshot = matchAiItem(shortlisted, item);
            if (snapshot == null || snapshot.getCoalId() == null) {
                continue;
            }
            String materialKey = StringUtils.hasText(snapshot.getProductBatchNo())
                    ? "PB:" + snapshot.getProductBatchNo()
                    : "COAL:" + snapshot.getCoalId();
            if (usedMaterialKeys.contains(materialKey) || usedCoalIds.contains(snapshot.getCoalId())) {
                continue;
            }
            usedMaterialKeys.add(materialKey);
            usedCoalIds.add(snapshot.getCoalId());
            snapshots.add(snapshot);
            ratios.add(item.getRatio());
            sum = sum.add(item.getRatio());
        }
        if (snapshots.size() < 2 || sum.compareTo(new BigDecimal("0.95")) < 0 || sum.compareTo(new BigDecimal("1.05")) > 0) {
            return new AiDraftInput(List.of(), List.of());
        }
        BigDecimal ratioSum = sum;
        List<BigDecimal> normalized = ratios.stream()
                .map(r -> r.divide(ratioSum, 4, RoundingMode.HALF_UP))
                .collect(Collectors.toCollection(ArrayList::new));
        BigDecimal normalizedSum = normalized.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!normalized.isEmpty()) {
            int last = normalized.size() - 1;
            normalized.set(last, normalized.get(last).add(BigDecimal.ONE.subtract(normalizedSum)).setScale(4, RoundingMode.HALF_UP));
        }
        return new AiDraftInput(snapshots, normalized);
    }

    private PlanCoalSnapshot matchAiItem(List<PlanCoalSnapshot> shortlisted, AiBlendCandidateItem item) {
        if (StringUtils.hasText(item.getProductBatchNo())) {
            for (PlanCoalSnapshot s : shortlisted) {
                if (item.getProductBatchNo().equals(s.getProductBatchNo())) {
                    return s;
                }
            }
        }
        if (item.getCoalId() != null) {
            for (PlanCoalSnapshot s : shortlisted) {
                if (item.getCoalId().equals(s.getCoalId())) {
                    return s;
                }
            }
        }
        return null;
    }

    private String buildAiCandidateReason(AiBlendCandidatePlan aiPlan) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(aiPlan.getPlanName())) {
            sb.append("AI方案名称：").append(aiPlan.getPlanName());
        }
        if (StringUtils.hasText(aiPlan.getStrategy())) {
            if (!sb.isEmpty()) {
                sb.append("；");
            }
            sb.append("策略：").append(aiPlan.getStrategy());
        }
        if (aiPlan.getItems() != null && !aiPlan.getItems().isEmpty()) {
            String itemReasons = aiPlan.getItems().stream()
                    .filter(i -> StringUtils.hasText(i.getReason()))
                    .map(i -> (StringUtils.hasText(i.getProductBatchNo()) ? i.getProductBatchNo() : String.valueOf(i.getCoalId()))
                            + "：" + i.getReason())
                    .collect(Collectors.joining("；"));
            if (StringUtils.hasText(itemReasons)) {
                if (!sb.isEmpty()) {
                    sb.append("；");
                }
                sb.append("物料理由：").append(itemReasons);
            }
        }
        if (StringUtils.hasText(aiPlan.getRisk())) {
            if (!sb.isEmpty()) {
                sb.append("；");
            }
            sb.append("AI风险提示：").append(aiPlan.getRisk());
        }
        return sb.toString();
    }

    private List<EvaluatedPlanDraft> mergeAndRankDrafts(List<EvaluatedPlanDraft> aiDrafts,
                                                        List<EvaluatedPlanDraft> systemDrafts) {
        Map<String, EvaluatedPlanDraft> merged = new LinkedHashMap<>();
        for (EvaluatedPlanDraft d : aiDrafts) {
            merged.put(draftSignature(d), d);
        }
        for (EvaluatedPlanDraft d : systemDrafts) {
            merged.putIfAbsent(draftSignature(d), d);
        }
        List<EvaluatedPlanDraft> drafts = new ArrayList<>(merged.values());
        drafts.sort(Comparator
                .comparing((EvaluatedPlanDraft d) -> d.getConstraintResult().isFeasible(), Comparator.reverseOrder())
                .thenComparing(d -> d.getScoreDetail().getOverallScore(), Comparator.reverseOrder())
                .thenComparing(EvaluatedPlanDraft::getTotalCost));
        return drafts;
    }

    private String draftSignature(EvaluatedPlanDraft draft) {
        if (draft == null || draft.getDetails() == null) {
            return "";
        }
        return draft.getDetails().stream()
                .map(d -> {
                    String material = StringUtils.hasText(d.getProductBatchNo()) ? d.getProductBatchNo() : String.valueOf(d.getCoalId());
                    BigDecimal pct = d.getBlendRatio() == null ? BigDecimal.ZERO : d.getBlendRatio().setScale(2, RoundingMode.HALF_UP);
                    return material + ":" + pct.toPlainString();
                })
                .sorted()
                .collect(Collectors.joining("|"));
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

    private record AiDraftInput(List<PlanCoalSnapshot> snapshots, List<BigDecimal> ratios) {
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
                                          Map<Long, CoalType> typeMap, String experimentCode,
                                          String aiModelName) {
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
            plan.setCandidateSource(StringUtils.hasText(draft.getCandidateSource()) ? draft.getCandidateSource() : "system");
            plan.setAiCandidateReason(draft.getAiCandidateReason());
            if ("ai".equalsIgnoreCase(plan.getCandidateSource()) && StringUtils.hasText(aiModelName)) {
                plan.setAiModelName(aiModelName);
                plan.setAiGenerateFlag(1);
            }
            plan.setCreateBy(createBy);
            blendPlanMapper.insert(plan);
            for (BlendPlanDetail d : draft.getDetails()) {
                d.setPlanId(plan.getId());
                blendPlanDetailMapper.insert(d);
            }
            persistExperimentRecord(experimentCode, order, plan, draft, aiModelName);
            persisted.add(new ScoredPlan(plan.getId(), score.getOverallScore()));
        }
        return persisted;
    }

    private void persistExperimentRecord(String experimentCode, Orders order, BlendPlan plan,
                                         EvaluatedPlanDraft draft, String aiModelName) {
        ConstraintResult constraint = draft.getConstraintResult();
        ScoreDetail score = draft.getScoreDetail();
        ExperimentRecord record = new ExperimentRecord();
        record.setExperimentCode(experimentCode);
        record.setOrderId(order.getId());
        record.setModelName(resolveExperimentModelName(draft, aiModelName));
        record.setPlanId(plan.getId());
        record.setTotalCost(draft.getTotalCost());
        record.setAvgAsh(constraint.getPredictedAsh());
        record.setAvgSulfur(constraint.getPredictedSulfur());
        record.setAvgMoisture(constraint.getPredictedMoisture());
        record.setAvgCalorific(constraint.getPredictedCalorific());
        record.setQualityScore(score.getQualityScore());
        record.setCostScore(score.getCostScore());
        record.setInventoryScore(score.getStabilityScore());
        record.setFinalScore(score.getOverallScore());
        record.setConstraintHit(buildExperimentConstraintJson(draft));
        record.setRiskWarning(StringUtils.hasText(draft.getRiskTip()) ? draft.getRiskTip() : constraint.riskLevel());
        record.setExplainText(buildExperimentExplainText(draft));
        record.setCreateTime(LocalDateTime.now());
        experimentRecordService.saveRecord(record);
    }

    private String resolveExperimentModelName(EvaluatedPlanDraft draft, String aiModelName) {
        String source = StringUtils.hasText(draft.getCandidateSource()) ? draft.getCandidateSource() : "system";
        String modelName;
        if ("ai".equalsIgnoreCase(source)) {
            modelName = StringUtils.hasText(aiModelName) ? aiModelName : "ai-model";
        } else {
            modelName = "system-enumeration";
        }
        return modelName.length() > 50 ? modelName.substring(0, 50) : modelName;
    }

    private String buildExperimentExplainText(EvaluatedPlanDraft draft) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(draft.getAiCandidateReason())) {
            sb.append("【模型生成理由】").append(draft.getAiCandidateReason());
        }
        if (StringUtils.hasText(draft.getExplanation())) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("【评分解释】").append(draft.getExplanation());
        }
        return sb.toString();
    }

    private String buildExperimentConstraintJson(EvaluatedPlanDraft draft) {
        ConstraintResult c = draft.getConstraintResult();
        return "{"
                + "\"candidateSource\":\"" + safeJson(draft.getCandidateSource()) + "\","
                + "\"feasible\":" + c.isFeasible() + ","
                + "\"riskLevel\":\"" + safeJson(c.riskLevel()) + "\","
                + "\"violations\":" + jsonArray(c.getViolations()) + ","
                + "\"warnings\":" + jsonArray(c.getWarnings()) + ","
                + "\"scoreDetail\":\"" + safeJson(buildScoreDetailSummary(draft.getScoreDetail())) + "\""
                + "}";
    }

    private String jsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream()
                .map(v -> "\"" + safeJson(v) + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String buildExperimentCode(Orders order, AiBlendCandidateResult aiCandidateResult) {
        String model = aiCandidateResult == null ? "unknown" : aiCandidateResult.getModelName();
        String modelTag = StringUtils.hasText(model)
                ? model.replaceAll("[^A-Za-z0-9_-]", "-")
                : "unknown";
        if (modelTag.length() > 18) {
            modelTag = modelTag.substring(0, 18);
        }
        return "EXP-O" + order.getId() + "-" + modelTag + "-" + System.currentTimeMillis();
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

    private List<CandidateEvaluationItemVO> toCandidateEvaluationVos(List<EvaluatedPlanDraft> drafts,
                                                                     Map<Long, CoalType> typeMap) {
        if (drafts == null || drafts.isEmpty()) {
            return List.of();
        }
        return drafts.stream()
                .limit(MAX_EVALUATED_CANDIDATES_RETURN)
                .map(d -> toCandidateEvaluationVo(d, typeMap))
                .collect(Collectors.toList());
    }

    private CandidateEvaluationItemVO toCandidateEvaluationVo(EvaluatedPlanDraft draft, Map<Long, CoalType> typeMap) {
        ConstraintResult constraint = draft.getConstraintResult();
        ScoreDetail score = draft.getScoreDetail();
        CandidateEvaluationItemVO vo = new CandidateEvaluationItemVO();
        vo.setCandidateSource(StringUtils.hasText(draft.getCandidateSource()) ? draft.getCandidateSource() : "system");
        vo.setPlanName("ai".equals(vo.getCandidateSource()) ? "AI候选方案" : "系统枚举方案");
        vo.setAiCandidateReason(draft.getAiCandidateReason());
        vo.setTotalCost(draft.getTotalCost());
        vo.setQualityScore(score.getQualityScore());
        vo.setCostScore(score.getCostScore());
        vo.setStabilityScore(score.getStabilityScore());
        vo.setOverallScore(score.getOverallScore());
        vo.setFeasibleFlag(constraint.isFeasible() ? 1 : 0);
        vo.setConstraintSummary(buildConstraintSummary(constraint));
        vo.setScoreDetail(buildScoreDetailSummary(score));
        vo.setRiskLevel(constraint.riskLevel());
        vo.setRiskTip(draft.getRiskTip());
        vo.setDetails(draft.getDetails().stream().map(d -> {
            PlanDetailVO row = new PlanDetailVO();
            row.setCoalId(d.getCoalId());
            row.setProductBatchId(d.getProductBatchId());
            row.setProductBatchNo(d.getProductBatchNo());
            row.setInventoryId(d.getInventoryId());
            row.setQualitySnapshotJson(d.getQualitySnapshotJson());
            CoalType t = typeMap.get(d.getCoalId());
            row.setCoalName(t == null ? null : t.getCoalName());
            row.setBlendRatio(d.getBlendRatio());
            row.setUseQuantity(d.getUseQuantity());
            row.setPredictedAsh(d.getPredictedAsh());
            row.setPredictedSulfur(d.getPredictedSulfur());
            row.setPredictedMoisture(d.getPredictedMoisture());
            row.setPredictedVolatile(d.getPredictedVolatile());
            row.setPredictedCalorific(d.getPredictedCalorific());
            row.setUnitCost(d.getUnitCost());
            row.setRemark(d.getRemark());
            return row;
        }).collect(Collectors.toList()));
        return vo;
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
            v.setProductBatchId(d.getProductBatchId());
            v.setProductBatchNo(d.getProductBatchNo());
            v.setInventoryId(d.getInventoryId());
            v.setQualitySnapshotJson(d.getQualitySnapshotJson());
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
