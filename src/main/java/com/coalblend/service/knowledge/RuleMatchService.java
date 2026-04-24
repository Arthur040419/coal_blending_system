package com.coalblend.service.knowledge;

import com.coalblend.entity.Inventory;
import com.coalblend.entity.Orders;
import com.coalblend.vo.knowledge.MatchedRuleVO;

import java.util.List;
import java.util.Map;

public interface RuleMatchService {

    /**
     * 根据订单、候选煤种及库存摘要匹配启用规则。
     *
     * @param candidateCoalIds 参与配煤的煤种 ID（已筛可配煤、有煤质）
     * @param invMap           coalId -> 选用的一条库存记录
     */
    List<MatchedRuleVO> match(Orders order, List<Long> candidateCoalIds, Map<Long, Inventory> invMap);
}
