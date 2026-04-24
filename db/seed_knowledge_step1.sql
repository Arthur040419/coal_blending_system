-- 第1步知识库落地：补充规则与案例（可按需重复执行；依赖 rule_code / case_code 唯一约束）
-- MySQL 8+

INSERT INTO `rule_knowledge` (`rule_code`, `rule_name`, `rule_type`, `rule_content`, `applicable_scope`, `priority_level`, `status`, `source_desc`)
VALUES
  ('R006', '低成本优先规则', '经验规则', '当订单优先级为一般或偏低时，在满足质量底线前提下可优先选择单位成本更低的煤种组合。', '非紧急订单', 3, 1, '知识库种子'),
  ('R007', '高库存优先调用规则', '库存约束', '当某煤种可用库存显著高于安全冗余时，配煤方案宜适度提高该煤种消化比例。', '库存偏高', 2, 1, '知识库种子'),
  ('R008', '库存不足预警规则', '库存约束', '当单库可用量低于本单需求量时，应提示调拨、增量采购或调整交期。', '全部订单', 5, 1, '知识库种子'),
  ('R009', '水分控制规则', '质量约束', '当订单水分上限较严时，应控制高水分煤种配比并关注堆放周期。', '低水分订单', 4, 1, '知识库种子'),
  ('R010', '灰分控制规则', '质量约束', '当订单灰分上限介于 18%～20% 时，仍应避免灰分过高的煤种作为主配。', '中等灰分订单', 3, 1, '知识库种子'),
  ('R011', '硫分一般控制规则', '质量约束', '当订单硫分上限在 0.8%～1.5% 时，应平衡成本与硫分达标风险。', '常规订单', 2, 1, '知识库种子'),
  ('R012', '大批量执行规则', '经验规则', '当单次需求量较大时，应校验多仓合计可用量与物流执行能力。', '大批量', 3, 1, '知识库种子')
ON DUPLICATE KEY UPDATE
  `rule_name` = VALUES(`rule_name`),
  `rule_type` = VALUES(`rule_type`),
  `rule_content` = VALUES(`rule_content`),
  `applicable_scope` = VALUES(`applicable_scope`),
  `priority_level` = VALUES(`priority_level`),
  `status` = VALUES(`status`),
  `source_desc` = VALUES(`source_desc`);

INSERT INTO `case_sample` (`case_code`, `case_name`, `order_desc`, `blend_desc`, `result_desc`, `quality_result`, `cost_result`, `effectiveness_eval`, `status`)
VALUES
  ('C004', '库存紧张下调配比案例', '需求 4000 吨，热值不低于 4600，库存整体偏紧。', '通过下调高库存消耗慢的煤种、提高周转快煤种占比完成交付。', '执行中曾出现单仓不足，经调拨后完成。', '指标达标', 1580000.00, '良好', 1),
  ('C005', '大批量 6000 吨动力煤案例', '大批量订单 6000 吨，硫分不高于 1.2%。', '分批发运+多仓组合发运，主配长焰煤与贫煤。', '成本可控，质量稳定。', '硫分 0.95%，热值 4750', 2890000.00, '良好', 1),
  ('C006', '高优先级保供案例', '高优先级订单 3500 吨，库存保护规则触发。', '优先调用库存充足煤种，少量引入高价高热值煤补偿。', '按期交付。', '热值 4920', 1750000.00, '优秀', 1),
  ('C007', '低成本路径案例', '优先级一般，成本敏感。', '以不粘煤为主，弱粘煤为辅，控制到厂成本。', '硫分略高但在合同范围内。', '成本较低', 980000.00, '中等', 1),
  ('C008', '水分敏感订单案例', '水分上限 8%，夏季运输。', '降低高水分煤比例，加强到场检验。', '未出现拒收。', '全水达标', 720000.00, '良好', 1),
  ('C009', '高热值补偿案例', '热值不低于 5200，基础煤热值不足。', '引入高热值煤种补偿，同步评估成本上升。', '热值达标。', '热值 5280', 1410000.00, '优秀', 1),
  ('C010', '低硫动力煤组合案例', '低硫动力煤场景，硫分不高于 0.8%。', '贫煤+弱粘煤组合，严控高硫煤进入配方。', '硫分 0.55%。', '低硫达标', 1980000.00, '优秀', 1)
ON DUPLICATE KEY UPDATE
  `case_name` = VALUES(`case_name`),
  `order_desc` = VALUES(`order_desc`),
  `blend_desc` = VALUES(`blend_desc`),
  `result_desc` = VALUES(`result_desc`),
  `quality_result` = VALUES(`quality_result`),
  `cost_result` = VALUES(`cost_result`),
  `effectiveness_eval` = VALUES(`effectiveness_eval`),
  `status` = VALUES(`status`);
