-- 大模型参与候选方案生成：记录候选来源和 AI 候选生成理由。
ALTER TABLE `blend_plan`
  ADD COLUMN `candidate_source` varchar(30) DEFAULT 'system' COMMENT '候选来源：system/ai/hybrid' AFTER `trace_status`,
  ADD COLUMN `ai_candidate_reason` text COMMENT 'AI候选生成理由' AFTER `candidate_source`;

