ALTER TABLE blend_plan
  ADD COLUMN decision_status VARCHAR(32) NULL COMMENT '决策状态：FEASIBLE/RISKY/INFEASIBLE',
  ADD COLUMN recommendation_mode VARCHAR(32) NULL COMMENT '推荐模式：NORMAL/RISK_REFERENCE/NO_SOLUTION',
  ADD COLUMN score_strategy VARCHAR(32) NULL COMMENT '评分策略',
  ADD COLUMN pareto_rank INT NULL COMMENT 'Pareto非支配排序等级',
  ADD COLUMN objective_cost_per_ton DECIMAL(18,4) NULL COMMENT '目标1：吨煤成本',
  ADD COLUMN objective_quality_deviation DECIMAL(18,4) NULL COMMENT '目标2：质量偏差',
  ADD COLUMN objective_execution_risk DECIMAL(18,4) NULL COMMENT '目标3：执行风险',
  ADD COLUMN problem_items_json JSON NULL COMMENT '结构化问题项',
  ADD COLUMN suggestion_items_json JSON NULL COMMENT '结构化建议项',
  ADD COLUMN generation_config_json JSON NULL COMMENT '生成参数快照';
