package com.coalblend.service.rag.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coalblend.common.config.CoalRagProperties;
import com.coalblend.entity.Orders;
import com.coalblend.entity.RagChunk;
import com.coalblend.entity.RagDocument;
import com.coalblend.entity.RagKnowledge;
import com.coalblend.mapper.RagChunkMapper;
import com.coalblend.mapper.RagDocumentMapper;
import com.coalblend.mapper.RagKnowledgeMapper;
import com.coalblend.service.rag.RagEmbeddingService;
import com.coalblend.service.rag.RagRetrieveService;
import com.coalblend.service.rag.RagVectorStoreService;
import com.coalblend.service.rag.model.VectorSearchHit;
import com.coalblend.vo.rag.RagKnowledgeHitVO;
import com.coalblend.vo.rag.RagRetrieveResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrieveServiceImpl implements RagRetrieveService {

    private static final int DEFAULT_TOP_K = 8;

    private final CoalRagProperties props;
    private final RagKnowledgeMapper ragKnowledgeMapper;
    private final RagDocumentMapper ragDocumentMapper;
    private final RagChunkMapper ragChunkMapper;
    private final RagEmbeddingService embeddingService;
    private final RagVectorStoreService vectorStoreService;

    @Override
    public RagRetrieveResultVO retrieveByOrder(Orders order, int topK) {
        int limit = topK <= 0 ? DEFAULT_TOP_K : topK;
        List<String> keywords = buildKeywordsByOrder(order);
        String queryText = buildQueryText(order, keywords);
        Map<Long, MergeHit> merged = new LinkedHashMap<>();

        boolean vectorUsed = false;
        if (props.isVectorEnabled()) {
            try {
                List<Double> queryVector = embeddingService.embed(queryText);
                if (!queryVector.isEmpty() && vectorStoreService.available()) {
                    vectorUsed = true;
                    for (VectorSearchHit vectorHit : vectorStoreService.search(queryVector, limit * 4)) {
                        RagChunk chunk = vectorHit.getChunkId() == null ? null : ragChunkMapper.selectById(vectorHit.getChunkId());
                        if (chunk == null || chunk.getStatus() == null || chunk.getStatus() != 1) {
                            continue;
                        }
                        MergeHit hit = merged.computeIfAbsent(chunk.getId(), id -> new MergeHit(toHit(chunk)));
                        hit.vectorScore = Math.max(hit.vectorScore, vectorHit.getScore() == null ? 0.0 : vectorHit.getScore());
                        hit.modes.add("vector");
                    }
                }
            } catch (Exception e) {
                log.warn("Vector RAG retrieve failed, fallback to keyword: {}", e.getMessage());
            }
        }

        for (String keyword : keywords) {
            for (RagChunk chunk : searchChunks(keyword, limit)) {
                MergeHit hit = merged.computeIfAbsent(chunk.getId(), id -> new MergeHit(toHit(chunk)));
                hit.keywordScore += keywordScore(chunk, keyword);
                hit.hit.getHitKeywords().add(keyword);
                hit.modes.add("keyword");
            }
        }

        if (merged.isEmpty()) {
            return legacyKeywordRetrieve(order, keywords, queryText, limit);
        }

        List<RagKnowledgeHitVO> all = merged.values().stream()
                .map(hit -> finalizeHit(hit, keywords))
                .sorted(Comparator.comparingDouble(RagKnowledgeHitVO::getScore).reversed()
                        .thenComparing(RagKnowledgeHitVO::getChunkId, Comparator.nullsLast(Long::compareTo)))
                .limit(limit)
                .collect(Collectors.toList());

        RagRetrieveResultVO result = new RagRetrieveResultVO();
        result.setOrderId(order == null ? null : order.getId());
        result.setKeywords(keywords);
        result.setQueryText(queryText);
        result.setRetrievalMode(vectorUsed ? "hybrid" : "keyword");
        result.setEmbeddingModel(embeddingService.modelName());
        result.setAll(all);
        result.setMatchedKnowledgeIds(all.stream()
                .map(RagKnowledgeHitVO::getDocumentId)
                .filter(id -> id != null)
                .collect(Collectors.toList()));
        result.setMatchedChunkIds(all.stream()
                .map(RagKnowledgeHitVO::getChunkId)
                .filter(id -> id != null)
                .collect(Collectors.toList()));
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

    private RagKnowledgeHitVO toHit(RagChunk chunk) {
        RagDocument doc = chunk.getDocumentId() == null ? null : ragDocumentMapper.selectById(chunk.getDocumentId());
        RagKnowledgeHitVO hit = new RagKnowledgeHitVO();
        hit.setId(chunk.getId());
        hit.setChunkId(chunk.getId());
        hit.setDocumentId(chunk.getDocumentId());
        hit.setKnowledgeCode(chunk.getChunkCode());
        hit.setTitle(doc == null ? chunk.getChunkCode() : doc.getTitle());
        hit.setKnowledgeType(doc == null ? "doc" : doc.getDocType());
        hit.setContent(chunk.getChunkText());
        hit.setSourceTable(chunk.getSourceType());
        hit.setSourceId(chunk.getSourceId());
        hit.setTags(StringUtils.hasText(chunk.getTags()) ? chunk.getTags() : (doc == null ? null : doc.getTags()));
        return hit;
    }

    private RagKnowledgeHitVO finalizeHit(MergeHit merge, List<String> keywords) {
        RagKnowledgeHitVO hit = merge.hit;
        hit.setVectorScore(merge.vectorScore);
        hit.setKeywordScore(merge.keywordScore);
        hit.setBusinessScore(businessScore(hit, keywords));
        hit.setRetrievalMode(String.join("+", merge.modes));

        double vectorPart = clamp01(merge.vectorScore) * props.getVectorWeight();
        double keywordPart = Math.min(1.0, merge.keywordScore / 30.0) * props.getKeywordWeight();
        double businessPart = clamp01(hit.getBusinessScore()) * props.getBusinessWeight();
        hit.setScore(vectorPart + keywordPart + businessPart);
        fillHitReason(hit);
        return hit;
    }

    private List<RagChunk> searchChunks(String keyword, int topK) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return ragChunkMapper.selectList(new LambdaQueryWrapper<RagChunk>()
                .eq(RagChunk::getStatus, 1)
                .and(w -> w.like(RagChunk::getChunkText, keyword)
                        .or().like(RagChunk::getTags, keyword))
                .orderByDesc(RagChunk::getUpdateTime)
                .last("LIMIT " + Math.max(1, topK)));
    }

    private RagRetrieveResultVO legacyKeywordRetrieve(Orders order, List<String> keywords, String queryText, int limit) {
        Map<Long, RagKnowledgeHitVO> merged = new LinkedHashMap<>();
        for (String keyword : keywords) {
            for (RagKnowledge k : searchRaw(keyword, limit)) {
                RagKnowledgeHitVO hit = merged.computeIfAbsent(k.getId(), id -> RagKnowledgeHitVO.fromEntity(k));
                hit.setKeywordScore((hit.getKeywordScore() == null ? 0.0 : hit.getKeywordScore()) + legacyScore(k, keyword));
                hit.setScore(hit.getScore() + legacyScore(k, keyword));
                hit.setRetrievalMode("keyword");
                if (!hit.getHitKeywords().contains(keyword)) {
                    hit.getHitKeywords().add(keyword);
                }
            }
        }
        List<RagKnowledgeHitVO> all = merged.values().stream()
                .peek(this::fillHitReason)
                .sorted(Comparator.comparingDouble(RagKnowledgeHitVO::getScore).reversed()
                        .thenComparing(RagKnowledgeHitVO::getId))
                .limit(limit)
                .collect(Collectors.toList());

        RagRetrieveResultVO result = new RagRetrieveResultVO();
        result.setOrderId(order == null ? null : order.getId());
        result.setKeywords(keywords);
        result.setQueryText(queryText);
        result.setRetrievalMode("keyword");
        result.setEmbeddingModel(embeddingService.modelName());
        result.setAll(all);
        result.setMatchedKnowledgeIds(all.stream().map(RagKnowledgeHitVO::getId).collect(Collectors.toList()));
        result.setRules(filterByType(all, "rule", 3));
        result.setCases(filterByType(all, "case", 2));
        result.setTerms(filterByType(all, "term", 2));
        result.setDocs(filterByType(all, "doc", 2));
        return result;
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

    private String buildQueryText(Orders order, List<String> keywords) {
        if (order == null) {
            return String.join(" ", keywords);
        }
        return "配煤订单 检索需求："
                + "需求量" + fmt(order.getDemandQuantity()) + "吨；"
                + "灰分上限" + fmt(order.getTargetAsh()) + "%；"
                + "硫分上限" + fmt(order.getTargetSulfur()) + "%；"
                + "水分上限" + fmt(order.getTargetMoisture()) + "%；"
                + "发热量下限" + fmt(order.getTargetCalorific()) + "kcal/kg；"
                + "优先级" + (order.getPriorityLevel() == null ? "—" : order.getPriorityLevel())
                + "；关键词：" + String.join(" ", keywords);
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

    private double keywordScore(RagChunk chunk, String keyword) {
        String kw = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        double score = 1;
        if (contains(chunk.getTags(), kw)) {
            score += 8;
        }
        if (contains(chunk.getChunkText(), kw)) {
            score += 5;
        }
        return score;
    }

    private double legacyScore(RagKnowledge k, String keyword) {
        String kw = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        double score = 1;
        if (contains(k.getTitle(), kw)) score += 10;
        if (contains(k.getTags(), kw)) score += 8;
        if (contains(k.getContent(), kw)) score += 5;
        return score;
    }

    private double businessScore(RagKnowledgeHitVO hit, List<String> keywords) {
        double score = 0.0;
        String haystack = (nz(hit.getTitle()) + " " + nz(hit.getTags()) + " " + nz(hit.getContent()))
                .toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                score += 0.12;
            }
        }
        if ("rule".equalsIgnoreCase(hit.getKnowledgeType())) score += 0.1;
        if ("case".equalsIgnoreCase(hit.getKnowledgeType())) score += 0.08;
        return Math.min(1.0, score);
    }

    private void fillHitReason(RagKnowledgeHitVO hit) {
        String kw = hit.getHitKeywords() == null || hit.getHitKeywords().isEmpty()
                ? "向量语义相似"
                : String.join("、", hit.getHitKeywords());
        String evidence = hit.getChunkId() == null ? "" : "；证据片段：RAG-CHUNK-" + hit.getChunkId();
        hit.setHitReason("召回方式：" + nz(hit.getRetrievalMode()) + "；命中依据：" + kw + evidence);
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
            String evidenceId = h.getChunkId() == null ? String.valueOf(h.getId()) : "RAG-CHUNK-" + h.getChunkId();
            sb.append(i + 1).append(". [").append(evidenceId).append("] ")
                    .append(nz(h.getTitle())).append("（")
                    .append(nz(h.getKnowledgeType())).append("，")
                    .append(h.getHitReason()).append("，综合分")
                    .append(String.format(Locale.ROOT, "%.4f", h.getScore())).append("）\n")
                    .append(nz(h.getContent())).append("\n");
        }
    }

    private static boolean lte(BigDecimal v, String threshold) {
        return v != null && v.compareTo(new BigDecimal(threshold)) <= 0;
    }

    private static boolean gte(BigDecimal v, String threshold) {
        return v != null && v.compareTo(new BigDecimal(threshold)) >= 0;
    }

    private static boolean contains(String text, String kw) {
        return StringUtils.hasText(text) && StringUtils.hasText(kw)
                && text.toLowerCase(Locale.ROOT).contains(kw);
    }

    private double clamp01(Double value) {
        if (value == null) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String fmt(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }

    private static String nz(String s) {
        return StringUtils.hasText(s) ? s : "—";
    }

    private static class MergeHit {
        private final RagKnowledgeHitVO hit;
        private double vectorScore = 0.0;
        private double keywordScore = 0.0;
        private final Set<String> modes = new LinkedHashSet<>();

        private MergeHit(RagKnowledgeHitVO hit) {
            this.hit = hit;
        }
    }
}
