package com.coalblend.vo.knowledge;

import lombok.Data;

/**
 * 配煤流程中命中的规则（含命中原因，供接口与大模型 Prompt 使用）。
 */
@Data
public class MatchedRuleVO {

    private Long id;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private String ruleContent;
    private String applicableScope;
    private Integer priorityLevel;
    /** 为何在当前订单/库存场景下命中 */
    private String hitReason;

    public static MatchedRuleVO fromEntity(com.coalblend.entity.RuleKnowledge r, String hitReason) {
        MatchedRuleVO vo = new MatchedRuleVO();
        vo.setId(r.getId());
        vo.setRuleCode(r.getRuleCode());
        vo.setRuleName(r.getRuleName());
        vo.setRuleType(r.getRuleType());
        vo.setRuleContent(r.getRuleContent());
        vo.setApplicableScope(r.getApplicableScope());
        vo.setPriorityLevel(r.getPriorityLevel());
        vo.setHitReason(hitReason);
        return vo;
    }
}
