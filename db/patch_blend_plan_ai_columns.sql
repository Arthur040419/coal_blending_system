-- 大模型解释生成：扩展 blend_plan（在已有库上执行一次即可）
ALTER TABLE `blend_plan`
  ADD COLUMN `optimize_suggestion` text NULL COMMENT 'AI优化建议' AFTER `risk_tip`,
  ADD COLUMN `ai_model_name` varchar(100) NULL COMMENT '解释所用模型名称' AFTER `optimize_suggestion`,
  ADD COLUMN `ai_generate_flag` tinyint NOT NULL DEFAULT 0 COMMENT '是否由大模型生成解释：1是，0否' AFTER `ai_model_name`;
