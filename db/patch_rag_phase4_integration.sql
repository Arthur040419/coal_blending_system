-- RAG 第四阶段：主流程接入后的检索/生成追溯日志表。
-- 说明：/blendPlan/generate 会把订单关键词、命中知识、最终 Prompt 与模型输出写入该表。
CREATE TABLE IF NOT EXISTS `rag_retrieval_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_type` varchar(50) NOT NULL COMMENT '业务类型：blend_generate/chat/search',
  `biz_id` bigint DEFAULT NULL COMMENT '业务ID，如方案ID',
  `query_text` varchar(1000) NOT NULL COMMENT '检索查询文本',
  `keywords` varchar(1000) DEFAULT NULL COMMENT '抽取出的关键词',
  `retrieved_ids` varchar(1000) DEFAULT NULL COMMENT '命中的知识ID列表',
  `model_name` varchar(100) DEFAULT NULL COMMENT '调用模型名称',
  `prompt_text` mediumtext COMMENT '最终提示词',
  `model_output` mediumtext COMMENT '模型输出',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_rag_log_biz` (`biz_type`, `biz_id`),
  KEY `idx_rag_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG检索与生成日志表';

-- RAG JSON 输出字段：对齐 docs/rag_implementation_plan.md 8.2。
ALTER TABLE `blend_plan`
  ADD COLUMN `case_reference` text DEFAULT NULL COMMENT 'RAG生成的案例参考' AFTER `rule_basis`,
  ADD COLUMN `recommend_reason` text DEFAULT NULL COMMENT 'RAG生成的推荐理由' AFTER `case_reference`,
  ADD COLUMN `final_explanation` text DEFAULT NULL COMMENT 'RAG生成的最终解释' AFTER `recommend_reason`;
