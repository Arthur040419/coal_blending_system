package com.coalblend.service.rag.impl;

import com.coalblend.common.config.CoalRagProperties;
import com.coalblend.service.rag.RagEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RagEmbeddingServiceImpl implements RagEmbeddingService {

    private final CoalRagProperties props;
    private final RestTemplate restTemplate;

    public RagEmbeddingServiceImpl(CoalRagProperties props,
                                   @Qualifier("ragRestTemplate") RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    @Override
    public List<Double> embed(String text) {
        try {
            List<Double> vector = callOllamaEmbed(text);
            if (vector != null && !vector.isEmpty()) {
                return normalizeSize(vector);
            }
        } catch (Exception e) {
            log.warn("RAG embedding call failed, fallbackHashEmbedding={}: {}", props.isFallbackHashEmbedding(), e.getMessage());
        }
        if (!props.isFallbackHashEmbedding()) {
            return List.of();
        }
        return hashEmbedding(text == null ? "" : text);
    }

    @Override
    public String modelName() {
        return props.getEmbeddingModel();
    }

    @SuppressWarnings("unchecked")
    private List<Double> callOllamaEmbed(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "model", props.getEmbeddingModel(),
                "input", text == null ? "" : text
        );
        Map<String, Object> resp = restTemplate.postForObject(
                props.getEmbeddingApiUrl(),
                new HttpEntity<>(body, headers),
                Map.class);
        if (resp == null) {
            return List.of();
        }
        Object embeddings = resp.get("embeddings");
        if (embeddings instanceof List<?> rows && !rows.isEmpty() && rows.get(0) instanceof List<?> first) {
            return toDoubleList(first);
        }
        Object embedding = resp.get("embedding");
        if (embedding instanceof List<?> values) {
            return toDoubleList(values);
        }
        return List.of();
    }

    private List<Double> toDoubleList(List<?> values) {
        List<Double> out = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof Number n) {
                out.add(n.doubleValue());
            }
        }
        return out;
    }

    private List<Double> normalizeSize(List<Double> vector) {
        int size = props.getVectorSize() == null ? vector.size() : props.getVectorSize();
        if (vector.size() == size) {
            return vector;
        }
        List<Double> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            out.add(i < vector.size() ? vector.get(i) : 0.0);
        }
        return out;
    }

    private List<Double> hashEmbedding(String text) {
        int size = props.getVectorSize() == null ? 1024 : props.getVectorSize();
        double[] values = new double[size];
        String safe = text == null ? "" : text;
        for (String token : safe.split("\\s+|(?=[\\u4e00-\\u9fa5])|(?<=[\\u4e00-\\u9fa5])")) {
            if (token == null || token.isBlank()) {
                continue;
            }
            byte[] hash = sha256(token);
            int idx = ((hash[0] & 0xff) << 8 | (hash[1] & 0xff)) % size;
            values[idx] += ((hash[2] & 1) == 0) ? 1.0 : -1.0;
        }
        double norm = 0.0;
        for (double v : values) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        List<Double> out = new ArrayList<>(size);
        for (double v : values) {
            out.add(norm == 0.0 ? 0.0 : v / norm);
        }
        return out;
    }

    private byte[] sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return text.getBytes(StandardCharsets.UTF_8);
        }
    }
}
