package com.coalblend.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `rag_document` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `doc_code` varchar(64) DEFAULT NULL COMMENT 'RAG文档编号',
                  `title` varchar(255) NOT NULL COMMENT '文档标题',
                  `doc_type` varchar(32) DEFAULT NULL COMMENT 'rule/case/term/doc',
                  `source_type` varchar(64) DEFAULT NULL COMMENT '来源类型',
                  `source_id` bigint DEFAULT NULL COMMENT '来源数据ID',
                  `tags` varchar(500) DEFAULT NULL COMMENT '标签',
                  `status` tinyint DEFAULT 1 COMMENT '状态',
                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  KEY `idx_rag_doc_source` (`source_type`, `source_id`),
                  KEY `idx_rag_doc_type` (`doc_type`, `status`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG文档元数据表'
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `rag_chunk` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `document_id` bigint NOT NULL COMMENT 'RAG文档ID',
                  `chunk_code` varchar(64) DEFAULT NULL COMMENT '切片编号',
                  `chunk_text` text NOT NULL COMMENT '切片文本',
                  `chunk_index` int DEFAULT NULL COMMENT '切片序号',
                  `token_count` int DEFAULT NULL COMMENT '粗略token/字符数',
                  `tags` varchar(500) DEFAULT NULL COMMENT '标签',
                  `source_type` varchar(64) DEFAULT NULL COMMENT '来源类型',
                  `source_id` bigint DEFAULT NULL COMMENT '来源数据ID',
                  `vector_id` varchar(64) DEFAULT NULL COMMENT '向量库点ID',
                  `embedding_model` varchar(100) DEFAULT NULL COMMENT '向量模型',
                  `status` tinyint DEFAULT 1 COMMENT '状态',
                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  KEY `idx_rag_chunk_document` (`document_id`),
                  KEY `idx_rag_chunk_source` (`source_type`, `source_id`),
                  KEY `idx_rag_chunk_vector` (`vector_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG知识切片表'
                """);

        Map<String, String> logColumns = new LinkedHashMap<>();
        logColumns.put("retrieval_mode", "VARCHAR(32) NULL COMMENT 'keyword/vector/hybrid'");
        logColumns.put("query_embedding_model", "VARCHAR(100) NULL COMMENT '查询向量模型'");
        logColumns.put("retrieved_chunks_json", "JSON NULL COMMENT '召回chunk明细'");
        logColumns.put("rerank_result_json", "JSON NULL COMMENT '重排结果'");
        logColumns.put("used_chunk_ids", "VARCHAR(1000) NULL COMMENT '注入Prompt的chunk ID'");
        for (Map.Entry<String, String> entry : logColumns.entrySet()) {
            ensureColumn("rag_retrieval_log", entry.getKey(), entry.getValue());
        }
    }

    private void ensureColumn(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        log.info("Added missing {}.{} column for vector RAG", tableName, columnName);
    }
}
