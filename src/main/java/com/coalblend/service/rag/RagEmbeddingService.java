package com.coalblend.service.rag;

import java.util.List;

public interface RagEmbeddingService {

    List<Double> embed(String text);

    String modelName();
}
