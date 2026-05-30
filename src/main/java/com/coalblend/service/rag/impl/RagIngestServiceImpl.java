package com.coalblend.service.rag.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coalblend.common.config.CoalRagProperties;
import com.coalblend.entity.CaseSample;
import com.coalblend.entity.RagChunk;
import com.coalblend.entity.RagDocument;
import com.coalblend.entity.RagKnowledge;
import com.coalblend.entity.RuleKnowledge;
import com.coalblend.mapper.CaseSampleMapper;
import com.coalblend.mapper.RagChunkMapper;
import com.coalblend.mapper.RagDocumentMapper;
import com.coalblend.mapper.RagKnowledgeMapper;
import com.coalblend.mapper.RuleKnowledgeMapper;
import com.coalblend.service.rag.RagEmbeddingService;
import com.coalblend.service.rag.RagIngestService;
import com.coalblend.service.rag.RagVectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagIngestServiceImpl implements RagIngestService {

    private final CoalRagProperties props;
    private final RagKnowledgeMapper ragKnowledgeMapper;
    private final RuleKnowledgeMapper ruleKnowledgeMapper;
    private final CaseSampleMapper caseSampleMapper;
    private final RagDocumentMapper ragDocumentMapper;
    private final RagChunkMapper ragChunkMapper;
    private final RagEmbeddingService embeddingService;
    private final RagVectorStoreService vectorStoreService;

    @Override
    public Map<String, Object> ingestAll() {
        int docs = 0;
        int chunks = 0;
        for (RagKnowledge row : ragKnowledgeMapper.selectList(new LambdaQueryWrapper<RagKnowledge>().eq(RagKnowledge::getStatus, 1))) {
            Map<String, Object> result = ingestRagKnowledge(row.getId());
            docs += intValue(result.get("documents"));
            chunks += intValue(result.get("chunks"));
        }
        for (RuleKnowledge row : ruleKnowledgeMapper.selectList(new LambdaQueryWrapper<RuleKnowledge>().eq(RuleKnowledge::getStatus, 1))) {
            Map<String, Object> result = ingestRule(row.getId());
            docs += intValue(result.get("documents"));
            chunks += intValue(result.get("chunks"));
        }
        for (CaseSample row : caseSampleMapper.selectList(new LambdaQueryWrapper<CaseSample>().eq(CaseSample::getStatus, 1))) {
            Map<String, Object> result = ingestCase(row.getId());
            docs += intValue(result.get("documents"));
            chunks += intValue(result.get("chunks"));
        }
        return result("all", docs, chunks);
    }

    @Override
    public Map<String, Object> ingestRagKnowledge(Long id) {
        RagKnowledge row = ragKnowledgeMapper.selectById(id);
        if (row == null) {
            return result("rag_knowledge", 0, 0);
        }
        SourceDocument doc = new SourceDocument(
                "rag_knowledge",
                row.getId(),
                row.getKnowledgeCode(),
                row.getTitle(),
                normalizeType(row.getKnowledgeType()),
                row.getTags(),
                row.getContent());
        return ingest(doc);
    }

    @Override
    public Map<String, Object> ingestRule(Long id) {
        RuleKnowledge row = ruleKnowledgeMapper.selectById(id);
        if (row == null) {
            return result("rule_knowledge", 0, 0);
        }
        String tags = join(row.getRuleType(), row.getApplicableScope(), row.getBusinessStage(),
                row.getQualityIndicator(), row.getMaterialType());
        String content = """
                规则编号：%s
                规则名称：%s
                规则类型：%s
                适用范围：%s
                业务阶段：%s
                质量指标：%s
                物料类型：%s
                规则内容：%s
                来源说明：%s
                """.formatted(nz(row.getRuleCode()), nz(row.getRuleName()), nz(row.getRuleType()),
                nz(row.getApplicableScope()), nz(row.getBusinessStage()), nz(row.getQualityIndicator()),
                nz(row.getMaterialType()), nz(row.getRuleContent()), nz(row.getSourceDesc()));
        return ingest(new SourceDocument("rule_knowledge", row.getId(), row.getRuleCode(), row.getRuleName(),
                "rule", tags, content));
    }

    @Override
    public Map<String, Object> ingestCase(Long id) {
        CaseSample row = caseSampleMapper.selectById(id);
        if (row == null) {
            return result("case_sample", 0, 0);
        }
        String tags = join(row.getBusinessStage(), row.getQualityResult(), row.getEffectivenessEval(), row.getRelatedBatchNo());
        String content = """
                案例编号：%s
                案例名称：%s
                订单描述：%s
                配煤方案：%s
                执行结果：%s
                质量结果：%s
                成本结果：%s
                效果评价：%s
                业务阶段：%s
                相关批次：%s
                """.formatted(nz(row.getCaseCode()), nz(row.getCaseName()), nz(row.getOrderDesc()),
                nz(row.getBlendDesc()), nz(row.getResultDesc()), nz(row.getQualityResult()),
                row.getCostResult() == null ? "—" : row.getCostResult().toPlainString(),
                nz(row.getEffectivenessEval()), nz(row.getBusinessStage()), nz(row.getRelatedBatchNo()));
        return ingest(new SourceDocument("case_sample", row.getId(), row.getCaseCode(), row.getCaseName(),
                "case", tags, content));
    }

    private Map<String, Object> ingest(SourceDocument source) {
        if (source == null || !StringUtils.hasText(source.text())) {
            return result("empty", 0, 0);
        }
        deleteExisting(source.sourceType(), source.sourceId());

        RagDocument doc = new RagDocument();
        doc.setDocCode(firstText(source.code(), source.sourceType() + "-" + source.sourceId()));
        doc.setTitle(firstText(source.title(), doc.getDocCode()));
        doc.setDocType(source.docType());
        doc.setSourceType(source.sourceType());
        doc.setSourceId(source.sourceId());
        doc.setTags(source.tags());
        doc.setStatus(1);
        doc.setCreateTime(LocalDateTime.now());
        doc.setUpdateTime(LocalDateTime.now());
        ragDocumentMapper.insert(doc);

        List<String> parts = split(source.text());
        int chunkCount = 0;
        for (int i = 0; i < parts.size(); i++) {
            String text = parts.get(i);
            RagChunk chunk = new RagChunk();
            chunk.setDocumentId(doc.getId());
            chunk.setChunkCode(doc.getDocCode() + "-C" + String.format("%03d", i + 1));
            chunk.setChunkText(text);
            chunk.setChunkIndex(i + 1);
            chunk.setTokenCount(text.length());
            chunk.setTags(source.tags());
            chunk.setSourceType(source.sourceType());
            chunk.setSourceId(source.sourceId());
            chunk.setEmbeddingModel(embeddingService.modelName());
            chunk.setStatus(1);
            chunk.setCreateTime(LocalDateTime.now());
            chunk.setUpdateTime(LocalDateTime.now());
            ragChunkMapper.insert(chunk);

            List<Double> vector = embeddingService.embed(text);
            if (props.isVectorEnabled() && vectorStoreService.available() && !vector.isEmpty()) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("chunkId", chunk.getId());
                payload.put("documentId", doc.getId());
                payload.put("sourceType", source.sourceType());
                payload.put("sourceId", source.sourceId());
                payload.put("docType", source.docType());
                payload.put("title", doc.getTitle());
                payload.put("tags", source.tags());
                vectorStoreService.upsert(chunk.getId(), vector, payload);
                chunk.setVectorId(String.valueOf(chunk.getId()));
                ragChunkMapper.updateById(chunk);
            }
            chunkCount++;
        }
        return result(source.sourceType(), 1, chunkCount);
    }

    private void deleteExisting(String sourceType, Long sourceId) {
        List<RagDocument> docs = ragDocumentMapper.selectList(new LambdaQueryWrapper<RagDocument>()
                .eq(RagDocument::getSourceType, sourceType)
                .eq(RagDocument::getSourceId, sourceId));
        for (RagDocument doc : docs) {
            try {
                vectorStoreService.deleteByDocumentId(doc.getId());
            } catch (Exception e) {
                log.warn("Delete Qdrant vectors failed, documentId={}: {}", doc.getId(), e.getMessage());
            }
            ragChunkMapper.delete(new LambdaQueryWrapper<RagChunk>().eq(RagChunk::getDocumentId, doc.getId()));
            ragDocumentMapper.deleteById(doc.getId());
        }
    }

    private List<String> split(String text) {
        String safe = text == null ? "" : text.trim();
        int max = props.getChunkMaxChars() == null ? 700 : Math.max(200, props.getChunkMaxChars());
        int overlap = props.getChunkOverlapChars() == null ? 80 : Math.max(0, Math.min(props.getChunkOverlapChars(), max / 2));
        List<String> out = new ArrayList<>();
        int start = 0;
        while (start < safe.length()) {
            int end = Math.min(safe.length(), start + max);
            out.add(safe.substring(start, end).trim());
            if (end >= safe.length()) {
                break;
            }
            start = Math.max(0, end - overlap);
        }
        return out;
    }

    private Map<String, Object> result(String source, int documents, int chunks) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("source", source);
        out.put("documents", documents);
        out.put("chunks", chunks);
        return out;
    }

    private int intValue(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private String normalizeType(String type) {
        return StringUtils.hasText(type) ? type.trim().toLowerCase() : "doc";
    }

    private String firstText(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }

    private String join(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                parts.add(value.trim());
            }
        }
        return String.join(",", parts);
    }

    private String nz(String value) {
        return StringUtils.hasText(value) ? value : "—";
    }

    private record SourceDocument(String sourceType, Long sourceId, String code, String title,
                                  String docType, String tags, String text) {
    }
}
