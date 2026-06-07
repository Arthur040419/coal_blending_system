package com.coalblend.service.rag.impl;

import com.coalblend.entity.RagRetrievalLog;
import com.coalblend.mapper.RagRetrievalLogMapper;
import com.coalblend.service.rag.RagTraceService;
import com.coalblend.vo.AiExplainResultVO;
import com.coalblend.vo.rag.RagRetrieveResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagTraceServiceImpl implements RagTraceService {

    private final RagRetrievalLogMapper ragRetrievalLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void saveBlendGenerateTrace(Long planId, RagRetrieveResultVO retrieveResult, AiExplainResultVO explainResult) {
        try {
            RagRetrievalLog logRow = new RagRetrievalLog();
            logRow.setBizType("blend_generate");
            logRow.setBizId(planId);
            logRow.setQueryText(retrieveResult == null ? "" : retrieveResult.getQueryText());
            logRow.setKeywords(retrieveResult == null ? "" : String.join(",", retrieveResult.getKeywords()));
            logRow.setRetrievedIds(retrieveResult == null ? "" : retrieveResult.getMatchedKnowledgeIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            logRow.setModelName(explainResult == null ? "" : explainResult.getModelNameUsed());
            logRow.setPromptText(explainResult == null ? "" : explainResult.getPromptText());
            logRow.setModelOutput(explainResult == null ? "" : explainResult.getRawText());
            logRow.setRetrievalMode(retrieveResult == null ? "" : retrieveResult.getRetrievalMode());
            logRow.setQueryEmbeddingModel(retrieveResult == null ? "" : retrieveResult.getEmbeddingModel());
            logRow.setRetrievedChunksJson(toJson(retrieveResult == null ? null : retrieveResult.getAll()));
            logRow.setRerankResultJson(toJson(retrieveResult));
            logRow.setUsedChunkIds(retrieveResult == null || retrieveResult.getMatchedChunkIds() == null ? ""
                    : retrieveResult.getMatchedChunkIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            ragRetrievalLogMapper.insert(logRow);
        } catch (Exception e) {
            log.warn("Save RAG retrieval log failed, planId={}: {}", planId, e.getMessage());
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }
}
