package com.coalblend.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "coal.rag")
public class CoalRagProperties {

    /** 是否启用向量 RAG。关闭时保留原关键词检索。 */
    private boolean vectorEnabled = true;

    /** Qdrant HTTP 地址，例如 http://qdrant:6333。 */
    private String qdrantUrl = "http://127.0.0.1:6333";
    private String qdrantApiKey;
    private String collectionName = "coal_rag_chunks";
    private Integer vectorSize = 1024;
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(60);

    /** Ollama embedding API，推荐 /api/embed；兼容回退 /api/embeddings。 */
    private String embeddingApiUrl = "http://127.0.0.1:11434/api/embed";
    private String embeddingModel = "bge-m3";
    private boolean fallbackHashEmbedding = true;

    /** 文档切片参数。 */
    private Integer chunkMaxChars = 700;
    private Integer chunkOverlapChars = 80;

    /** 混合召回权重。 */
    private Double vectorWeight = 0.60;
    private Double keywordWeight = 0.30;
    private Double businessWeight = 0.10;
}
