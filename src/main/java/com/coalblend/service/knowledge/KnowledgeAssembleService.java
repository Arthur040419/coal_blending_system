package com.coalblend.service.knowledge;

import com.coalblend.dto.knowledge.KnowledgeContextDTO;
import com.coalblend.entity.Inventory;
import com.coalblend.entity.Orders;
import com.coalblend.entity.CoalType;
import com.coalblend.vo.blend.PlanWithDetailsVO;
import com.coalblend.vo.knowledge.KnowledgeSummaryVO;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface KnowledgeAssembleService {

    /**
     * 组装知识上下文（第2步：在确定推荐方案后传入方案，生成 planText / 方案相关库存叙述等）。
     *
     * @param orderDemandQuantity 订单总需求量，用于判断方案涉及煤种是否“单库可覆盖”
     */
    KnowledgeContextDTO assemble(Orders order,
                                 Map<String, Object> constraints,
                                 Map<Long, CoalType> typeMap,
                                 Map<Long, Inventory> invMap,
                                 List<Long> rankedCoalIds,
                                 List<MatchedRuleVO> matchedRules,
                                 List<MatchedCaseVO> matchedCases,
                                 PlanWithDetailsVO recommendedPlan,
                                 BigDecimal orderDemandQuantity);

    KnowledgeSummaryVO summarize(KnowledgeContextDTO ctx);
}
