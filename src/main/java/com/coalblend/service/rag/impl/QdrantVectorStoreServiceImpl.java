package com.coalblend.service.rag.impl;

import com.coalblend.common.config.CoalRagProperties;
import com.coalblend.service.rag.RagVectorStoreService;
import com.coalblend.service.rag.model.VectorSearchHit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class QdrantVectorStoreServiceImpl implements RagVectorStoreService {

    private final CoalRagProperties props;
    private final RestTemplate restTemplate;

    public QdrantVectorStoreServiceImpl(CoalRagProperties props,
                                        @Qualifier("ragRestTemplate") RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean available() {
        try {
            restTemplate.exchange(url("/collections"), HttpMethod.GET, new HttpEntity<>(headers()), Map.class);
            return true;
        } catch (Exception e) {
            log.warn("Qdrant unavailable: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void ensureCollection(int vectorSize) {
        try {
            ResponseEntity<Map> exists = restTemplate.exchange(
                    url("/collections/" + props.getCollectionName()),
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    Map.class);
            if (exists.getStatusCode().is2xxSuccessful()) {
                return;
            }
        } catch (Exception ignored) {
            // Create below.
        }
        Map<String, Object> body = Map.of(
                "vectors", Map.of(
                        "size", vectorSize,
                        "distance", "Cosine"
                )
        );
        restTemplate.exchange(
                url("/collections/" + props.getCollectionName()),
                HttpMethod.PUT,
                new HttpEntity<>(body, headers()),
                Map.class);
    }

    @Override
    public void upsert(Long pointId, List<Double> vector, Map<String, Object> payload) {
        if (pointId == null || vector == null || vector.isEmpty()) {
            return;
        }
        ensureCollection(vector.size());
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("id", pointId);
        point.put("vector", vector);
        point.put("payload", payload == null ? Map.of() : payload);
        Map<String, Object> body = Map.of("points", List.of(point));
        restTemplate.exchange(
                url("/collections/" + props.getCollectionName() + "/points?wait=true"),
                HttpMethod.PUT,
                new HttpEntity<>(body, headers()),
                Map.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VectorSearchHit> search(List<Double> queryVector, int topK) {
        if (queryVector == null || queryVector.isEmpty()) {
            return List.of();
        }
        ensureCollection(queryVector.size());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vector", queryVector);
        body.put("limit", Math.max(1, topK));
        body.put("with_payload", true);
        Map<String, Object> resp = restTemplate.postForObject(
                url("/collections/" + props.getCollectionName() + "/points/search"),
                new HttpEntity<>(body, headers()),
                Map.class);
        Object result = resp == null ? null : resp.get("result");
        if (!(result instanceof List<?> rows)) {
            return List.of();
        }
        List<VectorSearchHit> hits = new ArrayList<>();
        for (Object rowObj : rows) {
            if (!(rowObj instanceof Map<?, ?> row)) {
                continue;
            }
            Map<String, Object> payload = row.get("payload") instanceof Map<?, ?> p
                    ? (Map<String, Object>) p
                    : Map.of();
            Long chunkId = toLong(payload.get("chunkId"));
            Long documentId = toLong(payload.get("documentId"));
            hits.add(new VectorSearchHit(chunkId, documentId, toDouble(row.get("score")), payload));
        }
        return hits;
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (documentId == null) {
            return;
        }
        Map<String, Object> body = Map.of(
                "filter", Map.of(
                        "must", List.of(Map.of(
                                "key", "documentId",
                                "match", Map.of("value", documentId)
                        ))
                )
        );
        restTemplate.postForObject(
                url("/collections/" + props.getCollectionName() + "/points/delete?wait=true"),
                new HttpEntity<>(body, headers()),
                Map.class);
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(props.getQdrantApiKey())) {
            headers.set("api-key", props.getQdrantApiKey());
        }
        return headers;
    }

    private String url(String path) {
        String base = props.getQdrantUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s && StringUtils.hasText(s)) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double toDouble(Object value) {
        return value instanceof Number n ? n.doubleValue() : null;
    }
}
