package com.coalblend.service.rag.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchHit {

    private Long chunkId;
    private Long documentId;
    private Double score;
    private Map<String, Object> payload;
}
