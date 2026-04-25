# ************************************************************
# Sequel Ace SQL dump
# 版本号： 20094
#
# https://sequel-ace.com/
# https://github.com/Sequel-Ace/Sequel-Ace
#
# 主机: 127.0.0.1 (MySQL 9.3.0)
# 数据库: coal_blending_system
# 生成时间: 2026-04-25 09:21:35 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# 转储表 batch_lineage
# ------------------------------------------------------------

DROP TABLE IF EXISTS `batch_lineage`;

CREATE TABLE `batch_lineage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_batch_no` varchar(64) NOT NULL COMMENT '父批次号',
  `parent_batch_type` varchar(50) NOT NULL COMMENT '父批次类型',
  `child_batch_no` varchar(64) NOT NULL COMMENT '子批次号',
  `child_batch_type` varchar(50) NOT NULL COMMENT '子批次类型',
  `process_stage` varchar(50) NOT NULL COMMENT '处理阶段',
  `quantity` decimal(12,2) DEFAULT NULL COMMENT '参与数量，吨',
  `ratio` decimal(8,4) DEFAULT NULL COMMENT '参与比例',
  `operation_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent_batch_no` (`parent_batch_no`),
  KEY `idx_child_batch_no` (`child_batch_no`),
  KEY `idx_process_stage` (`process_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='批次血缘追溯表';

LOCK TABLES `batch_lineage` WRITE;
/*!40000 ALTER TABLE `batch_lineage` DISABLE KEYS */;

INSERT INTO `batch_lineage` (`id`, `parent_batch_no`, `parent_batch_type`, `child_batch_no`, `child_batch_type`, `process_stage`, `quantity`, `ratio`, `operation_time`, `operator_name`, `remark`, `create_time`)
VALUES
	(1,'RC2026042501','raw_coal','WP2026042501','wash_batch','raw_to_wash',3000.00,0.5769,'2026-04-25 13:00:00','洗选班组A','金鸡滩长焰煤进入低硫高热值洗选批次','2026-04-25 16:14:38'),
	(2,'RC2026042502','raw_coal','WP2026042501','wash_batch','raw_to_wash',2200.00,0.4231,'2026-04-25 13:00:00','洗选班组A','营盘壕高热值煤进入低硫高热值洗选批次','2026-04-25 16:14:38'),
	(3,'RC2026042503','raw_coal','WP2026042502','wash_batch','raw_to_wash',3600.00,0.8571,'2026-04-25 17:00:00','洗选班组B','平朔高灰原煤进入洗选降灰批次','2026-04-25 16:14:38'),
	(4,'RC2026042504','raw_coal','WP2026042502','wash_batch','raw_to_wash',600.00,0.1429,'2026-04-25 17:00:00','洗选班组B','金鸡滩不粘煤用于调节入洗质量','2026-04-25 16:14:38'),
	(5,'RC2026042505','raw_coal','WP2026042601','wash_batch','raw_to_wash',1700.00,0.6800,'2026-04-26 08:00:00','洗选班组C','五彩湾低成本动力煤主投入','2026-04-25 16:14:38'),
	(6,'RC2026042503','raw_coal','WP2026042601','wash_batch','raw_to_wash',800.00,0.3200,'2026-04-26 08:00:00','洗选班组C','平朔动力煤成本补充投入','2026-04-25 16:14:38'),
	(7,'WP2026042501','wash_batch','PB2026042501-CL','product_batch','wash_to_product',3744.00,0.7200,'2026-04-25 16:20:00','洗选班组A','低硫高热值精煤产出','2026-04-25 16:14:38'),
	(8,'WP2026042501','wash_batch','PB2026042501-MD','product_batch','wash_to_product',624.00,0.1200,'2026-04-25 16:20:00','洗选班组A','中煤副产品产出','2026-04-25 16:14:38'),
	(9,'WP2026042502','wash_batch','PB2026042502-CL','product_batch','wash_to_product',2436.00,0.5800,'2026-04-25 21:30:00','洗选班组B','高灰原煤洗选后精煤产出','2026-04-25 16:14:38'),
	(10,'WP2026042502','wash_batch','PB2026042502-GG','product_batch','wash_to_product',672.00,0.1600,'2026-04-25 21:30:00','洗选班组B','矸石副产品产出','2026-04-25 16:14:38'),
	(11,'WP2026042601','wash_batch','PB2026042601-CL','product_batch','wash_to_product',2000.00,0.8000,'2026-04-26 10:30:00','洗选班组C','成本优先动力煤产品产出','2026-04-25 16:14:38'),
	(12,'PB2026042501-CL','product_batch','FP2026042501','final_product','product_to_blend',3000.00,0.6000,'2026-04-26 08:30:00','配煤计划员','低硫高热值最终产品主配批次','2026-04-25 16:14:38'),
	(13,'PB2026042502-CL','product_batch','FP2026042501','final_product','product_to_blend',2000.00,0.4000,'2026-04-26 08:30:00','配煤计划员','洗选降灰精煤辅助配批次','2026-04-25 16:14:38'),
	(14,'PB2026042501-CL','product_batch','FP2026042502','final_product','product_to_blend',1200.00,0.3750,'2026-04-26 09:20:00','配煤计划员','风险订单低硫精煤受库存限制','2026-04-25 16:14:38'),
	(15,'PB2026042502-CL','product_batch','FP2026042502','final_product','product_to_blend',2000.00,0.6250,'2026-04-26 09:20:00','配煤计划员','风险订单洗选降灰煤占比较高，硫分余量不足','2026-04-25 16:14:38'),
	(16,'FP2026042501','final_product','SHIP2026042501','shipment','product_to_shipment',5000.00,1.0000,'2026-04-26 11:00:00','发运班组A','最终商品煤装车发运','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `batch_lineage` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 blend_plan
# ------------------------------------------------------------

DROP TABLE IF EXISTS `blend_plan`;

CREATE TABLE `blend_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_code` varchar(50) NOT NULL COMMENT '方案编号',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `plan_name` varchar(100) DEFAULT NULL COMMENT '方案名称',
  `total_cost` decimal(12,2) DEFAULT NULL COMMENT '总成本',
  `quality_score` decimal(8,2) DEFAULT NULL COMMENT '质量评分',
  `cost_score` decimal(8,2) DEFAULT NULL COMMENT '成本评分',
  `stability_score` decimal(8,2) DEFAULT NULL COMMENT '稳定性评分',
  `overall_score` decimal(8,2) DEFAULT NULL COMMENT '综合评分',
  `feasible_flag` tinyint NOT NULL DEFAULT '1' COMMENT '是否满足硬约束：1是，0否',
  `constraint_summary` text COMMENT '约束校验摘要：预测指标、违反项、风险提示',
  `score_detail` text COMMENT '评分明细：质量、成本、库存稳定性和综合评分理由',
  `risk_level` varchar(20) DEFAULT NULL COMMENT '风险等级：low/medium/high',
  `plan_status` varchar(20) DEFAULT 'generated' COMMENT '方案状态：generated/selected/executed',
  `explanation` text COMMENT '方案解释',
  `rule_basis` text COMMENT 'AI生成的规则依据（知识增强）',
  `case_reference` text COMMENT 'RAG生成的案例参考',
  `recommend_reason` text COMMENT 'RAG生成的推荐理由',
  `final_explanation` text COMMENT 'RAG生成的最终解释',
  `risk_tip` text COMMENT '风险提示',
  `optimize_suggestion` text COMMENT 'AI优化建议',
  `ai_model_name` varchar(100) DEFAULT NULL COMMENT '解释所用模型名称',
  `ai_generate_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否由大模型生成解释：1是，0否',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `final_product_batch_no` varchar(64) DEFAULT NULL COMMENT '最终产品批次号',
  `trace_status` varchar(30) DEFAULT 'not_executed' COMMENT '追溯状态：not_executed/executed/inspected/shipped',
  PRIMARY KEY (`id`),
  UNIQUE KEY `plan_code` (`plan_code`),
  KEY `fk_plan_order` (`order_id`),
  CONSTRAINT `fk_plan_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配煤方案主表';

LOCK TABLES `blend_plan` WRITE;
/*!40000 ALTER TABLE `blend_plan` DISABLE KEYS */;

INSERT INTO `blend_plan` (`id`, `plan_code`, `order_id`, `plan_name`, `total_cost`, `quality_score`, `cost_score`, `stability_score`, `overall_score`, `feasible_flag`, `constraint_summary`, `score_detail`, `risk_level`, `plan_status`, `explanation`, `rule_basis`, `case_reference`, `recommend_reason`, `final_explanation`, `risk_tip`, `optimize_suggestion`, `ai_model_name`, `ai_generate_flag`, `create_by`, `create_time`, `update_time`, `final_product_batch_no`, `trace_status`)
VALUES
	(1,'P2026042501-A',1,'低硫高热值推荐方案-A',2535000.00,92.00,84.00,88.00,89.20,1,'预测灰分10.64%，硫分0.652%，水分6.84%，发热量6048kcal/kg，满足订单硬约束。','质量分较高；成本因使用高热值低硫产品略高；库存稳定性良好。','low','executed','优先使用低硫高热值精煤PB2026042501-CL，并搭配洗选降灰精煤PB2026042502-CL。','命中低硫煤优先、灰分约束、发热量下限、库存可用性规则。','参考历史低硫动力煤配煤案例。','在质量达标前提下兼顾库存消耗和成本控制。','方案已执行并生成最终产品批次FP2026042501，最终质检合格。','风险较低，需关注PB2026042502-CL库存余量。','后续同类订单可优先保留PB2026042501-CL作为低硫主配资源。','RAG关键词检索配置',1,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','FP2026042501','shipped'),
	(2,'P2026042502-RISK',2,'严格低硫订单风险方案',1712000.00,76.00,82.00,73.00,77.10,1,'预测灰分13.08%，硫分0.598%，发热量5750kcal/kg，理论满足订单，但硫分接近0.60%上限。','质量余量不足，成本中等，库存压力较高。','medium','executed','该方案满足硬约束但硫分安全余量过小，执行时需加强最终质检。','命中高硫煤限配、低硫煤优先和风险提示规则。','参考低硫订单案例，但本订单硫分约束更严格。','受库存限制，方案保留较少质量余量。','方案执行后最终质检硫分0.610%，超过订单硫分上限。','硫分接近上限，不建议作为长期稳定方案。','建议增加低硫高热值精煤比例，减少PB2026042502-CL占比。','RAG关键词检索配置',1,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','FP2026042502','inspected'),
	(3,'P2026042503-COST',3,'成本优先候选方案',2012500.00,81.00,91.00,83.00,84.40,1,'预测灰分15.18%，硫分0.568%，水分8.93%，发热量5375kcal/kg，满足成本优先订单要求。','成本优势明显，质量满足但水分余量一般。','medium','generated','为满足成本优先目标，使用成本优先动力煤产品PB2026042601-CL，并搭配低硫高热值精煤。','命中低成本煤使用边界、库存可用性和灰分约束规则。','参考成本优先动力煤案例。','该方案适合质量要求相对宽松、成本敏感的订单。','方案尚未发运，可作为候选方案展示。','水分接近订单上限，执行前建议复检产品批次。','若客户质量要求提高，应增加PB2026042501-CL比例。','RAG关键词检索配置',1,1,'2026-04-25 16:14:38','2026-04-25 16:14:38',NULL,'not_executed');

/*!40000 ALTER TABLE `blend_plan` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 blend_plan_detail
# ------------------------------------------------------------

DROP TABLE IF EXISTS `blend_plan_detail`;

CREATE TABLE `blend_plan_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_id` bigint NOT NULL COMMENT '方案ID',
  `coal_id` bigint NOT NULL COMMENT '煤种ID',
  `blend_ratio` decimal(6,4) NOT NULL COMMENT '配比',
  `use_quantity` decimal(12,2) DEFAULT NULL COMMENT '使用数量（吨）',
  `predicted_ash` decimal(6,2) DEFAULT NULL COMMENT '预测灰分',
  `predicted_sulfur` decimal(6,2) DEFAULT NULL COMMENT '预测硫分',
  `predicted_moisture` decimal(6,2) DEFAULT NULL COMMENT '预测水分',
  `predicted_volatile` decimal(6,2) DEFAULT NULL COMMENT '预测挥发分',
  `predicted_calorific` decimal(10,2) DEFAULT NULL COMMENT '预测发热量',
  `unit_cost` decimal(10,2) DEFAULT NULL COMMENT '单煤成本',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `product_batch_id` bigint DEFAULT NULL COMMENT '产品批次ID',
  `product_batch_no` varchar(64) DEFAULT NULL COMMENT '产品批次号',
  `inventory_id` bigint DEFAULT NULL COMMENT '库存ID',
  `quality_snapshot_json` json DEFAULT NULL COMMENT '生成方案时的煤质快照',
  PRIMARY KEY (`id`),
  KEY `fk_detail_plan` (`plan_id`),
  KEY `fk_detail_coal` (`coal_id`),
  CONSTRAINT `fk_detail_coal` FOREIGN KEY (`coal_id`) REFERENCES `coal_type` (`id`),
  CONSTRAINT `fk_detail_plan` FOREIGN KEY (`plan_id`) REFERENCES `blend_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配煤方案明细表';

LOCK TABLES `blend_plan_detail` WRITE;
/*!40000 ALTER TABLE `blend_plan_detail` DISABLE KEYS */;

INSERT INTO `blend_plan_detail` (`id`, `plan_id`, `coal_id`, `blend_ratio`, `use_quantity`, `predicted_ash`, `predicted_sulfur`, `predicted_moisture`, `predicted_volatile`, `predicted_calorific`, `unit_cost`, `remark`, `product_batch_id`, `product_batch_no`, `inventory_id`, `quality_snapshot_json`)
VALUES
	(1,1,1,0.6000,3000.00,7.20,0.68,6.20,31.60,6600.00,505.00,'低硫高热值主配产品',1,'PB2026042501-CL',6,'{\"ash\": 7.20, \"stage\": \"clean_coal\", \"sulfur\": 0.68, \"moisture\": 6.20, \"calorific\": 6600}'),
	(2,1,4,0.4000,2000.00,15.80,0.61,7.80,34.10,5220.00,510.00,'洗选降灰精煤辅助配煤',3,'PB2026042502-CL',8,'{\"ash\": 15.80, \"stage\": \"clean_coal\", \"sulfur\": 0.61, \"moisture\": 7.80, \"calorific\": 5220}'),
	(3,2,1,0.3750,1200.00,7.20,0.68,6.20,31.60,6600.00,505.00,'低硫高热值精煤，用量受库存限制',1,'PB2026042501-CL',6,'{\"ash\": 7.20, \"stage\": \"clean_coal\", \"sulfur\": 0.68, \"moisture\": 6.20, \"calorific\": 6600}'),
	(4,2,4,0.6250,2000.00,15.80,0.61,7.80,34.10,5220.00,510.00,'洗选降灰煤占比较高，硫分余量不足',3,'PB2026042502-CL',8,'{\"ash\": 15.80, \"stage\": \"clean_coal\", \"sulfur\": 0.61, \"moisture\": 7.80, \"calorific\": 5220}'),
	(5,3,5,0.7000,3150.00,18.60,0.52,10.10,37.20,4850.00,360.00,'成本优先主配产品',5,'PB2026042601-CL',10,'{\"ash\": 18.60, \"stage\": \"clean_coal\", \"sulfur\": 0.52, \"moisture\": 10.10, \"calorific\": 4850}'),
	(6,3,1,0.3000,1350.00,7.20,0.68,6.20,31.60,6600.00,505.00,'低硫高热值质量补偿产品',1,'PB2026042501-CL',6,'{\"ash\": 7.20, \"stage\": \"clean_coal\", \"sulfur\": 0.68, \"moisture\": 6.20, \"calorific\": 6600}');

/*!40000 ALTER TABLE `blend_plan_detail` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 blend_plan_feedback
# ------------------------------------------------------------

DROP TABLE IF EXISTS `blend_plan_feedback`;

CREATE TABLE `blend_plan_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_id` bigint NOT NULL COMMENT '关联方案ID',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `actual_quantity` decimal(12,2) NOT NULL COMMENT '实际执行量（吨）',
  `actual_ash` decimal(6,2) DEFAULT NULL COMMENT '实际灰分',
  `actual_sulfur` decimal(6,2) DEFAULT NULL COMMENT '实际硫分',
  `actual_moisture` decimal(6,2) DEFAULT NULL COMMENT '实际水分',
  `actual_volatile` decimal(6,2) DEFAULT NULL COMMENT '实际挥发分',
  `actual_calorific` decimal(10,2) DEFAULT NULL COMMENT '实际发热量',
  `actual_cost` decimal(12,2) DEFAULT NULL COMMENT '实际成本',
  `qualified_flag` tinyint NOT NULL DEFAULT '1' COMMENT '是否达标：1是，0否',
  `effectiveness_eval` varchar(50) DEFAULT NULL COMMENT '执行评价：优秀/良好/一般/较差',
  `feedback_desc` text COMMENT '反馈说明',
  `execute_date` date DEFAULT NULL COMMENT '执行日期',
  `operator_id` bigint DEFAULT NULL COMMENT '反馈录入人',
  `case_generated_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否已沉淀为案例：1是，0否',
  `case_id` bigint DEFAULT NULL COMMENT '回流生成的案例ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1有效，0无效',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_plan` (`plan_id`),
  KEY `idx_feedback_order` (`order_id`),
  KEY `idx_feedback_case` (`case_id`),
  CONSTRAINT `fk_feedback_case` FOREIGN KEY (`case_id`) REFERENCES `case_sample` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_feedback_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_feedback_plan` FOREIGN KEY (`plan_id`) REFERENCES `blend_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配煤方案执行反馈表';

LOCK TABLES `blend_plan_feedback` WRITE;
/*!40000 ALTER TABLE `blend_plan_feedback` DISABLE KEYS */;

INSERT INTO `blend_plan_feedback` (`id`, `plan_id`, `order_id`, `actual_quantity`, `actual_ash`, `actual_sulfur`, `actual_moisture`, `actual_volatile`, `actual_calorific`, `actual_cost`, `qualified_flag`, `effectiveness_eval`, `feedback_desc`, `execute_date`, `operator_id`, `case_generated_flag`, `case_id`, `status`, `create_time`, `update_time`)
VALUES
	(1,1,1,5000.00,10.68,0.65,6.88,32.40,6048.00,2535000.00,1,'优秀','方案执行稳定，最终产品质量余量充足，适合作为低硫高热值订单参考案例。','2026-04-26',1,1,1,1,'2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(2,2,2,3200.00,13.18,0.61,7.25,33.10,5735.00,1712000.00,0,'一般','方案理论满足但硫分余量不足，最终硫分略超订单上限，建议提高低硫精煤比例。','2026-04-26',1,1,2,1,'2026-04-25 16:14:38','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `blend_plan_feedback` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 case_sample
# ------------------------------------------------------------

DROP TABLE IF EXISTS `case_sample`;

CREATE TABLE `case_sample` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `case_code` varchar(50) NOT NULL COMMENT '案例编号',
  `case_name` varchar(100) NOT NULL COMMENT '案例名称',
  `order_desc` text COMMENT '订单描述',
  `blend_desc` text COMMENT '配煤方案描述',
  `result_desc` text COMMENT '执行结果描述',
  `quality_result` varchar(255) DEFAULT NULL COMMENT '质量结果摘要',
  `cost_result` decimal(12,2) DEFAULT NULL COMMENT '成本结果',
  `effectiveness_eval` varchar(255) DEFAULT NULL COMMENT '效果评价',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1有效，0无效',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `business_stage` varchar(50) DEFAULT 'blending' COMMENT '案例业务阶段',
  `related_batch_no` varchar(64) DEFAULT NULL COMMENT '关联批次号',
  `related_order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `case_code` (`case_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史案例表';

LOCK TABLES `case_sample` WRITE;
/*!40000 ALTER TABLE `case_sample` DISABLE KEYS */;

INSERT INTO `case_sample` (`id`, `case_code`, `case_name`, `order_desc`, `blend_desc`, `result_desc`, `quality_result`, `cost_result`, `effectiveness_eval`, `status`, `create_time`, `update_time`, `business_stage`, `related_batch_no`, `related_order_id`)
VALUES
	(1,'C2026042501','低硫高热值商品煤配煤成功案例','客户要求5000吨动力煤，灰分≤18%，硫分≤0.80%，发热量≥5000kcal/kg。','PB2026042501-CL 60% + PB2026042502-CL 40%，形成FP2026042501。','最终质检合格并完成发运，质量余量充足。','灰分10.68%，硫分0.652%，水分6.88%，发热量6048kcal/kg',2535000.00,'优秀',1,'2026-04-25 16:14:38','2026-04-25 16:14:38','feedback','FP2026042501',1),
	(2,'C2026042502','严格低硫订单风险反馈案例','客户要求3200吨动力煤，灰分≤16%，硫分≤0.60%，发热量≥5200kcal/kg。','PB2026042501-CL 37.5% + PB2026042502-CL 62.5%，理论满足但硫分余量不足。','最终质检硫分0.610%，略高于订单上限，需要调整低硫煤比例。','灰分13.18%，硫分0.610%，水分7.25%，发热量5735kcal/kg',1712000.00,'一般',1,'2026-04-25 16:14:38','2026-04-25 16:14:38','feedback','FP2026042502',2),
	(3,'C2026042503','成本优先动力煤候选案例','客户要求4500吨动力煤，灰分≤22%，硫分≤1.20%，发热量≥4700kcal/kg，成本优先。','PB2026042601-CL 70% + PB2026042501-CL 30%。','预测满足质量要求且成本较低，尚未执行。','预测灰分15.18%，硫分0.568%，水分8.93%，发热量5375kcal/kg',2012500.00,'良好',1,'2026-04-25 16:14:38','2026-04-25 16:14:38','blending',NULL,3);

/*!40000 ALTER TABLE `case_sample` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 coal_quality
# ------------------------------------------------------------

DROP TABLE IF EXISTS `coal_quality`;

CREATE TABLE `coal_quality` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `coal_id` bigint NOT NULL COMMENT '煤种ID',
  `batch_no` varchar(50) DEFAULT NULL COMMENT '批次号',
  `sample_stage` varchar(50) DEFAULT 'coal_type' COMMENT '采样阶段：geological/raw_coal/wash_feed/clean_coal/mixed_product/final_product',
  `related_batch_no` varchar(64) DEFAULT NULL COMMENT '关联批次号',
  `sample_time` datetime DEFAULT NULL COMMENT '采样时间',
  `sample_point` varchar(100) DEFAULT NULL COMMENT '采样点',
  `ash_content` decimal(6,2) DEFAULT NULL COMMENT '灰分Ad',
  `sulfur_content` decimal(6,2) DEFAULT NULL COMMENT '硫分St,d',
  `moisture_content` decimal(6,2) DEFAULT NULL COMMENT '水分Mt',
  `volatile_content` decimal(6,2) DEFAULT NULL COMMENT '挥发分Vdaf',
  `calorific_value` decimal(10,2) DEFAULT NULL COMMENT '发热量Qnet',
  `fixed_carbon` decimal(6,2) DEFAULT NULL COMMENT '固定碳',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1有效，0无效',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `report_no` varchar(64) DEFAULT NULL COMMENT '化验报告号',
  `standard_basis` varchar(200) DEFAULT NULL COMMENT '采样/制样/化验依据',
  PRIMARY KEY (`id`),
  KEY `fk_quality_coal` (`coal_id`),
  CONSTRAINT `fk_quality_coal` FOREIGN KEY (`coal_id`) REFERENCES `coal_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='煤质指标表';

LOCK TABLES `coal_quality` WRITE;
/*!40000 ALTER TABLE `coal_quality` DISABLE KEYS */;

INSERT INTO `coal_quality` (`id`, `coal_id`, `batch_no`, `sample_stage`, `related_batch_no`, `sample_time`, `sample_point`, `ash_content`, `sulfur_content`, `moisture_content`, `volatile_content`, `calorific_value`, `fixed_carbon`, `status`, `create_time`, `update_time`, `report_no`, `standard_basis`)
VALUES
	(1,1,'GEO-JJT-112','geological','MS-JJT-112','2026-04-24 08:00:00','金鸡滩112工作面地质样',7.76,0.74,6.80,31.50,6250.00,53.20,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042501-GEO','公开煤质资料 + GB/T 212 工业分析方法参考'),
	(2,2,'GEO-JJT-118','geological','MS-JJT-118','2026-04-24 08:20:00','金鸡滩118工作面地质样',8.20,0.71,6.50,30.80,6200.00,53.80,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042502-GEO','公开煤质资料近似构造 + GB/T 212 工业分析方法参考'),
	(3,3,'GEO-YPH-203','geological','MS-YPH-203','2026-04-24 08:40:00','营盘壕203工作面地质样',8.90,0.68,5.60,32.80,6988.00,52.70,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042503-GEO','公开煤质资料 + GB/T 212 工业分析方法参考'),
	(4,4,'GEO-PS-410','geological','MS-PS-410','2026-04-24 09:00:00','平朔410工作面地质样',32.50,0.62,8.50,36.00,4550.00,42.10,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042504-GEO','公开煤质区间近似构造 + GB/T 212 工业分析方法参考'),
	(5,5,'GEO-WCW-305','geological','MS-WCW-305','2026-04-24 09:20:00','五彩湾305采区地质样',16.80,0.45,11.20,38.00,4850.00,44.50,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042505-GEO','公开煤质特征近似构造 + GB/T 212 工业分析方法参考'),
	(6,1,'RAW-RC2026042501','raw_coal','RC2026042501','2026-04-25 10:00:00','RAW-WH-01入仓采样点',8.10,0.75,7.10,31.20,6210.00,52.90,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042501-RAW','GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'),
	(7,3,'RAW-RC2026042502','raw_coal','RC2026042502','2026-04-25 11:10:00','RAW-WH-02入仓采样点',9.20,0.69,5.90,32.40,6900.00,52.40,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042502-RAW','GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'),
	(8,4,'RAW-RC2026042503','raw_coal','RC2026042503','2026-04-25 10:40:00','RAW-WH-03入仓采样点',33.80,0.64,8.90,35.80,4480.00,41.30,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042503-RAW','GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'),
	(9,2,'RAW-RC2026042504','raw_coal','RC2026042504','2026-04-25 23:20:00','RAW-WH-04入仓采样点',8.60,0.72,6.80,30.50,6180.00,53.10,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042504-RAW','GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'),
	(10,5,'RAW-RC2026042505','raw_coal','RC2026042505','2026-04-26 09:00:00','RAW-WH-05入仓采样点',17.20,0.46,11.60,38.20,4820.00,43.60,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042505-RAW','GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'),
	(11,1,'FEED-WP2026042501','wash_feed','WP2026042501','2026-04-25 13:05:00','WP2026042501入洗皮带采样点',8.57,0.72,6.59,31.71,6502.00,52.80,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042501-FEED','GB/T 19494.1 采样、GB/T 212 工业分析'),
	(12,4,'FEED-WP2026042502','wash_feed','WP2026042502','2026-04-25 17:05:00','WP2026042502入洗皮带采样点',30.20,0.65,8.60,35.10,4720.00,42.40,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042502-FEED','GB/T 19494.1 采样、GB/T 212 工业分析'),
	(13,5,'FEED-WP2026042601','wash_feed','WP2026042601','2026-04-26 08:05:00','WP2026042601入洗皮带采样点',22.51,0.52,10.74,37.14,4711.00,42.80,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042601-FEED','GB/T 19494.1 采样、GB/T 212 工业分析'),
	(14,1,'CL-PB2026042501','clean_coal','PB2026042501-CL','2026-04-25 16:35:00','PROD-WH-01产品仓采样点',7.20,0.68,6.20,31.60,6600.00,54.10,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042501-CL','GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'),
	(15,4,'CL-PB2026042502','clean_coal','PB2026042502-CL','2026-04-25 21:45:00','PROD-WH-02产品仓采样点',15.80,0.61,7.80,34.10,5220.00,46.30,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042502-CL','GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'),
	(16,5,'CL-PB2026042601','clean_coal','PB2026042601-CL','2026-04-26 10:45:00','PROD-WH-03产品仓采样点',18.60,0.52,10.10,37.20,4850.00,43.90,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042601-CL','GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'),
	(17,1,'FINAL-FP2026042501','final_product','FP2026042501','2026-04-26 09:40:00','装车口机械化采样点',10.68,0.65,6.88,32.40,6048.00,50.70,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042501-FINAL','GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'),
	(18,1,'FINAL-FP2026042502','final_product','FP2026042502','2026-04-26 10:40:00','装车口机械化采样点',13.18,0.61,7.25,33.10,5735.00,47.70,1,'2026-04-25 16:14:38','2026-04-25 16:14:38','LAB2026042502-FINAL','GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析');

/*!40000 ALTER TABLE `coal_quality` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 coal_type
# ------------------------------------------------------------

DROP TABLE IF EXISTS `coal_type`;

CREATE TABLE `coal_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `coal_code` varchar(50) NOT NULL COMMENT '煤种编号',
  `coal_name` varchar(100) NOT NULL COMMENT '煤种名称',
  `coal_category` varchar(50) DEFAULT NULL COMMENT '煤种类别',
  `source_area` varchar(100) DEFAULT NULL COMMENT '产地/矿区',
  `purchase_price` decimal(10,2) DEFAULT NULL COMMENT '采购单价（元/吨）',
  `transport_mode` varchar(50) DEFAULT NULL COMMENT '运输方式',
  `blendable_flag` tinyint NOT NULL DEFAULT '1' COMMENT '是否可参与配煤：1是，0否',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `coal_code` (`coal_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='煤种基础信息表';

LOCK TABLES `coal_type` WRITE;
/*!40000 ALTER TABLE `coal_type` DISABLE KEYS */;

INSERT INTO `coal_type` (`id`, `coal_code`, `coal_name`, `coal_category`, `source_area`, `purchase_price`, `transport_mode`, `blendable_flag`, `remark`, `create_time`, `update_time`)
VALUES
	(1,'CT-JJT-CY','金鸡滩长焰煤','长焰煤','陕西榆林金鸡滩矿区',505.00,'铁路',1,'公开资料显示金鸡滩煤矿原煤低灰、低硫，发热量约6100-6300kcal/kg，适合作为低硫高热值配煤资源。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(2,'CT-JJT-NM','金鸡滩不粘煤','不粘煤','陕西榆林金鸡滩矿区',515.00,'铁路',1,'用于稳定库存和低硫动力煤配煤演示。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(3,'CT-YPH-CY','营盘壕长焰煤','长焰煤','内蒙古鄂尔多斯营盘壕矿区',560.00,'铁路',1,'公开资料显示营盘壕煤发热量接近7000kcal/kg，可用于热值补偿。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(4,'CT-PS-DL','平朔动力煤','动力煤','山西朔州平朔矿区',385.00,'铁路',1,'构造高灰原煤洗选降灰场景，洗后产品参与成本优先配煤。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(5,'CT-WCW-DL','五彩湾动力煤','动力煤','新疆准东五彩湾矿区',360.00,'铁路',1,'构造低成本、高挥发分动力煤场景，适合成本优先订单但需控制灰分和水分。','2026-04-25 16:14:38','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `coal_type` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 final_product_inspection
# ------------------------------------------------------------

DROP TABLE IF EXISTS `final_product_inspection`;

CREATE TABLE `final_product_inspection` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_no` varchar(64) NOT NULL COMMENT '质检报告号',
  `product_batch_id` bigint NOT NULL COMMENT '最终产品批次ID',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `plan_id` bigint DEFAULT NULL COMMENT '方案ID',
  `sample_time` datetime NOT NULL COMMENT '采样时间',
  `sample_point` varchar(100) DEFAULT NULL COMMENT '采样点',
  `ash_content` decimal(8,2) DEFAULT NULL COMMENT '灰分%',
  `sulfur_content` decimal(8,3) DEFAULT NULL COMMENT '硫分%',
  `moisture_content` decimal(8,2) DEFAULT NULL COMMENT '水分%',
  `volatile_content` decimal(8,2) DEFAULT NULL COMMENT '挥发分%',
  `calorific_value` decimal(10,2) DEFAULT NULL COMMENT '发热量',
  `qualified_flag` tinyint DEFAULT NULL COMMENT '是否合格：1合格，0不合格',
  `inspector` varchar(50) DEFAULT NULL COMMENT '检验员',
  `standard_basis` varchar(200) DEFAULT NULL COMMENT '依据标准',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_final_report_no` (`report_no`),
  KEY `idx_final_product_batch_id` (`product_batch_id`),
  KEY `idx_final_order_id` (`order_id`),
  KEY `idx_final_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='最终产品质检表';

LOCK TABLES `final_product_inspection` WRITE;
/*!40000 ALTER TABLE `final_product_inspection` DISABLE KEYS */;

INSERT INTO `final_product_inspection` (`id`, `report_no`, `product_batch_id`, `order_id`, `plan_id`, `sample_time`, `sample_point`, `ash_content`, `sulfur_content`, `moisture_content`, `volatile_content`, `calorific_value`, `qualified_flag`, `inspector`, `standard_basis`, `remark`, `create_time`)
VALUES
	(1,'QI2026042501',6,1,1,'2026-04-26 09:40:00','装车口机械化采样点',10.68,0.652,6.88,32.40,6048.00,1,'质检员','GB/T 19494.1、GB/T 474、GB/T 212；商品煤质量档案要求','灰分、硫分、水分、发热量均满足订单要求，准予发运','2026-04-25 16:14:38'),
	(2,'QI2026042502',7,2,2,'2026-04-26 10:40:00','装车口机械化采样点',13.18,0.610,7.25,33.10,5735.00,0,'质检员','GB/T 19494.1、GB/T 474、GB/T 212；商品煤质量档案要求','硫分0.610%高于订单上限0.60%，标记为不合格/风险场景','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `final_product_inspection` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 inventory
# ------------------------------------------------------------

DROP TABLE IF EXISTS `inventory`;

CREATE TABLE `inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `coal_id` bigint NOT NULL COMMENT '煤种ID',
  `warehouse_code` varchar(50) DEFAULT NULL COMMENT '仓库/煤仓编号',
  `stock_quantity` decimal(12,2) NOT NULL COMMENT '库存量（吨）',
  `available_quantity` decimal(12,2) NOT NULL COMMENT '可用库存量（吨）',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '库存更新时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1正常，0停用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `material_stage` varchar(50) DEFAULT 'coal_type' COMMENT '物料阶段：raw_coal/product_batch/final_product/coal_type',
  `raw_batch_no` varchar(64) DEFAULT NULL COMMENT '原煤批次号',
  `product_batch_no` varchar(64) DEFAULT NULL COMMENT '产品批次号',
  `locked_quantity` decimal(12,2) DEFAULT '0.00' COMMENT '锁定量',
  PRIMARY KEY (`id`),
  KEY `fk_inventory_coal` (`coal_id`),
  CONSTRAINT `fk_inventory_coal` FOREIGN KEY (`coal_id`) REFERENCES `coal_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存信息表';

LOCK TABLES `inventory` WRITE;
/*!40000 ALTER TABLE `inventory` DISABLE KEYS */;

INSERT INTO `inventory` (`id`, `coal_id`, `warehouse_code`, `stock_quantity`, `available_quantity`, `update_time`, `status`, `remark`, `material_stage`, `raw_batch_no`, `product_batch_no`, `locked_quantity`)
VALUES
	(1,1,'RAW-WH-01',4200.00,4200.00,'2026-04-25 10:30:00',1,'金鸡滩长焰煤原煤库存','raw_coal','RC2026042501',NULL,0.00),
	(2,3,'RAW-WH-02',3600.00,3600.00,'2026-04-25 11:30:00',1,'营盘壕高热值原煤库存','raw_coal','RC2026042502',NULL,0.00),
	(3,4,'RAW-WH-03',5000.00,5000.00,'2026-04-25 11:00:00',1,'平朔高灰原煤库存，需洗选','raw_coal','RC2026042503',NULL,0.00),
	(4,2,'RAW-WH-04',2800.00,2800.00,'2026-04-25 23:40:00',1,'金鸡滩不粘煤原煤库存','raw_coal','RC2026042504',NULL,0.00),
	(5,5,'RAW-WH-05',3900.00,3900.00,'2026-04-26 09:20:00',1,'五彩湾低成本动力煤原煤库存','raw_coal','RC2026042505',NULL,0.00),
	(6,1,'PROD-WH-01',3744.00,744.00,'2026-04-25 16:50:00',1,'低硫高热值精煤产品库存，3000吨用于订单O2026042501','product_batch',NULL,'PB2026042501-CL',3000.00),
	(7,1,'PROD-WH-MD01',624.00,624.00,'2026-04-25 16:50:00',1,'中煤副产品库存，不参与高要求订单','product_batch',NULL,'PB2026042501-MD',0.00),
	(8,4,'PROD-WH-02',2436.00,436.00,'2026-04-25 22:00:00',1,'洗选降灰精煤产品库存，2000吨用于订单O2026042501/O2026042502','product_batch',NULL,'PB2026042502-CL',2000.00),
	(9,4,'GANGUE-YARD-01',672.00,672.00,'2026-04-25 22:00:00',1,'矸石副产品堆场，不参与配煤','product_batch',NULL,'PB2026042502-GG',0.00),
	(10,5,'PROD-WH-03',2000.00,1250.00,'2026-04-26 11:00:00',1,'成本优先动力煤产品库存，750吨被候选方案锁定','product_batch',NULL,'PB2026042601-CL',750.00),
	(11,1,'FINAL-WH-01',5000.00,0.00,'2026-04-26 09:30:00',1,'最终商品煤已发运，库存归零','final_product',NULL,'FP2026042501',0.00),
	(12,1,'FINAL-WH-02',3200.00,3200.00,'2026-04-26 10:30:00',1,'最终商品煤待处理，风险场景：硫分超订单上限','final_product',NULL,'FP2026042502',0.00);

/*!40000 ALTER TABLE `inventory` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 mine_source
# ------------------------------------------------------------

DROP TABLE IF EXISTS `mine_source`;

CREATE TABLE `mine_source` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_code` varchar(64) NOT NULL COMMENT '来源编码',
  `mine_area` varchar(100) NOT NULL COMMENT '矿区名称',
  `mine_name` varchar(100) NOT NULL COMMENT '矿井名称',
  `coal_seam` varchar(100) DEFAULT NULL COMMENT '煤层编号',
  `working_face` varchar(100) DEFAULT NULL COMMENT '工作面编号',
  `coal_category` varchar(50) DEFAULT NULL COMMENT '煤种类别',
  `designed_capacity` decimal(12,2) DEFAULT NULL COMMENT '设计产能/预计产量',
  `geological_ash` decimal(8,2) DEFAULT NULL COMMENT '地质灰分%',
  `geological_sulfur` decimal(8,3) DEFAULT NULL COMMENT '地质硫分%',
  `geological_moisture` decimal(8,2) DEFAULT NULL COMMENT '地质水分%',
  `geological_volatile` decimal(8,2) DEFAULT NULL COMMENT '地质挥发分%',
  `geological_calorific` decimal(10,2) DEFAULT NULL COMMENT '地质发热量',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_code` (`source_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='矿区煤种煤质来源表';

LOCK TABLES `mine_source` WRITE;
/*!40000 ALTER TABLE `mine_source` DISABLE KEYS */;

INSERT INTO `mine_source` (`id`, `source_code`, `mine_area`, `mine_name`, `coal_seam`, `working_face`, `coal_category`, `designed_capacity`, `geological_ash`, `geological_sulfur`, `geological_moisture`, `geological_volatile`, `geological_calorific`, `status`, `remark`, `create_time`, `update_time`)
VALUES
	(1,'MS-JJT-112','陕西榆林','金鸡滩煤矿','3-1煤层','112工作面','长焰煤',1800.00,7.76,0.740,6.80,31.50,6250.00,1,'公开资料参考：原煤硫分约0.74%、灰分约7.76%、发热量约6100-6300kcal/kg。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(2,'MS-JJT-118','陕西榆林','金鸡滩煤矿','3-1煤层','118工作面','不粘煤',1800.00,8.20,0.710,6.50,30.80,6200.00,1,'同矿区相近煤质构造，用于库存稳定性和候选配煤演示。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(3,'MS-YPH-203','内蒙古鄂尔多斯','营盘壕煤矿','2-2煤层','203工作面','长焰煤',1200.00,8.90,0.680,5.60,32.80,6988.00,1,'公开资料参考：营盘壕煤发热量接近29.21MJ/kg，约6988kcal/kg。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(4,'MS-PS-410','山西朔州','平朔矿区','4-1煤层','410工作面','动力煤',2000.00,32.50,0.620,8.50,36.00,4550.00,1,'构造高灰原煤入洗降灰场景。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(5,'MS-WCW-305','新疆准东','五彩湾露天矿','B1煤层','305采区','动力煤',1500.00,16.80,0.450,11.20,38.00,4850.00,1,'构造低成本动力煤场景，低硫但水分较高。','2026-04-25 16:14:38','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `mine_source` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 model_config
# ------------------------------------------------------------

DROP TABLE IF EXISTS `model_config`;

CREATE TABLE `model_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `model_name` varchar(100) NOT NULL COMMENT '模型名称',
  `model_type` varchar(50) DEFAULT NULL COMMENT '模型类型：LLM/RAG/OPT',
  `api_url` varchar(255) DEFAULT NULL COMMENT '接口地址',
  `api_key` varchar(255) DEFAULT NULL COMMENT '接口密钥',
  `temperature` decimal(4,2) DEFAULT NULL COMMENT '温度参数',
  `top_p` decimal(4,2) DEFAULT NULL COMMENT 'Top-p参数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0禁用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模型配置表';

LOCK TABLES `model_config` WRITE;
/*!40000 ALTER TABLE `model_config` DISABLE KEYS */;

INSERT INTO `model_config` (`id`, `model_name`, `model_type`, `api_url`, `api_key`, `temperature`, `top_p`, `status`, `remark`, `create_time`, `update_time`)
VALUES
	(1,'本地Ollama-Qwen示例模型','LLM','http://localhost:11434/v1/chat/completions','',0.30,0.90,0,'本地 OpenAI 兼容接口示例，可根据实际模型名称修改','2026-04-20 14:27:28','2026-04-24 20:51:54'),
	(2,'RAG关键词检索配置','RAG',NULL,NULL,NULL,NULL,1,'当前阶段使用 MySQL LIKE 关键词检索作为轻量 RAG 检索方式','2026-04-20 14:45:23','2026-04-24 20:51:47'),
	(3,'规则评分优化模块','OPT',NULL,NULL,NULL,NULL,1,'当前阶段使用规则筛选和评分排序，后续可扩展多目标优化算法','2026-04-24 13:33:45','2026-04-24 20:30:34'),
	(4,'qwen3:4b','LOCAL_OLLAMA','http://localhost:11434/api/chat','',0.70,0.90,0,'本地 Ollama 开源模型','2026-04-24 18:30:51','2026-04-24 20:51:53'),
	(5,'deepseek-v4-pro','LLM','https://api.deepseek.com/chat/completions','sk-9bb2830857284701b89de20ee9df9fbd',0.30,0.80,0,NULL,'2026-04-24 20:53:33','2026-04-25 10:45:39'),
	(6,'deepseek-v4-flash','LLM','https://api.deepseek.com/chat/completions','sk-9bb2830857284701b89de20ee9df9fbd',0.30,0.80,0,NULL,'2026-04-25 10:45:35','2026-04-25 15:58:17'),
	(7,'qwen3:4b-local-demo','LLM','http://localhost:11434/v1/chat/completions','EMPTY',0.30,0.85,0,'本地 Ollama OpenAI 兼容接口演示配置','2026-04-25 13:28:15','2026-04-25 13:51:01'),
	(8,'rag-keyword-demo','RAG','http://localhost:8080/rag/search','EMPTY',0.20,0.90,1,'关键词检索 + 规则/案例召回演示配置','2026-04-25 13:28:15','2026-04-25 13:28:15'),
	(9,'blend-rule-score-demo','OPT','http://localhost:8080/blend/score','EMPTY',0.10,0.80,1,'规则筛选与评分排序演示配置','2026-04-25 13:28:15','2026-04-25 13:28:15'),
	(10,'qwen3-vl:4b','LLM','https://glancing-sphere-unguarded.ngrok-free.dev/api/chat',NULL,0.30,0.80,1,NULL,'2026-04-25 15:57:56','2026-04-25 15:57:56');

/*!40000 ALTER TABLE `model_config` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 orders
# ------------------------------------------------------------

DROP TABLE IF EXISTS `orders`;

CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_code` varchar(50) NOT NULL COMMENT '订单编号',
  `customer_name` varchar(100) NOT NULL COMMENT '客户名称',
  `demand_quantity` decimal(12,2) NOT NULL COMMENT '需求数量（吨）',
  `target_ash` decimal(6,2) DEFAULT NULL COMMENT '目标灰分上限',
  `target_sulfur` decimal(6,2) DEFAULT NULL COMMENT '目标硫分上限',
  `target_moisture` decimal(6,2) DEFAULT NULL COMMENT '目标水分上限',
  `target_volatile` decimal(6,2) DEFAULT NULL COMMENT '目标挥发分范围参考值',
  `target_calorific` decimal(10,2) DEFAULT NULL COMMENT '目标发热量下限',
  `priority_level` int DEFAULT '1' COMMENT '优先级：1低，2中，3高',
  `delivery_date` date DEFAULT NULL COMMENT '交付日期',
  `order_status` varchar(20) DEFAULT 'pending' COMMENT '订单状态：pending/generated/completed',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_code` (`order_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单需求表';

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;

INSERT INTO `orders` (`id`, `order_code`, `customer_name`, `demand_quantity`, `target_ash`, `target_sulfur`, `target_moisture`, `target_volatile`, `target_calorific`, `priority_level`, `delivery_date`, `order_status`, `remark`, `create_time`, `update_time`)
VALUES
	(1,'O2026042501','华东热电厂',5000.00,18.00,0.80,9.00,30.00,5000.00,3,'2026-05-05','completed','低硫高热值动力煤订单，演示合格闭环。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(2,'O2026042502','长三角热电联供客户',3200.00,16.00,0.60,8.50,30.00,5200.00,3,'2026-05-08','completed','严格低硫订单，演示硫分接近/超过上限的风险反馈。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(3,'O2026042503','西北园区自备电厂',4500.00,22.00,1.20,10.00,32.00,4700.00,2,'2026-05-12','generated','成本优先订单，演示候选方案未执行。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(4,'O2026042504','煤化工气化用煤客户',2800.00,14.00,0.90,8.00,31.00,5800.00,2,'2026-05-15','pending','高热值气化用煤需求，用于后续生成方案。','2026-04-25 16:14:38','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 product_batch
# ------------------------------------------------------------

DROP TABLE IF EXISTS `product_batch`;

CREATE TABLE `product_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_batch_no` varchar(64) NOT NULL COMMENT '产品批次号',
  `wash_batch_id` bigint DEFAULT NULL COMMENT '来源洗选批次ID',
  `coal_id` bigint DEFAULT NULL COMMENT '关联煤种ID，用于兼容煤种级配煤',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `plan_id` bigint DEFAULT NULL COMMENT '关联配煤方案ID',
  `product_type` varchar(50) NOT NULL COMMENT '产品类型：clean_coal/middlings/slime/gangue/mixed_product',
  `product_name` varchar(100) DEFAULT NULL COMMENT '产品名称',
  `quantity` decimal(12,2) NOT NULL COMMENT '产量，吨',
  `available_quantity` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '可用量，吨',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '产品仓/堆场',
  `ash_content` decimal(8,2) DEFAULT NULL COMMENT '灰分%',
  `sulfur_content` decimal(8,3) DEFAULT NULL COMMENT '硫分%',
  `moisture_content` decimal(8,2) DEFAULT NULL COMMENT '水分%',
  `volatile_content` decimal(8,2) DEFAULT NULL COMMENT '挥发分%',
  `calorific_value` decimal(10,2) DEFAULT NULL COMMENT '发热量',
  `status` varchar(30) DEFAULT 'available' COMMENT '状态：available/locked/used/shipped/closed',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_batch_no` (`product_batch_no`),
  KEY `idx_product_wash_batch_id` (`wash_batch_id`),
  KEY `idx_product_order_id` (`order_id`),
  KEY `idx_product_plan_id` (`plan_id`),
  KEY `idx_product_type` (`product_type`),
  KEY `idx_product_status` (`status`),
  KEY `idx_product_coal_id` (`coal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品批次表';

LOCK TABLES `product_batch` WRITE;
/*!40000 ALTER TABLE `product_batch` DISABLE KEYS */;

INSERT INTO `product_batch` (`id`, `product_batch_no`, `wash_batch_id`, `coal_id`, `order_id`, `plan_id`, `product_type`, `product_name`, `quantity`, `available_quantity`, `warehouse_code`, `ash_content`, `sulfur_content`, `moisture_content`, `volatile_content`, `calorific_value`, `status`, `remark`, `create_time`, `update_time`)
VALUES
	(1,'PB2026042501-CL',1,1,NULL,NULL,'clean_coal','低硫高热值精煤产品',3744.00,744.00,'PROD-WH-01',7.20,0.680,6.20,31.60,6600.00,'used','WP2026042501精煤，适合低硫高热值订单','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(2,'PB2026042501-MD',1,1,NULL,NULL,'middlings','中煤副产品',624.00,624.00,'PROD-WH-MD01',22.50,0.820,8.50,33.20,4300.00,'available','中煤副产品，不用于高要求订单','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(3,'PB2026042502-CL',2,4,NULL,NULL,'clean_coal','洗选降灰精煤产品',2436.00,436.00,'PROD-WH-02',15.80,0.610,7.80,34.10,5220.00,'used','高灰原煤洗选后精煤','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(4,'PB2026042502-GG',2,4,NULL,NULL,'gangue','矸石副产品',672.00,672.00,'GANGUE-YARD-01',68.00,0.550,9.50,22.00,1500.00,'available','矸石副产品，不参与配煤','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(5,'PB2026042601-CL',3,5,NULL,NULL,'clean_coal','成本优先动力煤产品',2000.00,2000.00,'PROD-WH-03',18.60,0.520,10.10,37.20,4850.00,'available','成本优先订单备选产品','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(6,'FP2026042501',NULL,1,1,1,'mixed_product','低硫高热值最终商品煤',5000.00,0.00,'FINAL-WH-01',10.68,0.652,6.88,32.40,6048.00,'shipped','订单O2026042501最终商品煤，质检合格并已发运','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(7,'FP2026042502',NULL,1,2,2,'mixed_product','严格低硫订单最终商品煤',3200.00,3200.00,'FINAL-WH-02',13.18,0.610,7.25,33.10,5735.00,'used','订单O2026042502最终商品煤，硫分略超目标上限，作为风险场景','2026-04-25 16:14:38','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `product_batch` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 rag_knowledge
# ------------------------------------------------------------

DROP TABLE IF EXISTS `rag_knowledge`;

CREATE TABLE `rag_knowledge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `knowledge_code` varchar(64) NOT NULL COMMENT '知识编号',
  `title` varchar(200) NOT NULL COMMENT '知识标题',
  `knowledge_type` varchar(50) NOT NULL COMMENT '知识类型：rule/case/term/doc',
  `content` text NOT NULL COMMENT '知识正文',
  `source_table` varchar(100) DEFAULT NULL COMMENT '来源表',
  `source_id` bigint DEFAULT NULL COMMENT '来源记录ID',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0停用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_code` (`knowledge_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG统一知识检索表';

LOCK TABLES `rag_knowledge` WRITE;
/*!40000 ALTER TABLE `rag_knowledge` DISABLE KEYS */;

INSERT INTO `rag_knowledge` (`id`, `knowledge_code`, `title`, `knowledge_type`, `content`, `source_table`, `source_id`, `tags`, `status`, `create_time`, `update_time`)
VALUES
	(1,'K-RULE-001','低硫订单优先低硫煤规则','rule','当订单硫分上限较低时，应优先选择硫分较低且煤质稳定的产品批次参与配煤；若硫分预测值接近订单上限，应给出风险提示并建议增加低硫煤比例。','rule_knowledge',1,'低硫,配煤规则,硫分,风险控制',1,'2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(2,'K-RULE-002','库存可用性与锁定规则','rule','参与配煤的产品批次必须存在可用库存，方案使用量不得超过可用库存。方案被选择或执行后，应在库存中体现锁定量或扣减量。','rule_knowledge',5,'库存,锁定量,可执行性,产品批次',1,'2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(3,'K-CASE-001','低硫高热值商品煤成功案例','case','订单O2026042501要求5000吨低硫高热值动力煤，方案P2026042501-A采用PB2026042501-CL 60%与PB2026042502-CL 40%，最终产品FP2026042501质检合格并发运。','case_sample',1,'成功案例,低硫,高热值,发运',1,'2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(4,'K-CASE-002','严格低硫订单风险反馈案例','case','订单O2026042502硫分上限0.60%，方案P2026042502-RISK理论预测接近上限，最终产品FP2026042502实测硫分0.610%，说明严格低硫订单需要保留更大质量余量。','case_sample',2,'风险案例,硫分超标,反馈,质量余量',1,'2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(5,'K-TERM-001','批次血缘追溯说明','term','批次血缘用于记录父批次到子批次的业务关系，例如原煤批次进入洗选批次、洗选批次产出产品批次、产品批次混配生成最终产品、最终产品发运形成发运批次。',NULL,NULL,'批次血缘,追溯,数据链条',1,'2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(6,'K-DOC-001','煤矿智能配煤全链路流程说明','doc','系统业务链路包括矿区煤种煤质、原煤生产、原煤入仓、洗选加工、洗后产品、产品库存、订单驱动配煤、最终产品质检、发运交付和执行反馈。',NULL,NULL,'全链路,原煤生产,洗选加工,最终产品',1,'2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(7,'K-DOC-002','多阶段煤质数据说明','doc','煤质数据应按采样阶段区分，包括geological地质煤质、raw_coal原煤煤质、wash_feed入洗煤质、clean_coal洗后产品煤质、mixed_product混配产品煤质和final_product最终商品煤煤质。',NULL,NULL,'煤质,采样阶段,质量档案',1,'2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(8,'K-DOC-003','商品煤分质管理说明','doc','商品煤生产、加工、储运、销售等环节均应重视质量管理。不同质量商品煤应分质堆存、分质装车，并建立商品煤质量档案。',NULL,NULL,'商品煤质量,分质堆存,分质装车,质量档案',1,'2026-04-25 16:14:38','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `rag_knowledge` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 rag_retrieval_log
# ------------------------------------------------------------

DROP TABLE IF EXISTS `rag_retrieval_log`;

CREATE TABLE `rag_retrieval_log` (
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
  KEY `idx_rag_log_biz` (`biz_type`,`biz_id`),
  KEY `idx_rag_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG检索与生成日志表';

LOCK TABLES `rag_retrieval_log` WRITE;
/*!40000 ALTER TABLE `rag_retrieval_log` DISABLE KEYS */;

INSERT INTO `rag_retrieval_log` (`id`, `biz_type`, `biz_id`, `query_text`, `keywords`, `retrieved_ids`, `model_name`, `prompt_text`, `model_output`, `create_time`)
VALUES
	(1,'blend_generate',1,'为订单O2026042501生成低硫高热值配煤方案，并解释为什么推荐该方案。','低硫,高热值,订单O2026042501,配煤方案,库存','1,2,3,6','RAG关键词检索配置','订单约束：灰分≤18%，硫分≤0.80%，发热量≥5000；候选批次：PB2026042501-CL、PB2026042502-CL；请结合规则和案例生成解释。','推荐方案P2026042501-A。该方案使用低硫高热值精煤作为主配煤，并搭配洗选降灰精煤，预测指标满足订单要求，且最终质检合格。','2026-04-25 16:14:38'),
	(2,'blend_generate',2,'解释订单O2026042502为什么被标记为严格低硫风险方案。','严格低硫,硫分0.60,风险方案,质量余量','1,4,7','RAG关键词检索配置','订单约束：硫分≤0.60%；方案预测硫分约0.598%；最终质检硫分0.610%；请给出风险解释。','该方案理论预测接近硫分上限，质量余量不足。最终质检硫分0.610%超过订单上限，建议增加低硫精煤比例或更换更低硫批次。','2026-04-25 16:14:38'),
	(3,'trace',1,'从最终产品FP2026042501反查它的原煤来源、洗选批次和发运信息。','FP2026042501,批次血缘,原煤,洗选,发运','5,6,3','RAG关键词检索配置','请根据batch_lineage说明最终产品FP2026042501的来源路径。','FP2026042501由PB2026042501-CL和PB2026042502-CL混配形成；上游分别来自WP2026042501和WP2026042502；再向上可追溯到RC2026042501、RC2026042502、RC2026042503和RC2026042504；该最终产品已通过SHIP2026042501发运。','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `rag_retrieval_log` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 raw_coal_batch
# ------------------------------------------------------------

DROP TABLE IF EXISTS `raw_coal_batch`;

CREATE TABLE `raw_coal_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `raw_batch_no` varchar(64) NOT NULL COMMENT '原煤批次号',
  `source_id` bigint NOT NULL COMMENT '矿区来源ID',
  `coal_id` bigint DEFAULT NULL COMMENT '关联煤种ID',
  `production_date` date NOT NULL COMMENT '生产日期',
  `shift_no` varchar(20) DEFAULT NULL COMMENT '班次',
  `output_quantity` decimal(12,2) NOT NULL COMMENT '原煤产量，吨',
  `gangue_rate` decimal(8,2) DEFAULT NULL COMMENT '矸石率%',
  `destination` varchar(100) DEFAULT NULL COMMENT '去向',
  `warehouse_code` varchar(64) DEFAULT NULL COMMENT '入库仓号或堆场位置',
  `status` varchar(30) DEFAULT 'produced' COMMENT '状态：produced/stored/washing/washed/closed',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_raw_batch_no` (`raw_batch_no`),
  KEY `idx_raw_source_id` (`source_id`),
  KEY `idx_raw_coal_id` (`coal_id`),
  KEY `idx_raw_production_date` (`production_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='原煤生产批次表';

LOCK TABLES `raw_coal_batch` WRITE;
/*!40000 ALTER TABLE `raw_coal_batch` DISABLE KEYS */;

INSERT INTO `raw_coal_batch` (`id`, `raw_batch_no`, `source_id`, `coal_id`, `production_date`, `shift_no`, `output_quantity`, `gangue_rate`, `destination`, `warehouse_code`, `status`, `remark`, `create_time`, `update_time`)
VALUES
	(1,'RC2026042501',1,1,'2026-04-25','早班',4200.00,3.20,'原煤仓 RAW-WH-01','RAW-WH-01','stored','金鸡滩112工作面低灰低硫原煤。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(2,'RC2026042502',3,3,'2026-04-25','中班',3600.00,2.80,'原煤仓 RAW-WH-02','RAW-WH-02','stored','营盘壕203工作面高热值原煤。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(3,'RC2026042503',4,4,'2026-04-25','早班',5000.00,12.50,'原煤仓 RAW-WH-03','RAW-WH-03','stored','平朔410工作面高灰原煤，后续入洗降灰。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(4,'RC2026042504',2,2,'2026-04-25','夜班',2800.00,3.80,'原煤仓 RAW-WH-04','RAW-WH-04','stored','金鸡滩118工作面不粘煤。','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(5,'RC2026042505',5,5,'2026-04-26','早班',3900.00,5.20,'原煤仓 RAW-WH-05','RAW-WH-05','stored','五彩湾低成本动力煤，水分偏高。','2026-04-25 16:14:38','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `raw_coal_batch` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 rule_knowledge
# ------------------------------------------------------------

DROP TABLE IF EXISTS `rule_knowledge`;

CREATE TABLE `rule_knowledge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编号',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_type` varchar(50) NOT NULL COMMENT '规则类型：质量约束/库存约束/配比约束/经验规则',
  `rule_content` text NOT NULL COMMENT '规则内容',
  `applicable_scope` varchar(255) DEFAULT NULL COMMENT '适用范围',
  `priority_level` int DEFAULT '1' COMMENT '优先级',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0停用',
  `source_desc` varchar(255) DEFAULT NULL COMMENT '规则来源说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `business_stage` varchar(50) DEFAULT 'blending' COMMENT '业务阶段',
  `quality_indicator` varchar(200) DEFAULT NULL COMMENT '相关质量指标',
  `material_type` varchar(50) DEFAULT NULL COMMENT '适用物料类型',
  PRIMARY KEY (`id`),
  UNIQUE KEY `rule_code` (`rule_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='规则知识表';

LOCK TABLES `rule_knowledge` WRITE;
/*!40000 ALTER TABLE `rule_knowledge` DISABLE KEYS */;

INSERT INTO `rule_knowledge` (`id`, `rule_code`, `rule_name`, `rule_type`, `rule_content`, `applicable_scope`, `priority_level`, `status`, `source_desc`, `create_time`, `update_time`, `business_stage`, `quality_indicator`, `material_type`)
VALUES
	(1,'R001','低硫订单优先低硫煤规则','经验规则','当订单硫分上限较低时，应优先选择硫分较低且煤质稳定的产品批次参与配煤，并谨慎使用硫分接近上限的批次。','低硫订单、环保约束严格订单',5,1,'根据商品煤质量管理要求和智能配煤实践整理','2026-04-25 16:14:38','2026-04-25 16:14:38','blending','sulfur','product_batch'),
	(2,'R002','高硫煤限配规则','配比约束','当订单硫分上限小于等于0.80%时，硫分偏高或波动较大的煤种不宜作为主配煤种，应限制其参与比例。','target_sulfur <= 0.80 的订单',5,1,'根据硫分约束整理','2026-04-25 16:14:38','2026-04-25 16:14:38','blending','sulfur','product_batch'),
	(3,'R003','灰分约束优先校验规则','质量约束','配煤方案生成后必须校验预测灰分，若预测灰分高于订单灰分上限，则该方案应判定为不可行。','全部订单',5,1,'根据煤质指标约束整理','2026-04-25 16:14:38','2026-04-25 16:14:38','blending','ash','product_batch'),
	(4,'R004','发热量下限校验规则','质量约束','配煤方案预测发热量应不低于订单目标发热量下限；若热值不足，可引入高热值煤种补偿。','含目标发热量下限的订单',5,1,'根据动力煤质量评价和配煤优化要求整理','2026-04-25 16:14:38','2026-04-25 16:14:38','blending','calorific','product_batch'),
	(5,'R005','库存可用性约束规则','库存约束','参与配煤的产品批次必须存在可用库存，且方案使用量不得超过可用量；已执行方案应同步锁定或扣减库存。','全部订单',5,1,'根据配煤方案可执行性要求整理','2026-04-25 16:14:38','2026-04-25 16:14:38','inventory','quantity','product_batch'),
	(6,'R006','分质堆存与分质装车规则','经验规则','不同质量商品煤应分质堆存、分质装车，并建立商品煤质量档案，避免储运环节降低煤炭质量。','产品仓储和发运',4,1,'根据商品煤质量管理要求整理','2026-04-25 16:14:38','2026-04-25 16:14:38','shipment','quality','final_product'),
	(7,'R007','低成本煤使用边界规则','配比约束','低价煤种可用于降低方案成本，但若灰分、水分或硫分偏高，应设置使用边界，避免质量不达标。','成本优先订单',4,1,'根据成本与质量多目标权衡整理','2026-04-25 16:14:38','2026-04-25 16:14:38','blending','ash,moisture,sulfur','product_batch'),
	(8,'R008','相似案例参考规则','经验规则','当存在与当前订单在灰分、硫分、发热量或需求量方面相近的历史案例时，可将其作为候选方案和解释生成的参考。','案例检索和RAG解释',3,1,'根据案例库设计要求整理','2026-04-25 16:14:38','2026-04-25 16:14:38','rag','case','all');

/*!40000 ALTER TABLE `rule_knowledge` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 shipment_delivery
# ------------------------------------------------------------

DROP TABLE IF EXISTS `shipment_delivery`;

CREATE TABLE `shipment_delivery` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shipment_no` varchar(64) NOT NULL COMMENT '发运批次号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `product_batch_id` bigint NOT NULL COMMENT '最终产品批次ID',
  `customer_name` varchar(100) DEFAULT NULL COMMENT '客户名称',
  `shipment_quantity` decimal(12,2) NOT NULL COMMENT '发运量，吨',
  `vehicle_no` varchar(100) DEFAULT NULL COMMENT '车号/车皮号',
  `loading_time` datetime DEFAULT NULL COMMENT '装车时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '发运时间',
  `delivery_status` varchar(30) DEFAULT 'created' COMMENT '状态：created/loaded/shipped/received',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shipment_no` (`shipment_no`),
  KEY `idx_ship_order_id` (`order_id`),
  KEY `idx_ship_product_batch_id` (`product_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发运交付表';

LOCK TABLES `shipment_delivery` WRITE;
/*!40000 ALTER TABLE `shipment_delivery` DISABLE KEYS */;

INSERT INTO `shipment_delivery` (`id`, `shipment_no`, `order_id`, `product_batch_id`, `customer_name`, `shipment_quantity`, `vehicle_no`, `loading_time`, `delivery_time`, `delivery_status`, `remark`, `create_time`, `update_time`)
VALUES
	(1,'SHIP2026042501',1,6,'华东热电厂',5000.00,'C70-452301,C70-452302,C70-452303','2026-04-26 11:00:00','2026-04-26 13:30:00','shipped','按分质装车原则发运，质检报告QI2026042501合格','2026-04-25 16:14:38','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `shipment_delivery` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 sys_user
# ------------------------------------------------------------

DROP TABLE IF EXISTS `sys_user`;

CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `role` varchar(20) NOT NULL DEFAULT 'user' COMMENT '角色：admin/user',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;

INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `phone`, `email`, `status`, `create_time`, `update_time`)
VALUES
	(1,'admin','123456','系统管理员','admin','13800000000','admin@example.com',1,'2026-04-18 16:51:35','2026-04-24 20:30:34'),
	(2,'operator','123456','配煤业务员','user','13900000001','operator@example.com',1,'2026-04-24 20:30:34','2026-04-24 20:30:34'),
	(3,'quality','123456','煤质检测员','user','13900000002','quality@example.com',1,'2026-04-24 20:30:34','2026-04-24 20:30:34'),
	(4,'admin_demo','123456','系统管理员-演示','admin','13800000001','admin_demo@example.com',1,'2026-04-25 13:28:15','2026-04-25 13:28:15'),
	(5,'planner_demo','123456','配煤计划员-演示','user','13800000002','planner_demo@example.com',1,'2026-04-25 13:28:15','2026-04-25 13:28:15'),
	(6,'inspector_demo','123456','质检员-演示','user','13800000003','inspector_demo@example.com',1,'2026-04-25 13:28:15','2026-04-25 13:28:15');

/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 wash_input_detail
# ------------------------------------------------------------

DROP TABLE IF EXISTS `wash_input_detail`;

CREATE TABLE `wash_input_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `wash_batch_id` bigint NOT NULL COMMENT '洗选批次ID',
  `raw_batch_id` bigint NOT NULL COMMENT '原煤批次ID',
  `input_quantity` decimal(12,2) NOT NULL COMMENT '投入量，吨',
  `input_ratio` decimal(8,4) DEFAULT NULL COMMENT '投入比例',
  `quality_snapshot_json` json DEFAULT NULL COMMENT '投入时原煤煤质快照',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_wash_input_wash` (`wash_batch_id`),
  KEY `idx_wash_input_raw` (`raw_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='洗选投入明细表';

LOCK TABLES `wash_input_detail` WRITE;
/*!40000 ALTER TABLE `wash_input_detail` DISABLE KEYS */;

INSERT INTO `wash_input_detail` (`id`, `wash_batch_id`, `raw_batch_id`, `input_quantity`, `input_ratio`, `quality_snapshot_json`, `remark`, `create_time`)
VALUES
	(1,1,1,3000.00,0.5769,'{\"ash\": 8.10, \"sulfur\": 0.75, \"moisture\": 7.10, \"calorific\": 6210}','低硫低灰主投入','2026-04-25 16:14:38'),
	(2,1,2,2200.00,0.4231,'{\"ash\": 9.20, \"sulfur\": 0.69, \"moisture\": 5.90, \"calorific\": 6900}','高热值补偿投入','2026-04-25 16:14:38'),
	(3,2,3,3600.00,0.8571,'{\"ash\": 33.80, \"sulfur\": 0.64, \"moisture\": 8.90, \"calorific\": 4480}','高灰原煤主投入','2026-04-25 16:14:38'),
	(4,2,4,600.00,0.1429,'{\"ash\": 8.60, \"sulfur\": 0.72, \"moisture\": 6.80, \"calorific\": 6180}','低灰煤调节投入','2026-04-25 16:14:38'),
	(5,3,5,1700.00,0.6800,'{\"ash\": 17.20, \"sulfur\": 0.46, \"moisture\": 11.60, \"calorific\": 4820}','低成本动力煤主投入','2026-04-25 16:14:38'),
	(6,3,3,800.00,0.3200,'{\"ash\": 33.80, \"sulfur\": 0.64, \"moisture\": 8.90, \"calorific\": 4480}','高灰煤成本补充投入','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `wash_input_detail` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 wash_process_batch
# ------------------------------------------------------------

DROP TABLE IF EXISTS `wash_process_batch`;

CREATE TABLE `wash_process_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `wash_batch_no` varchar(64) NOT NULL COMMENT '洗选批次号',
  `process_type` varchar(50) DEFAULT NULL COMMENT '工艺类型',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `feed_quantity` decimal(12,2) DEFAULT NULL COMMENT '入洗量，吨',
  `clean_coal_yield` decimal(8,2) DEFAULT NULL COMMENT '精煤产率%',
  `middlings_yield` decimal(8,2) DEFAULT NULL COMMENT '中煤产率%',
  `slime_yield` decimal(8,2) DEFAULT NULL COMMENT '煤泥产率%',
  `gangue_yield` decimal(8,2) DEFAULT NULL COMMENT '矸石产率%',
  `medium_density` decimal(8,3) DEFAULT NULL COMMENT '介质密度',
  `separation_density` decimal(8,3) DEFAULT NULL COMMENT '分选密度',
  `equipment_code` varchar(100) DEFAULT NULL COMMENT '主要设备编号',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人员',
  `status` varchar(30) DEFAULT 'created' COMMENT '状态：created/running/finished/cancelled',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wash_batch_no` (`wash_batch_no`),
  KEY `idx_wash_time` (`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='洗选加工批次表';

LOCK TABLES `wash_process_batch` WRITE;
/*!40000 ALTER TABLE `wash_process_batch` DISABLE KEYS */;

INSERT INTO `wash_process_batch` (`id`, `wash_batch_no`, `process_type`, `start_time`, `end_time`, `feed_quantity`, `clean_coal_yield`, `middlings_yield`, `slime_yield`, `gangue_yield`, `medium_density`, `separation_density`, `equipment_code`, `operator_name`, `status`, `remark`, `create_time`, `update_time`)
VALUES
	(1,'WP2026042501','重介浅槽+筛分脱水','2026-04-25 13:00:00','2026-04-25 16:20:00',5200.00,72.00,12.00,5.00,11.00,1.360,1.520,'WASH-DMC-01','洗选班组A','finished','低硫高热值产品洗选批次','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(2,'WP2026042502','重介旋流器+浮选','2026-04-25 17:00:00','2026-04-25 21:30:00',4200.00,58.00,18.00,8.00,16.00,1.420,1.650,'WASH-DMC-02','洗选班组B','finished','平朔高灰原煤洗选降灰批次','2026-04-25 16:14:38','2026-04-25 16:14:38'),
	(3,'WP2026042601','筛分破碎+简易排矸','2026-04-26 08:00:00','2026-04-26 10:30:00',2500.00,80.00,8.00,4.00,8.00,1.300,1.450,'WASH-SCR-01','洗选班组C','finished','成本优先订单备选产品加工批次','2026-04-25 16:14:38','2026-04-25 16:14:38');

/*!40000 ALTER TABLE `wash_process_batch` ENABLE KEYS */;
UNLOCK TABLES;



/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
