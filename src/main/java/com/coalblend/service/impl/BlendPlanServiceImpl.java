package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.config.CoalBlendProperties;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.dto.BlendGenerateDTO;
import com.coalblend.dto.BlendPlanExecuteDTO;
import com.coalblend.dto.knowledge.KnowledgeContextDTO;
import com.coalblend.entity.BlendPlan;
import com.coalblend.entity.BlendPlanDetail;
import com.coalblend.entity.CoalQuality;
import com.coalblend.entity.CoalType;
import com.coalblend.entity.ExperimentRecord;
import com.coalblend.entity.Inventory;
import com.coalblend.entity.Orders;
import com.coalblend.entity.ProductBatch;
import com.coalblend.enums.DecisionProblemSeverity;
import com.coalblend.enums.DecisionProblemType;
import com.coalblend.enums.PlanDecisionStatus;
import com.coalblend.enums.RecommendationMode;
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
import com.coalblend.service.intelligent.BlendGenerationConfigResolver;
import com.coalblend.service.intelligent.ModelInferenceService;
import com.coalblend.service.intelligent.ParetoRankService;
import com.coalblend.service.intelligent.PlanDecisionService;
import com.coalblend.service.intelligent.PlanScoreService;
import com.coalblend.service.intelligent.model.AiBlendCandidateItem;
import com.coalblend.service.intelligent.model.AiBlendCandidatePlan;
import com.coalblend.service.intelligent.model.AiBlendCandidateResult;
import com.coalblend.service.intelligent.model.BlendGenerationRuntimeConfig;
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
import com.coalblend.vo.blend.DecisionProblemItemVO;
import com.coalblend.vo.blend.DecisionSuggestionItemVO;
import com.coalblend.vo.blend.GenerationConfigVO;
import com.coalblend.vo.blend.ParetoSummaryVO;
import com.coalblend.vo.blend.PlanDetailVO;
import com.coalblend.vo.blend.PlanWithDetailsVO;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;
import com.coalblend.vo.rag.RagRetrieveResultVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Objects;
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
    private static final int TOTAL_RATIO_UNIT = 100;

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
    private final BlendGenerationConfigResolver blendGenerationConfigResolver;
    private final PlanDecisionService planDecisionService;
    private final ParetoRankService paretoRankService;
    private final ObjectMapper objectMapper;

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
        IPage<BlendPlan> result = blendPlanMapper.selectPage(page, w);
        result.getRecords().forEach(this::hydratePlanDecisionFields);
        return result;
    }

    @Override
    public BlendPlan getById(Long id) {
        BlendPlan row = blendPlanMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "方案不存在");
        }
        hydratePlanDecisionFields(row);
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
        List<BlendPlan> rows = blendPlanMapper.selectList(new LambdaQueryWrapper<BlendPlan>()
                .eq(BlendPlan::getOrderId, orderId)
                .orderByDesc(BlendPlan::getCreateTime));
        rows.forEach(this::hydratePlanDecisionFields);
        return rows;
    }

    @Override
    public void selectPlan(Long planId) {
        BlendPlan plan = getById(planId);
        if (!isExecutableDecision(plan)) {
            throw new BusinessException("风险参考或不可执行方案不能被选择");
        }
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
        if (!isExecutableDecision(plan)) {
            throw new BusinessException("风险参考或不可执行方案不能直接执行");
        }
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
        BlendGenerationRuntimeConfig runtimeConfig = blendGenerationConfigResolver.resolve(dto);
        GenerationConfigVO generationConfig = toGenerationConfigVo(runtimeConfig);

        Map<String, Object> constraints = new LinkedHashMap<>();
        constraints.put("demandQuantity", order.getDemandQuantity());
        constraints.put("maxAsh", order.getTargetAsh());
        constraints.put("maxSulfur", order.getTargetSulfur());
        constraints.put("maxMoisture", order.getTargetMoisture());
        constraints.put("referenceVolatile", order.getTargetVolatile());
        constraints.put("minCalorific", order.getTargetCalorific());
        constraints.put("priorityLevel", order.getPriorityLevel());
        constraints.put("modelConfigId", dto.getModelConfigId());
        String candidateScope = StringUtils.hasText(dto.getCandidateScope()) ? dto.getCandidateScope() : "coal_type";
        constraints.put("candidateScope", candidateScope);
        constraints.put("scoreStrategy", runtimeConfig.getScoreStrategy().name());
        constraints.put("ratioStep", runtimeConfig.getRatioStep());
        constraints.put("maxShortlistCoals", runtimeConfig.getMaxShortlistCoals());
        constraints.put("maxMaterialCount", runtimeConfig.getMaxMaterialCount());
        constraints.put("maxReturnPlans", runtimeConfig.getMaxReturnPlans());

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
                ? buildProductBatchShortlist(order, typeMap, runtimeConfig)
                : buildCoalTypeShortlist(order, bestInv, typeMap, runtimeConfig);

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
                order, shortlisted, matchedRules, matchedCases, ragRetrieveResult, candidateScope, dto.getModelConfigId());
        List<EvaluatedPlanDraft> aiDrafts = buildAiCandidateDrafts(order, shortlisted, aiCandidateResult, runtimeConfig);
        List<EvaluatedPlanDraft> systemDrafts = coalBlendProperties.isEnableSystemEnumeration()
                ? buildCandidateDrafts(order, shortlisted, runtimeConfig)
                : List.of();
        List<EvaluatedPlanDraft> drafts = mergeDrafts(aiDrafts, systemDrafts);
        planDecisionService.decide(order, drafts, runtimeConfig);
        paretoRankService.rank(order, drafts);
        List<EvaluatedPlanDraft> sorted = sortFinalDrafts(drafts);

        List<EvaluatedPlanDraft> feasible = sorted.stream()
                .filter(d -> PlanDecisionStatus.FEASIBLE.name().equals(d.getDecisionStatus()))
                .toList();
        List<EvaluatedPlanDraft> risky = sorted.stream()
                .filter(d -> PlanDecisionStatus.RISKY.name().equals(d.getDecisionStatus()))
                .toList();

        RecommendationMode mode;
        EvaluatedPlanDraft recommendedDraft;
        if (!feasible.isEmpty()) {
            mode = RecommendationMode.NORMAL;
            recommendedDraft = feasible.get(0);
        } else if (!risky.isEmpty()) {
            mode = RecommendationMode.RISK_REFERENCE;
            recommendedDraft = risky.get(0);
        } else {
            mode = RecommendationMode.NO_SOLUTION;
            recommendedDraft = null;
        }
        if (recommendedDraft != null) {
            recommendedDraft.setRecommendationMode(mode.name());
        }

        List<EvaluatedPlanDraft> persistable = buildPersistableDrafts(sorted, mode, runtimeConfig);

        String experimentCode = StringUtils.hasText(dto.getExperimentCode())
                ? dto.getExperimentCode().trim()
                : buildExperimentCode(order, aiCandidateResult);
        List<ScoredPlan> persistedPlans = persistable.isEmpty()
                ? List.of()
                : persistPlans(order, persistable, dto.getCreateBy(), typeMap, experimentCode,
                aiCandidateResult == null ? null : aiCandidateResult.getModelName(), mode, generationConfig);
        ScoredPlan best = persistedPlans.isEmpty() ? null : persistedPlans.get(0);
        List<ScoredPlan> others = persistedPlans.size() <= 1 ? List.of() : persistedPlans.subList(1, persistedPlans.size());

        if (!persistedPlans.isEmpty()) {
            Orders orderPatch = new Orders();
            orderPatch.setId(order.getId());
            orderPatch.setOrderStatus("generated");
            ordersMapper.updateById(orderPatch);
        }

        BlendGenerateResultVO vo = new BlendGenerateResultVO();
        vo.setOrder(ordersMapper.selectById(order.getId()));
        vo.setConstraints(constraints);
        constraints.put("shortlistedCoalCount", shortlisted.size());
        constraints.put("generatedPlanCount", persistedPlans.size());
        constraints.put("experimentCode", experimentCode);
        constraints.put("experimentModelName", aiCandidateResult == null ? null : aiCandidateResult.getModelName());
        constraints.put("systemEnumerationEnabled", coalBlendProperties.isEnableSystemEnumeration());
        constraints.put("aiCandidatePlanCount", aiCandidateResult == null || aiCandidateResult.getPlans() == null
                ? 0 : aiCandidateResult.getPlans().size());
        constraints.put("acceptedAiCandidateCount", aiDrafts.size());
        if (aiCandidateResult != null && StringUtils.hasText(aiCandidateResult.getErrorMessage())) {
            constraints.put("aiCandidateError", aiCandidateResult.getErrorMessage());
        }
        constraints.put("totalCandidateCount", sorted.size());
        constraints.put("feasiblePlanCount", feasible.size());
        constraints.put("riskyPlanCount", risky.size());
        constraints.put("infeasiblePlanCount", sorted.stream()
                .filter(d -> PlanDecisionStatus.INFEASIBLE.name().equals(d.getDecisionStatus())).count());

        PlanDecisionStatus resultStatus = mode == RecommendationMode.NORMAL ? PlanDecisionStatus.FEASIBLE
                : (mode == RecommendationMode.RISK_REFERENCE ? PlanDecisionStatus.RISKY : PlanDecisionStatus.INFEASIBLE);
        List<DecisionProblemItemVO> resultProblems = sorted.isEmpty()
                ? buildNoCandidateProblems()
                : resolveResultProblems(mode, recommendedDraft, sorted);
        List<DecisionSuggestionItemVO> resultSuggestions = sorted.isEmpty()
                ? buildNoCandidateSuggestions()
                : resolveResultSuggestions(resultProblems, recommendedDraft, sorted);

        vo.setDecisionStatus(resultStatus.name());
        vo.setDecisionStatusLabel(resultStatus.getLabel());
        vo.setRecommendationMode(mode.name());
        vo.setRecommendationModeLabel(mode.getLabel());
        vo.setDecisionSummary(buildDecisionSummary(sorted, mode, recommendedDraft));
        vo.setProblemItems(resultProblems);
        vo.setSuggestionItems(resultSuggestions);
        vo.setParetoSummary(buildParetoSummary(sorted, recommendedDraft));
        vo.setGenerationConfig(generationConfig);

        PlanWithDetailsVO recommended = best == null ? null : toVo(best.planId, typeMap);
        vo.setRecommendedPlan(recommended);
        vo.setCandidatePlans(others.stream().map(p -> toVo(p.planId, typeMap)).collect(Collectors.toList()));
        vo.setAiEvaluatedCandidates(toCandidateEvaluationVos(aiDrafts, typeMap));
        vo.setSystemEvaluatedCandidates(toCandidateEvaluationVos(systemDrafts, typeMap));
        vo.setMatchedRules(matchedRules);
        vo.setMatchedCases(matchedCases);
        vo.setAiCandidateResult(aiCandidateResult);
        vo.setRagRetrieveResult(ragRetrieveResult);
        if (best != null && recommended != null) {
            KnowledgeContextDTO knowledgeContext = knowledgeAssembleService.assemble(
                    order, constraints, typeMap, bestInv, shortlistedCoalIds, matchedRules, matchedCases,
                    recommended, order.getDemandQuantity());
            knowledgeContext.setRagRetrieveResult(ragRetrieveResult);
            String chainContext = buildPlanChainContext(best.planId);
            knowledgeContext.setRagKnowledgeText(ragRetrieveService.buildKnowledgeText(ragRetrieveResult)
                    + (StringUtils.hasText(chainContext) ? "\n\n【方案批次来源追溯】\n" + chainContext : ""));
            vo.setKnowledgeContext(knowledgeContext);
            vo.setKnowledgeSummary(knowledgeAssembleService.summarize(knowledgeContext));

            AiExplainResultVO ai = modelInferenceService.enrichRecommendedPlan(
                    best.planId, order, recommended, matchedRules, matchedCases, knowledgeContext, dto.getModelConfigId());
            ragTraceService.saveBlendGenerateTrace(best.planId, ragRetrieveResult, ai);
            vo.setRecommendedPlan(toVo(best.planId, typeMap));
            vo.setRagExplanation(ai);
            vo.setExplainSummary(buildDecisionExplainSummary(vo, recommendedDraft, ai));
        } else {
            vo.setCandidatePlans(List.of());
            vo.setExplainSummary(buildDecisionExplainSummary(vo, null, null));
        }
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

    private GenerationConfigVO toGenerationConfigVo(BlendGenerationRuntimeConfig config) {
        GenerationConfigVO vo = new GenerationConfigVO();
        vo.setScoreStrategy(config.getScoreStrategy().name());
        vo.setScoreStrategyLabel(config.getScoreStrategy().getLabel());
        vo.setQualityWeight(config.getScoreStrategy().getQualityWeight());
        vo.setCostWeight(config.getScoreStrategy().getCostWeight());
        vo.setStabilityWeight(config.getScoreStrategy().getStabilityWeight());
        vo.setRatioStep(config.getRatioStep());
        vo.setMaxShortlistCoals(config.getMaxShortlistCoals());
        vo.setMaxMaterialCount(config.getMaxMaterialCount());
        vo.setMaxReturnPlans(config.getMaxReturnPlans());
        return vo;
    }

    private List<EvaluatedPlanDraft> sortFinalDrafts(List<EvaluatedPlanDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return List.of();
        }
        return drafts.stream()
                .sorted(Comparator
                        .comparingInt((EvaluatedPlanDraft d) -> decisionPriority(d.getDecisionStatus()))
                        .thenComparing(d -> d.getParetoRank() == null ? Integer.MAX_VALUE : d.getParetoRank())
                        .thenComparing((EvaluatedPlanDraft d) -> d.getScoreDetail().getOverallScore(), Comparator.reverseOrder())
                        .thenComparing(EvaluatedPlanDraft::getTotalCost, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(this::draftSignature))
                .collect(Collectors.toList());
    }

    private int decisionPriority(String decisionStatus) {
        if (PlanDecisionStatus.FEASIBLE.name().equals(decisionStatus)) {
            return PlanDecisionStatus.FEASIBLE.getPriority();
        }
        if (PlanDecisionStatus.RISKY.name().equals(decisionStatus)) {
            return PlanDecisionStatus.RISKY.getPriority();
        }
        return PlanDecisionStatus.INFEASIBLE.getPriority();
    }

    private List<EvaluatedPlanDraft> buildPersistableDrafts(List<EvaluatedPlanDraft> sorted,
                                                            RecommendationMode mode,
                                                            BlendGenerationRuntimeConfig runtimeConfig) {
        if (sorted == null || sorted.isEmpty() || mode == RecommendationMode.NO_SOLUTION) {
            return List.of();
        }
        int limit = runtimeConfig.getMaxReturnPlans() == null ? MAX_RETURN_PLANS : runtimeConfig.getMaxReturnPlans();
        if (mode == RecommendationMode.NORMAL) {
            return sorted.stream()
                    .filter(d -> PlanDecisionStatus.FEASIBLE.name().equals(d.getDecisionStatus())
                            || PlanDecisionStatus.RISKY.name().equals(d.getDecisionStatus()))
                    .limit(limit)
                    .collect(Collectors.toList());
        }
        return sorted.stream()
                .filter(d -> PlanDecisionStatus.RISKY.name().equals(d.getDecisionStatus()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private ParetoSummaryVO buildParetoSummary(List<EvaluatedPlanDraft> sorted, EvaluatedPlanDraft recommendedDraft) {
        ParetoSummaryVO vo = new ParetoSummaryVO();
        List<EvaluatedPlanDraft> rows = sorted == null ? List.of() : sorted;
        vo.setTotalCandidateCount(rows.size());
        vo.setFeasibleCount((int) rows.stream().filter(d -> PlanDecisionStatus.FEASIBLE.name().equals(d.getDecisionStatus())).count());
        vo.setRiskyCount((int) rows.stream().filter(d -> PlanDecisionStatus.RISKY.name().equals(d.getDecisionStatus())).count());
        vo.setInfeasibleCount((int) rows.stream().filter(d -> PlanDecisionStatus.INFEASIBLE.name().equals(d.getDecisionStatus())).count());
        vo.setParetoFrontCount((int) rows.stream().filter(d -> Objects.equals(d.getParetoRank(), 1)).count());
        vo.setBestDecisionStatus(recommendedDraft == null ? null : recommendedDraft.getDecisionStatus());
        vo.setBestPlanName(recommendedDraft == null ? null : ("ai".equalsIgnoreCase(recommendedDraft.getCandidateSource())
                ? "AI候选方案" : "系统枚举方案"));
        return vo;
    }

    private String buildDecisionSummary(List<EvaluatedPlanDraft> sorted, RecommendationMode mode,
                                        EvaluatedPlanDraft recommendedDraft) {
        int total = sorted == null ? 0 : sorted.size();
        long feasible = sorted == null ? 0 : sorted.stream()
                .filter(d -> PlanDecisionStatus.FEASIBLE.name().equals(d.getDecisionStatus())).count();
        long risky = sorted == null ? 0 : sorted.stream()
                .filter(d -> PlanDecisionStatus.RISKY.name().equals(d.getDecisionStatus())).count();
        long infeasible = sorted == null ? 0 : sorted.stream()
                .filter(d -> PlanDecisionStatus.INFEASIBLE.name().equals(d.getDecisionStatus())).count();
        if (mode == RecommendationMode.NORMAL) {
            return "系统生成 " + total + " 组候选方案，其中可执行 " + feasible + " 组，风险参考 "
                    + risky + " 组，不可执行 " + infeasible + " 组。推荐方案位于 Pareto 第 "
                    + (recommendedDraft == null ? "—" : recommendedDraft.getParetoRank()) + " 前沿，综合评分靠前。";
        }
        if (mode == RecommendationMode.RISK_REFERENCE) {
            return "当前订单没有完全可执行方案，系统返回风险最小参考方案。该方案存在库存余量偏低或质量安全余量不足，禁止直接执行。";
        }
        return "当前订单约束下无可执行方案，主要候选均存在硬约束违规。系统没有保存不可执行方案。";
    }

    private List<DecisionProblemItemVO> resolveResultProblems(RecommendationMode mode,
                                                              EvaluatedPlanDraft recommendedDraft,
                                                              List<EvaluatedPlanDraft> sorted) {
        if (mode == RecommendationMode.RISK_REFERENCE && recommendedDraft != null) {
            return distinctProblems(recommendedDraft.getProblemItems(), true);
        }
        if (mode == RecommendationMode.NO_SOLUTION) {
            return distinctProblems(sorted.stream()
                    .flatMap(d -> d.getProblemItems().stream())
                    .filter(p -> DecisionProblemSeverity.BLOCKER.name().equals(p.getSeverity()))
                    .collect(Collectors.toList()), true);
        }
        return distinctProblems(sorted.stream()
                .flatMap(d -> d.getProblemItems().stream())
                .filter(p -> DecisionProblemSeverity.WARNING.name().equals(p.getSeverity()))
                .collect(Collectors.toList()), false);
    }

    private List<DecisionProblemItemVO> distinctProblems(List<DecisionProblemItemVO> problems, boolean includeBlocker) {
        if (problems == null || problems.isEmpty()) {
            return List.of();
        }
        Map<String, DecisionProblemItemVO> map = new LinkedHashMap<>();
        for (DecisionProblemItemVO item : problems) {
            if (!includeBlocker && DecisionProblemSeverity.BLOCKER.name().equals(item.getSeverity())) {
                continue;
            }
            String key = item.getType() + "|" + item.getSeverity() + "|" + item.getMessage();
            map.putIfAbsent(key, item);
            if (map.size() >= 20) {
                break;
            }
        }
        return new ArrayList<>(map.values());
    }

    private List<DecisionSuggestionItemVO> resolveResultSuggestions(List<DecisionProblemItemVO> resultProblems,
                                                                    EvaluatedPlanDraft recommendedDraft,
                                                                    List<EvaluatedPlanDraft> sorted) {
        Set<String> problemTypes = resultProblems == null ? Set.of() : resultProblems.stream()
                .map(DecisionProblemItemVO::getType)
                .collect(Collectors.toSet());
        List<DecisionSuggestionItemVO> candidates = new ArrayList<>();
        if (recommendedDraft != null) {
            candidates.addAll(recommendedDraft.getSuggestionItems());
        }
        if (sorted != null) {
            sorted.forEach(d -> candidates.addAll(d.getSuggestionItems()));
        }
        Map<String, DecisionSuggestionItemVO> map = new LinkedHashMap<>();
        for (DecisionSuggestionItemVO item : candidates) {
            if (!problemTypes.isEmpty() && !problemTypes.contains(item.getType())) {
                continue;
            }
            map.putIfAbsent(item.getType(), item);
        }
        return map.values().stream()
                .sorted(Comparator.comparing(DecisionSuggestionItemVO::getPriority)
                        .thenComparing(DecisionSuggestionItemVO::getAction, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    private List<DecisionProblemItemVO> buildNoCandidateProblems() {
        DecisionProblemItemVO item = new DecisionProblemItemVO();
        item.setType(DecisionProblemType.MATERIAL_COUNT_INVALID.name());
        item.setTypeLabel(DecisionProblemType.MATERIAL_COUNT_INVALID.getLabel());
        item.setSeverity(DecisionProblemSeverity.BLOCKER.name());
        item.setSeverityLabel(DecisionProblemSeverity.BLOCKER.getLabel());
        item.setFieldName("candidatePlans");
        item.setMessage("当前候选空间未生成有效候选方案，请检查候选范围、煤质、库存和模型候选输出。");
        return List.of(item);
    }

    private List<DecisionSuggestionItemVO> buildNoCandidateSuggestions() {
        DecisionSuggestionItemVO item = new DecisionSuggestionItemVO();
        item.setType(DecisionProblemType.MATERIAL_COUNT_INVALID.name());
        item.setAction("EXPAND_CANDIDATE_SCOPE");
        item.setMessage("扩大候选范围、补充有效煤质和库存数据，或开启系统枚举后重新生成方案。");
        item.setPriority(1);
        return List.of(item);
    }

    private String buildDecisionExplainSummary(BlendGenerateResultVO vo, EvaluatedPlanDraft recommendedDraft,
                                               AiExplainResultVO ai) {
        StringBuilder sb = new StringBuilder();
        sb.append(vo.getDecisionSummary());
        if (vo.getGenerationConfig() != null) {
            sb.append(" 当前采用").append(vo.getGenerationConfig().getScoreStrategyLabel()).append("。");
        }
        sb.append(" 推荐模式：").append(vo.getRecommendationModeLabel()).append("。");
        if (recommendedDraft != null) {
            sb.append(" 推荐方案 Pareto Rank 为 ").append(recommendedDraft.getParetoRank()).append("，综合评分 ")
                    .append(fmt(recommendedDraft.getScoreDetail().getOverallScore())).append("。");
        }
        String risks = summarizeProblems(vo.getProblemItems());
        if (StringUtils.hasText(risks)) {
            sb.append(" 主要风险：").append(risks).append("。");
        }
        String suggestions = summarizeSuggestions(vo.getSuggestionItems());
        if (StringUtils.hasText(suggestions)) {
            sb.append(" 调整建议：").append(suggestions).append("。");
        }
        if (ai != null) {
            sb.append("\n\n").append(buildAiSummaryLine(ai));
        }
        return sb.toString();
    }

    private String summarizeProblems(List<DecisionProblemItemVO> problems) {
        if (problems == null || problems.isEmpty()) {
            return null;
        }
        return problems.stream()
                .map(DecisionProblemItemVO::getTypeLabel)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(4)
                .collect(Collectors.joining("、"));
    }

    private String summarizeSuggestions(List<DecisionSuggestionItemVO> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }
        return suggestions.stream()
                .map(DecisionSuggestionItemVO::getMessage)
                .filter(StringUtils::hasText)
                .limit(3)
                .collect(Collectors.joining("；"));
    }

    private static BigDecimal nzSort(BigDecimal v) {
        return v == null ? BigDecimal.valueOf(Double.MAX_VALUE) : v;
    }

    private List<PlanCoalSnapshot> buildCoalTypeShortlist(Orders order, Map<Long, Inventory> bestInv,
                                                          Map<Long, CoalType> typeMap,
                                                          BlendGenerationRuntimeConfig runtimeConfig) {
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

        List<PlanCoalSnapshot> candidates = candidateCoalIds.stream()
                .filter(qualityMap::containsKey)
                .map(cid -> new PlanCoalSnapshot(cid, typeMap.get(cid), qualityMap.get(cid), bestInv.get(cid)))
                .filter(s -> s.getType() != null && s.getQuality() != null && s.getInventory() != null)
                .filter(s -> s.getType().getPurchasePrice() != null)
                .filter(s -> s.getInventory().getAvailableQuantity() != null
                        && s.getInventory().getAvailableQuantity().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        PriceRange priceRange = priceRange(candidates);
        return candidates.stream()
                .sorted(Comparator
                        .comparing((PlanCoalSnapshot s) -> shortlistScore(order, s, priceRange)).reversed()
                        .thenComparing(s -> nzSort(s.getQuality().getSulfurContent()))
                        .thenComparing(s -> nzSort(s.getType().getPurchasePrice())))
                .limit(runtimeConfig.getMaxShortlistCoals())
                .collect(Collectors.toList());
    }

    private List<PlanCoalSnapshot> buildProductBatchShortlist(Orders order, Map<Long, CoalType> typeMap,
                                                              BlendGenerationRuntimeConfig runtimeConfig) {
        List<ProductBatch> products = productBatchMapper.selectList(new LambdaQueryWrapper<ProductBatch>()
                .eq(ProductBatch::getStatus, "available")
                .gt(ProductBatch::getAvailableQuantity, BigDecimal.ZERO)
                .in(ProductBatch::getProductType, List.of("clean_coal", "mixed_product"))
                .orderByDesc(ProductBatch::getAvailableQuantity)
                .orderByDesc(ProductBatch::getId)
                .last("LIMIT 30"));
        List<PlanCoalSnapshot> candidates = products.stream()
                .filter(p -> p.getCoalId() != null && typeMap.containsKey(p.getCoalId()))
                .filter(p -> productQualityBoundary(order, p))
                .map(p -> toProductSnapshot(p, typeMap.get(p.getCoalId())))
                .filter(s -> s.getType() != null && s.getQuality() != null && s.getInventory() != null)
                .filter(s -> s.getType().getPurchasePrice() != null)
                .filter(s -> s.getInventory().getAvailableQuantity() != null
                        && s.getInventory().getAvailableQuantity().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        PriceRange priceRange = priceRange(candidates);
        return candidates.stream()
                .sorted(Comparator
                        .comparing((PlanCoalSnapshot s) -> shortlistScore(order, s, priceRange)).reversed()
                        .thenComparing(s -> nzSort(s.getQuality().getSulfurContent()))
                        .thenComparing(s -> nzSort(s.getType().getPurchasePrice())))
                .limit(runtimeConfig.getMaxShortlistCoals())
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

    private BigDecimal shortlistScore(Orders order, PlanCoalSnapshot s, PriceRange priceRange) {
        CoalQuality q = s.getQuality();
        Inventory inv = s.getInventory();
        CoalType t = s.getType();
        BigDecimal qualityDeviation = shortlistQualityDeviation(order, q);
        BigDecimal inventoryCoverageScore = BigDecimal.ZERO;
        if (inv.getAvailableQuantity() != null && order.getDemandQuantity() != null
                && order.getDemandQuantity().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal coverage = inv.getAvailableQuantity().divide(order.getDemandQuantity(), 6, RoundingMode.HALF_UP);
            if (coverage.compareTo(BigDecimal.ONE) >= 0) {
                inventoryCoverageScore = new BigDecimal("15");
            } else if (coverage.compareTo(new BigDecimal("0.5")) >= 0) {
                inventoryCoverageScore = new BigDecimal("8");
            } else {
                inventoryCoverageScore = new BigDecimal("3");
            }
        }
        BigDecimal costPenalty = normalizedPrice(t.getPurchasePrice(), priceRange).multiply(new BigDecimal("10"));
        return new BigDecimal("100")
                .subtract(qualityDeviation.multiply(new BigDecimal("40")))
                .add(inventoryCoverageScore)
                .subtract(costPenalty)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal shortlistQualityDeviation(Orders order, CoalQuality q) {
        if (q == null || order == null) {
            return BigDecimal.ZERO;
        }
        return normalizedDeviation(q.getAshContent(), order.getTargetAsh()).multiply(new BigDecimal("0.25"))
                .add(normalizedDeviation(q.getSulfurContent(), order.getTargetSulfur()).multiply(new BigDecimal("0.30")))
                .add(normalizedDeviation(q.getMoistureContent(), order.getTargetMoisture()).multiply(new BigDecimal("0.15")))
                .add(normalizedDeviation(q.getCalorificValue(), order.getTargetCalorific()).multiply(new BigDecimal("0.25")))
                .add(normalizedDeviation(q.getVolatileContent(), order.getTargetVolatile()).multiply(new BigDecimal("0.05")));
    }

    private BigDecimal normalizedDeviation(BigDecimal actual, BigDecimal target) {
        if (actual == null || target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return actual.subtract(target).abs().divide(target, 6, RoundingMode.HALF_UP);
    }

    private PriceRange priceRange(List<PlanCoalSnapshot> candidates) {
        BigDecimal min = null;
        BigDecimal max = null;
        for (PlanCoalSnapshot candidate : candidates) {
            BigDecimal price = candidate.getType() == null ? null : candidate.getType().getPurchasePrice();
            if (price == null) {
                continue;
            }
            min = min == null || price.compareTo(min) < 0 ? price : min;
            max = max == null || price.compareTo(max) > 0 ? price : max;
        }
        return new PriceRange(min, max);
    }

    private BigDecimal normalizedPrice(BigDecimal price, PriceRange priceRange) {
        if (price == null || priceRange == null || priceRange.min() == null || priceRange.max() == null
                || priceRange.max().compareTo(priceRange.min()) <= 0) {
            return BigDecimal.ZERO;
        }
        return price.subtract(priceRange.min())
                .divide(priceRange.max().subtract(priceRange.min()), 6, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(BigDecimal.ONE);
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
                                                            AiBlendCandidateResult aiResult,
                                                            BlendGenerationRuntimeConfig runtimeConfig) {
        if (aiResult == null || aiResult.getPlans() == null || aiResult.getPlans().isEmpty()) {
            return List.of();
        }
        List<EvaluatedPlanDraft> drafts = new ArrayList<>();
        for (AiBlendCandidatePlan aiPlan : aiResult.getPlans()) {
            AiDraftInput input = toAiDraftInput(shortlisted, aiPlan, runtimeConfig);
            if (input.snapshots().size() < 2 || input.ratios().size() != input.snapshots().size()) {
                continue;
            }
            EvaluatedPlanDraft draft = planScoreService.evaluate(order, input.snapshots(), input.ratios(),
                    runtimeConfig.getScoreStrategy(), runtimeConfig);
            draft.setCandidateSource("ai");
            draft.setAiCandidateReason(buildAiCandidateReason(aiPlan));
            drafts.add(draft);
        }
        return drafts.stream()
                .sorted(Comparator
                        .comparing((EvaluatedPlanDraft d) -> d.getConstraintResult().isFeasible(), Comparator.reverseOrder())
                        .thenComparing(d -> d.getScoreDetail().getOverallScore(), Comparator.reverseOrder())
                        .thenComparing(EvaluatedPlanDraft::getTotalCost))
                .collect(Collectors.toList());
    }

    private AiDraftInput toAiDraftInput(List<PlanCoalSnapshot> shortlisted, AiBlendCandidatePlan aiPlan,
                                        BlendGenerationRuntimeConfig runtimeConfig) {
        int maxMaterialCount = runtimeConfig == null || runtimeConfig.getMaxMaterialCount() == null
                ? 3 : runtimeConfig.getMaxMaterialCount();
        if (aiPlan == null || aiPlan.getItems() == null || aiPlan.getItems().size() < 2
                || aiPlan.getItems().size() > maxMaterialCount) {
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

    private List<EvaluatedPlanDraft> mergeDrafts(List<EvaluatedPlanDraft> aiDrafts,
                                                 List<EvaluatedPlanDraft> systemDrafts) {
        Map<String, EvaluatedPlanDraft> merged = new LinkedHashMap<>();
        for (EvaluatedPlanDraft d : aiDrafts) {
            merged.put(draftSignature(d), d);
        }
        for (EvaluatedPlanDraft d : systemDrafts) {
            merged.putIfAbsent(draftSignature(d), d);
        }
        return new ArrayList<>(merged.values());
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

    private List<EvaluatedPlanDraft> buildCandidateDrafts(Orders order, List<PlanCoalSnapshot> shortlisted,
                                                          BlendGenerationRuntimeConfig runtimeConfig) {
        List<List<PlanCoalSnapshot>> combinations = buildCombinations(shortlisted, runtimeConfig);
        List<EvaluatedPlanDraft> drafts = new ArrayList<>();
        int maxEvaluated = runtimeConfig.getMaxEvaluatedCandidates() == null
                ? 25000 : runtimeConfig.getMaxEvaluatedCandidates();
        for (List<PlanCoalSnapshot> combo : combinations) {
            for (List<BigDecimal> ratios : buildRatioTemplates(combo.size(), runtimeConfig)) {
                if (drafts.size() >= maxEvaluated) {
                    return drafts;
                }
                drafts.add(planScoreService.evaluate(order, combo, ratios,
                        runtimeConfig.getScoreStrategy(), runtimeConfig));
            }
        }
        return drafts;
    }

    private record AiDraftInput(List<PlanCoalSnapshot> snapshots, List<BigDecimal> ratios) {
    }

    private List<List<PlanCoalSnapshot>> buildCombinations(List<PlanCoalSnapshot> shortlisted,
                                                           BlendGenerationRuntimeConfig runtimeConfig) {
        List<List<PlanCoalSnapshot>> combinations = new ArrayList<>();
        int maxMaterialCount = runtimeConfig == null || runtimeConfig.getMaxMaterialCount() == null
                ? 3 : runtimeConfig.getMaxMaterialCount();
        for (int i = 0; i < shortlisted.size(); i++) {
            for (int j = i + 1; j < shortlisted.size(); j++) {
                combinations.add(List.of(shortlisted.get(i), shortlisted.get(j)));
                if (maxMaterialCount >= 3) {
                    for (int k = j + 1; k < shortlisted.size(); k++) {
                        combinations.add(List.of(shortlisted.get(i), shortlisted.get(j), shortlisted.get(k)));
                    }
                }
            }
        }
        return combinations;
    }

    private List<List<BigDecimal>> buildRatioTemplates(int size, BlendGenerationRuntimeConfig runtimeConfig) {
        List<List<BigDecimal>> ratios = new ArrayList<>();
        int stepUnit = ratioUnit(runtimeConfig.getRatioStep());
        int minUnit = ratioUnit(runtimeConfig.getMinSingleRatio());
        if (size == 2) {
            for (int a = minUnit; a <= TOTAL_RATIO_UNIT - minUnit; a += stepUnit) {
                int b = TOTAL_RATIO_UNIT - a;
                ratios.add(List.of(scaleRatio(a), scaleRatio(b)));
            }
            return ratios;
        }
        if (size == 3) {
            for (int a = minUnit; a <= TOTAL_RATIO_UNIT - 2 * minUnit; a += stepUnit) {
                for (int b = minUnit; b <= TOTAL_RATIO_UNIT - a - minUnit; b += stepUnit) {
                    int c = TOTAL_RATIO_UNIT - a - b;
                    if (c < minUnit) {
                        continue;
                    }
                    ratios.add(List.of(scaleRatio(a), scaleRatio(b), scaleRatio(c)));
                }
            }
            return ratios;
        }
        return List.of(List.of(BigDecimal.ONE));
    }

    private int ratioUnit(BigDecimal ratio) {
        BigDecimal value = ratio == null ? new BigDecimal("0.05") : ratio;
        return value.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal scaleRatio(int unit) {
        return new BigDecimal(unit).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    private List<ScoredPlan> persistPlans(Orders order, List<EvaluatedPlanDraft> drafts, Long createBy,
                                          Map<Long, CoalType> typeMap, String experimentCode,
                                          String aiModelName, RecommendationMode recommendationMode,
                                          GenerationConfigVO generationConfig) {
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
            plan.setFeasibleFlag(PlanDecisionStatus.INFEASIBLE.name().equals(draft.getDecisionStatus()) ? 0 : 1);
            plan.setConstraintSummary(buildConstraintSummary(constraint));
            plan.setScoreDetail(buildScoreDetailSummary(score));
            plan.setRiskLevel(constraint.riskLevel());
            plan.setCandidateSource(StringUtils.hasText(draft.getCandidateSource()) ? draft.getCandidateSource() : "system");
            plan.setAiCandidateReason(draft.getAiCandidateReason());
            plan.setDecisionStatus(draft.getDecisionStatus());
            plan.setRecommendationMode(i == 0 ? recommendationMode.name() : null);
            plan.setScoreStrategy(draft.getScoreStrategy());
            plan.setParetoRank(draft.getParetoRank());
            plan.setObjectiveCostPerTon(draft.getObjectiveCostPerTon());
            plan.setObjectiveQualityDeviation(draft.getObjectiveQualityDeviation());
            plan.setObjectiveExecutionRisk(draft.getObjectiveExecutionRisk());
            plan.setProblemItemsJson(toJson(draft.getProblemItems()));
            plan.setSuggestionItemsJson(toJson(draft.getSuggestionItems()));
            plan.setGenerationConfigJson(toJson(generationConfig));
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
            persisted.add(new ScoredPlan(plan.getId(), score.getOverallScore(), draft));
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
        vo.setFeasibleFlag(PlanDecisionStatus.INFEASIBLE.name().equals(draft.getDecisionStatus()) ? 0 : 1);
        vo.setConstraintSummary(buildConstraintSummary(constraint));
        vo.setScoreDetail(buildScoreDetailSummary(score));
        vo.setRiskLevel(constraint.riskLevel());
        vo.setRiskTip(draft.getRiskTip());
        vo.setDecisionStatus(draft.getDecisionStatus());
        vo.setDecisionStatusLabel(draft.getDecisionStatusLabel());
        vo.setRecommendationMode(draft.getRecommendationMode());
        vo.setProblemItems(draft.getProblemItems());
        vo.setSuggestionItems(draft.getSuggestionItems());
        vo.setParetoRank(draft.getParetoRank());
        vo.setDominatedCount(draft.getDominatedCount());
        vo.setDominatesCount(draft.getDominatesCount());
        vo.setObjectiveCostPerTon(draft.getObjectiveCostPerTon());
        vo.setObjectiveQualityDeviation(draft.getObjectiveQualityDeviation());
        vo.setObjectiveExecutionRisk(draft.getObjectiveExecutionRisk());
        vo.setScoreStrategy(draft.getScoreStrategy());
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

    private boolean isExecutableDecision(BlendPlan plan) {
        return plan == null || !StringUtils.hasText(plan.getDecisionStatus())
                || PlanDecisionStatus.FEASIBLE.name().equals(plan.getDecisionStatus());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("结构化决策字段序列化失败：" + e.getMessage());
        }
    }

    private void hydratePlanDecisionFields(BlendPlan plan) {
        if (plan == null) {
            return;
        }
        PlanDecisionStatus status = parseDecisionStatus(plan.getDecisionStatus(), plan.getFeasibleFlag());
        if (status != null) {
            plan.setDecisionStatus(status.name());
            plan.setDecisionStatusLabel(status.getLabel());
        }
        if (StringUtils.hasText(plan.getRecommendationMode())) {
            try {
                plan.setRecommendationModeLabel(RecommendationMode.valueOf(plan.getRecommendationMode()).getLabel());
            } catch (IllegalArgumentException ignored) {
                plan.setRecommendationModeLabel(plan.getRecommendationMode());
            }
        }
        plan.setProblemItems(readProblemItems(plan.getProblemItemsJson()));
        plan.setSuggestionItems(readSuggestionItems(plan.getSuggestionItemsJson()));
    }

    private PlanDecisionStatus parseDecisionStatus(String value, Integer feasibleFlag) {
        if (StringUtils.hasText(value)) {
            try {
                return PlanDecisionStatus.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        if (feasibleFlag == null) {
            return null;
        }
        return feasibleFlag == 0 ? PlanDecisionStatus.INFEASIBLE : PlanDecisionStatus.FEASIBLE;
    }

    private List<DecisionProblemItemVO> readProblemItems(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<DecisionProblemItemVO>>() {
            });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<DecisionSuggestionItemVO> readSuggestionItems(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<DecisionSuggestionItemVO>>() {
            });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private PlanWithDetailsVO toVo(Long planId, Map<Long, CoalType> typeMap) {
        BlendPlan p = blendPlanMapper.selectById(planId);
        hydratePlanDecisionFields(p);
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

    private record PriceRange(BigDecimal min, BigDecimal max) {
    }

    private record ScoredPlan(Long planId, BigDecimal overall, EvaluatedPlanDraft draft) {
    }

}
