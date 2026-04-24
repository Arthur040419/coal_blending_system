package com.coalblend.service.intelligent;

import com.coalblend.dto.knowledge.KnowledgeContextDTO;
import com.coalblend.entity.Orders;
import com.coalblend.vo.AiExplainResultVO;
import com.coalblend.vo.blend.PlanWithDetailsVO;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;

import java.util.List;

public interface ModelInferenceService {

    /**
     * 调用大模型生成解释并写回 blend_plan；失败时使用兜底文案，不抛出到业务层。
     *
     * @param recommendedPlanId 推荐方案主键
     * @return 结构化结果（含是否真实调用模型）
     */
    AiExplainResultVO enrichRecommendedPlan(Long recommendedPlanId, Orders order, PlanWithDetailsVO recommended,
                                            List<MatchedRuleVO> matchedRules, List<MatchedCaseVO> matchedCases,
                                            KnowledgeContextDTO knowledgeContext);
}
