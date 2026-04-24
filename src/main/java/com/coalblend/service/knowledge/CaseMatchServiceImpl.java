package com.coalblend.service.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coalblend.entity.CaseSample;
import com.coalblend.entity.Orders;
import com.coalblend.mapper.CaseSampleMapper;
import com.coalblend.vo.knowledge.MatchedCaseVO;
import com.coalblend.vo.knowledge.MatchedRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CaseMatchServiceImpl implements CaseMatchService {

    private static final int MAX_RESULTS = 3;
    private static final BigDecimal LARGE_DEMAND = new BigDecimal("5000");
    private static final BigDecimal SULFUR_STRICT = new BigDecimal("0.8");
    private static final BigDecimal CAL_HIGH = new BigDecimal("5000");

    private final CaseSampleMapper caseSampleMapper;

    @Override
    public List<MatchedCaseVO> match(Orders order, List<Long> rankedCoalIds, List<MatchedRuleVO> matchedRules) {
        List<CaseSample> all = caseSampleMapper.selectList(new LambdaQueryWrapper<CaseSample>()
                .eq(CaseSample::getStatus, 1)
                .orderByDesc(CaseSample::getId));

        record Scored(CaseSample sample, int score, String reason) {
        }

        List<Scored> scored = new ArrayList<>();
        for (CaseSample c : all) {
            ScoreResult sr = scoreCase(c, order, matchedRules, rankedCoalIds);
            if (sr.score() > 0) {
                scored.add(new Scored(c, sr.score(), sr.reason()));
            }
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed());

        List<MatchedCaseVO> out = new ArrayList<>();
        if (scored.isEmpty()) {
            for (int i = 0; i < Math.min(MAX_RESULTS, all.size()); i++) {
                CaseSample c = all.get(i);
                out.add(toVo(c, "启用案例（弱特征匹配，作泛化参考）"));
            }
            return out;
        }
        for (int i = 0; i < Math.min(MAX_RESULTS, scored.size()); i++) {
            Scored s = scored.get(i);
            out.add(toVo(s.sample(), s.reason()));
        }
        return out;
    }

    private record ScoreResult(int score, String reason) {
    }

    private ScoreResult scoreCase(CaseSample c, Orders order, List<MatchedRuleVO> matchedRules,
                                  List<Long> rankedCoalIds) {
        String blob = (nz(c.getCaseName()) + " " + nz(c.getOrderDesc()) + " " + nz(c.getBlendDesc()) + " " + nz(c.getResultDesc()))
                .toLowerCase(Locale.ROOT);
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (order.getTargetSulfur() != null && order.getTargetSulfur().compareTo(SULFUR_STRICT) <= 0) {
            if (containsAny(blob, "低硫", "0.8", "硫分不高于", "低硫动力")) {
                score += 45;
                reasons.add("低硫场景关键词命中");
            }
        }
        if (order.getTargetCalorific() != null && order.getTargetCalorific().compareTo(CAL_HIGH) >= 0) {
            if (containsAny(blob, "高热值", "热值不低于", "热值补偿", "5300", "5000大卡", "5000")) {
                score += 40;
                reasons.add("高热值场景关键词命中");
            }
        }
        if (order.getDemandQuantity() != null && order.getDemandQuantity().compareTo(LARGE_DEMAND) >= 0) {
            if (containsAny(blob, "大批量", "5000吨", "大订单", "万吨")) {
                score += 28;
                reasons.add("大批量需求关键词命中");
            }
        }
        if (containsAny(blob, "库存紧", "库存不足", "库存偏紧", "紧张")) {
            score += 30;
            reasons.add("库存紧张类案例");
        }
        if (containsAny(blob, "低成本", "成本较低", "成本适中")) {
            if (order.getPriorityLevel() != null && order.getPriorityLevel() >= 3) {
                score += 22;
                reasons.add("成本优先与案例表述一致");
            }
        }
        if (matchedRules != null) {
            for (MatchedRuleVO mr : matchedRules) {
                if (mr.getRuleName() != null && blob.contains(mr.getRuleName().toLowerCase(Locale.ROOT))) {
                    score += 8;
                    reasons.add("与命中规则名称相关");
                    break;
                }
            }
        }

        String reason = reasons.isEmpty() ? "" : String.join("；", reasons);
        int bonus = (rankedCoalIds != null && rankedCoalIds.size() >= 3) ? 2 : 0;
        return new ScoreResult(score + bonus, reason);
    }

    private static MatchedCaseVO toVo(CaseSample c, String matchReason) {
        String summary = buildSummary(c);
        String reason = matchReason == null || matchReason.isBlank() ? "与当前订单约束关键词部分匹配" : matchReason;
        return MatchedCaseVO.of(c.getId(), c.getCaseCode(), c.getCaseName(), summary, c.getEffectivenessEval(), reason);
    }

    private static String buildSummary(CaseSample c) {
        StringBuilder sb = new StringBuilder();
        if (c.getOrderDesc() != null && !c.getOrderDesc().isBlank()) {
            sb.append(trimLen(c.getOrderDesc(), 120));
        }
        if (c.getBlendDesc() != null && !c.getBlendDesc().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(trimLen(c.getBlendDesc(), 120));
        }
        if (sb.isEmpty()) {
            sb.append(c.getCaseName() == null ? "历史案例" : c.getCaseName());
        }
        return sb.toString();
    }

    private static String trimLen(String s, int max) {
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "…";
    }

    private static boolean containsAny(String blob, String... keys) {
        for (String k : keys) {
            if (k != null && blob.contains(k.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
