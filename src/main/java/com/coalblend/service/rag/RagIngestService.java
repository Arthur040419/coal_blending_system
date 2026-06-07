package com.coalblend.service.rag;

import java.util.Map;

public interface RagIngestService {

    Map<String, Object> ingestAll();

    Map<String, Object> ingestRagKnowledge(Long id);

    Map<String, Object> ingestRule(Long id);

    Map<String, Object> ingestCase(Long id);
}
