package com.coalblend.vo.rag;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RagRetrieveResultVO {

    private Long orderId;
    private String queryText;
    /** keyword/vector/hybrid */
    private String retrievalMode;
    private String embeddingModel;
    private List<String> keywords = new ArrayList<>();
    private List<Long> matchedKnowledgeIds = new ArrayList<>();
    private List<Long> matchedChunkIds = new ArrayList<>();
    private List<RagKnowledgeHitVO> rules = new ArrayList<>();
    private List<RagKnowledgeHitVO> cases = new ArrayList<>();
    private List<RagKnowledgeHitVO> terms = new ArrayList<>();
    private List<RagKnowledgeHitVO> docs = new ArrayList<>();
    private List<RagKnowledgeHitVO> all = new ArrayList<>();
}
