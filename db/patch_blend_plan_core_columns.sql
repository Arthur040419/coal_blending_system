-- 第1阶段：配煤方案核心增强字段（可行性、约束摘要、评分明细、风险等级）
ALTER TABLE `blend_plan`
  ADD COLUMN `feasible_flag` tinyint NOT NULL DEFAULT 1 COMMENT '是否满足硬约束：1是，0否' AFTER `overall_score`,
  ADD COLUMN `constraint_summary` text NULL COMMENT '约束校验摘要：预测指标、违反项、风险提示' AFTER `feasible_flag`,
  ADD COLUMN `score_detail` text NULL COMMENT '评分明细：质量、成本、库存稳定性和综合评分理由' AFTER `constraint_summary`,
  ADD COLUMN `risk_level` varchar(20) NULL COMMENT '风险等级：low/medium/high' AFTER `score_detail`;
