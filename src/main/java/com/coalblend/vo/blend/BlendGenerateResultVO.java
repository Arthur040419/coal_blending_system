package com.coalblend.vo.blend;

import com.coalblend.dto.knowledge.KnowledgeContextDTO;
import com.coalblend.entity.Orders;
import com.coalblend.vo.knowledge.KnowledgeSummaryVO;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;
import com.coalblend.vo.AiExplainResultVO;
import com.coalblend.vo.rag.RagRetrieveResultVO;
import com.coalblend.service.intelligent.model.AiBlendCandidateResult;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class BlendGenerateResultVO {

    private Orders order;
    private Map<String, Object> constraints = new LinkedHashMap<>();
    private PlanWithDetailsVO recommendedPlan;
    private List<PlanWithDetailsVO> candidatePlans;
    /** 本次配煤生成时进入搜索与大模型提示词的候选物料短名单 */
    private List<CandidateMaterialVO> candidateMaterials;
    /** 本次生成过程中 AI 候选方案的完整评分结果（未必全部落库） */
    private List<CandidateEvaluationItemVO> aiEvaluatedCandidates;
    /** 本次生成过程中系统枚举候选方案的完整评分结果（未必全部落库） */
    private List<CandidateEvaluationItemVO> systemEvaluatedCandidates;
    /** 知识库：命中规则（含命中原因） */
    private List<MatchedRuleVO> matchedRules;
    /** 知识库：参考案例（含匹配原因与摘要） */
    private List<MatchedCaseVO> matchedCases;
    /** 知识命中概览（便于前端与论文展示） */
    private KnowledgeSummaryVO knowledgeSummary;
    /** 完整知识上下文（含可拼 Prompt 的文本字段） */
    private KnowledgeContextDTO knowledgeContext;
    /** RAG 统一知识库检索结果 */
    private RagRetrieveResultVO ragRetrieveResult;
    /** 大模型参与候选方案生成结果 */
    private AiBlendCandidateResult aiCandidateResult;
    /** 大模型基于 RAG 上下文生成的解释结果 */
    private AiExplainResultVO ragExplanation;
    private String explainSummary;

    /** 总体决策状态：FEASIBLE / RISKY / INFEASIBLE */
    private String decisionStatus;
    /** 决策状态中文名 */
    private String decisionStatusLabel;
    /** 推荐模式：NORMAL / RISK_REFERENCE / NO_SOLUTION */
    private String recommendationMode;
    /** 推荐模式中文说明 */
    private String recommendationModeLabel;
    /** 总体决策摘要 */
    private String decisionSummary;
    /** 当前订单和候选空间下的主要问题 */
    private List<DecisionProblemItemVO> problemItems;
    /** 面向用户的调整建议 */
    private List<DecisionSuggestionItemVO> suggestionItems;
    /** 多目标 Pareto 概览 */
    private ParetoSummaryVO paretoSummary;
    /** 本次生成使用的评分和搜索配置 */
    private GenerationConfigVO generationConfig;
}
