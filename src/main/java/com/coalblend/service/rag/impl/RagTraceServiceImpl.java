package com.coalblend.service.rag.impl;

import com.coalblend.entity.RagRetrievalLog;
import com.coalblend.mapper.RagRetrievalLogMapper;
import com.coalblend.service.rag.RagTraceService;
import com.coalblend.vo.AiExplainResultVO;
import com.coalblend.vo.rag.RagRetrieveResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagTraceServiceImpl implements RagTraceService {

    private final RagRetrievalLogMapper ragRetrievalLogMapper;

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
            ragRetrievalLogMapper.insert(logRow);
        } catch (Exception e) {
            log.warn("Save RAG retrieval log failed, planId={}: {}", planId, e.getMessage());
        }
    }
}
