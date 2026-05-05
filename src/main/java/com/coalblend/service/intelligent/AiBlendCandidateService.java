package com.coalblend.service.intelligent;

import com.coalblend.entity.Orders;
import com.coalblend.service.intelligent.model.AiBlendCandidateResult;
import com.coalblend.service.intelligent.model.PlanCoalSnapshot;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;
import com.coalblend.vo.rag.RagRetrieveResultVO;

import java.util.List;

public interface AiBlendCandidateService {

    AiBlendCandidateResult generateCandidates(Orders order,
                                               List<PlanCoalSnapshot> candidates,
                                               List<MatchedRuleVO> matchedRules,
                                               List<MatchedCaseVO> matchedCases,
                                               RagRetrieveResultVO ragRetrieveResult,
                                               String candidateScope,
                                               Long modelConfigId);
}
