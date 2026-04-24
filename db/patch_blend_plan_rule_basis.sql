-- 第2步：大模型「规则依据」单独落库，便于前端与论文展示
ALTER TABLE `blend_plan`
  ADD COLUMN `rule_basis` text DEFAULT NULL COMMENT 'AI生成的规则依据（知识增强）' AFTER `explanation`;
