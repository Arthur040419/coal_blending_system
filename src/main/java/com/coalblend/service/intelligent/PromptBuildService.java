package com.coalblend.service.intelligent;

import com.coalblend.dto.AiExplainRequestDTO;
import com.coalblend.dto.knowledge.KnowledgeContextDTO;
import com.coalblend.entity.Orders;
import com.coalblend.vo.blend.PlanWithDetailsVO;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;

import java.util.List;

public interface PromptBuildService {

    AiExplainRequestDTO buildRequest(Orders order, PlanWithDetailsVO recommended,
                                     List<MatchedRuleVO> matchedRules,
                                     List<MatchedCaseVO> matchedCases,
                                     KnowledgeContextDTO knowledgeContext);

    /**
     * @param knowledgeContext 完整知识上下文；为 null 或缺少方案文本时使用降级 Prompt。
     */
    String buildPrompt(AiExplainRequestDTO dto, KnowledgeContextDTO knowledgeContext);
}
