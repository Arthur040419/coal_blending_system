package com.coalblend.vo.knowledge;

import lombok.Data;

/**
 * 与当前订单场景相近的历史案例（供接口与大模型 Prompt 使用）。
 */
@Data
public class MatchedCaseVO {

    private Long id;
    private String caseCode;
    private String caseName;
    /** 订单+方案要点摘要 */
    private String summary;
    private String effectivenessEval;
    /** 为何检索到该案例 */
    private String matchReason;

    public static MatchedCaseVO of(Long id, String caseCode, String caseName, String summary,
                                    String effectivenessEval, String matchReason) {
        MatchedCaseVO vo = new MatchedCaseVO();
        vo.setId(id);
        vo.setCaseCode(caseCode);
        vo.setCaseName(caseName);
        vo.setSummary(summary);
        vo.setEffectivenessEval(effectivenessEval);
        vo.setMatchReason(matchReason);
        return vo;
    }
}
