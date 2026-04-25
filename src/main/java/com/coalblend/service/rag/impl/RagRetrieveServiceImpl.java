package com.coalblend.service.rag.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coalblend.entity.Orders;
import com.coalblend.entity.RagKnowledge;
import com.coalblend.mapper.RagKnowledgeMapper;
import com.coalblend.service.rag.RagRetrieveService;
import com.coalblend.vo.rag.RagKnowledgeHitVO;
import com.coalblend.vo.rag.RagRetrieveResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagRetrieveServiceImpl implements RagRetrieveService {

    private static final int DEFAULT_PER_KEYWORD_LIMIT = 8;

    private final RagKnowledgeMapper ragKnowledgeMapper;

    @Override
    public RagRetrieveResultVO retrieveByOrder(Orders order, int topK) {
        int limit = topK <= 0 ? DEFAULT_PER_KEYWORD_LIMIT : topK;
        List<String> keywords = buildKeywordsByOrder(order);
        Map<Long, RagKnowledgeHitVO> merged = new LinkedHashMap<>();

        for (String keyword : keywords) {
            for (RagKnowledge k : searchRaw(keyword, limit)) {
                RagKnowledgeHitVO hit = merged.computeIfAbsent(k.getId(), id -> RagKnowledgeHitVO.fromEntity(k));
                double score = score(k, keyword);
                hit.setScore(hit.getScore() + score);
                if (!hit.getHitKeywords().contains(keyword)) {
                    hit.getHitKeywords().add(keyword);
                }
            }
        }

        List<RagKnowledgeHitVO> all = merged.values().stream()
                .peek(this::fillHitReason)
                .sorted(Comparator.comparingDouble(RagKnowledgeHitVO::getScore).reversed()
                        .thenComparing(RagKnowledgeHitVO::getId))
                .collect(Collectors.toList());

        RagRetrieveResultVO result = new RagRetrieveResultVO();
        result.setOrderId(order == null ? null : order.getId());
        result.setKeywords(keywords);
        result.setQueryText(String.join(" ", keywords));
        result.setAll(all);
        result.setMatchedKnowledgeIds(all.stream().map(RagKnowledgeHitVO::getId).collect(Collectors.toList()));
        result.setRules(filterByType(all, "rule", 3));
        result.setCases(filterByType(all, "case", 2));
        result.setTerms(filterByType(all, "term", 2));
        result.setDocs(filterByType(all, "doc", 2));
        return result;
    }

    @Override
    public String buildKnowledgeText(RagRetrieveResultVO result) {
        if (result == null || result.getAll() == null || result.getAll().isEmpty()) {
            return "（未检索到 RAG 知识库条目）";
        }
        StringBuilder sb = new StringBuilder();
        appendGroup(sb, "规则知识", result.getRules());
        appendGroup(sb, "历史案例", result.getCases());
        appendGroup(sb, "术语说明", result.getTerms());
        appendGroup(sb, "业务文档", result.getDocs());
        return sb.toString().trim();
    }

    private List<RagKnowledge> searchRaw(String keyword, int topK) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return ragKnowledgeMapper.selectList(new LambdaQueryWrapper<RagKnowledge>()
                .eq(RagKnowledge::getStatus, 1)
                .and(w -> w.like(RagKnowledge::getTitle, keyword)
                        .or().like(RagKnowledge::getTags, keyword)
                        .or().like(RagKnowledge::getContent, keyword))
                .orderByDesc(RagKnowledge::getUpdateTime)
                .last("LIMIT " + Math.max(1, topK)));
    }

    private List<String> buildKeywordsByOrder(Orders order) {
        Set<String> out = new LinkedHashSet<>();
        if (order != null) {
            if (lte(order.getTargetSulfur(), "0.8")) {
                out.add("低硫");
                out.add("硫分约束");
                out.add("高硫煤限配");
            } else if (order.getTargetSulfur() != null) {
                out.add("硫分控制");
            }
            if (gte(order.getTargetCalorific(), "5000")) {
                out.add("高热值");
                out.add("热值补偿");
                out.add("动力煤");
            } else if (order.getTargetCalorific() != null) {
                out.add("发热量");
            }
            if (lte(order.getTargetAsh(), "18")) {
                out.add("低灰");
                out.add("灰分控制");
            } else if (order.getTargetAsh() != null) {
                out.add("灰分约束");
            }
            if (lte(order.getTargetMoisture(), "8.5")) {
                out.add("水分控制");
            }
            if (order.getPriorityLevel() != null && order.getPriorityLevel() >= 3) {
                out.add("高优先级");
                out.add("交付保障");
                out.add("稳定性");
            }
        }
        out.add("配煤方案");
        out.add("质量约束");
        out.add("成本控制");
        out.add("历史案例");
        return new ArrayList<>(out);
    }

    private static boolean lte(BigDecimal v, String threshold) {
        return v != null && v.compareTo(new BigDecimal(threshold)) <= 0;
    }

    private static boolean gte(BigDecimal v, String threshold) {
        return v != null && v.compareTo(new BigDecimal(threshold)) >= 0;
    }

    private double score(RagKnowledge k, String keyword) {
        String kw = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        double score = 1;
        if (contains(k.getTitle(), kw)) {
            score += 10;
        }
        if (contains(k.getTags(), kw)) {
            score += 8;
        }
        if (contains(k.getContent(), kw)) {
            score += 5;
        }
        return score;
    }

    private static boolean contains(String text, String kw) {
        return StringUtils.hasText(text) && StringUtils.hasText(kw)
                && text.toLowerCase(Locale.ROOT).contains(kw);
    }

    private void fillHitReason(RagKnowledgeHitVO hit) {
        String kw = hit.getHitKeywords() == null || hit.getHitKeywords().isEmpty()
                ? "订单特征"
                : String.join("、", hit.getHitKeywords());
        hit.setHitReason("命中订单关键词：" + kw);
    }

    private List<RagKnowledgeHitVO> filterByType(List<RagKnowledgeHitVO> all, String type, int limit) {
        return all.stream()
                .filter(h -> matchType(h.getKnowledgeType(), type))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private static boolean matchType(String actual, String expected) {
        if (!StringUtils.hasText(actual) || !StringUtils.hasText(expected)) {
            return false;
        }
        String a = actual.toLowerCase(Locale.ROOT);
        String e = expected.toLowerCase(Locale.ROOT);
        if ("rule".equals(e)) {
            return a.equals("rule") || a.endsWith("_rule") || a.contains("规则");
        }
        if ("doc".equals(e)) {
            return a.equals("doc") || a.equals("document") || a.contains("文档");
        }
        return a.equals(e);
    }

    private void appendGroup(StringBuilder sb, String title, List<RagKnowledgeHitVO> hits) {
        if (hits == null || hits.isEmpty()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append("\n\n");
        }
        sb.append("【").append(title).append("】\n");
        for (int i = 0; i < hits.size(); i++) {
            RagKnowledgeHitVO h = hits.get(i);
            sb.append(i + 1).append(". ")
                    .append(nz(h.getTitle())).append("（")
                    .append(nz(h.getKnowledgeType())).append("，")
                    .append(h.getHitReason()).append("）\n")
                    .append(nz(h.getContent())).append("\n");
        }
    }

    private static String nz(String s) {
        return StringUtils.hasText(s) ? s : "—";
    }
}
