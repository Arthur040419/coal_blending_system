package com.coalblend.service.rag;

import com.coalblend.service.rag.model.VectorSearchHit;

import java.util.List;
import java.util.Map;

public interface RagVectorStoreService {

    boolean available();

    void ensureCollection(int vectorSize);

    void upsert(Long pointId, List<Double> vector, Map<String, Object> payload);

    List<VectorSearchHit> search(List<Double> queryVector, int topK);

    void deleteByDocumentId(Long documentId);
}
