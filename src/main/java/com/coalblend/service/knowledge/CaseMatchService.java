package com.coalblend.service.knowledge;

import com.coalblend.entity.Orders;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;

import java.util.List;

public interface CaseMatchService {

    /**
     * 检索与当前订单最相近的若干条启用案例（至多 3 条）。
     */
    List<MatchedCaseVO> match(Orders order, List<Long> rankedCoalIds, List<MatchedRuleVO> matchedRules);
}
