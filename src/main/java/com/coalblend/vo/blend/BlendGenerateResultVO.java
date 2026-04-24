package com.coalblend.vo.blend;

import com.coalblend.dto.knowledge.KnowledgeContextDTO;
import com.coalblend.entity.Orders;
import com.coalblend.vo.knowledge.KnowledgeSummaryVO;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;
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
    /** 知识库：命中规则（含命中原因） */
    private List<MatchedRuleVO> matchedRules;
    /** 知识库：参考案例（含匹配原因与摘要） */
    private List<MatchedCaseVO> matchedCases;
    /** 知识命中概览（便于前端与论文展示） */
    private KnowledgeSummaryVO knowledgeSummary;
    /** 完整知识上下文（含可拼 Prompt 的文本字段） */
    private KnowledgeContextDTO knowledgeContext;
    private String explainSummary;
}
