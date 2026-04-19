# ************************************************************
# Sequel Ace SQL dump
# 版本号： 20094
#
# https://sequel-ace.com/
# https://github.com/Sequel-Ace/Sequel-Ace
#
# 主机: 127.0.0.1 (MySQL 9.3.0)
# 数据库: coal_blending_system
# 生成时间: 2026-04-18 10:07:57 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


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
  `plan_status` varchar(20) DEFAULT 'generated' COMMENT '方案状态：generated/selected/executed',
  `explanation` text COMMENT '方案解释',
  `risk_tip` text COMMENT '风险提示',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `plan_code` (`plan_code`),
  KEY `fk_plan_order` (`order_id`),
  CONSTRAINT `fk_plan_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配煤方案主表';



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
  PRIMARY KEY (`id`),
  KEY `fk_detail_plan` (`plan_id`),
  KEY `fk_detail_coal` (`coal_id`),
  CONSTRAINT `fk_detail_coal` FOREIGN KEY (`coal_id`) REFERENCES `coal_type` (`id`),
  CONSTRAINT `fk_detail_plan` FOREIGN KEY (`plan_id`) REFERENCES `blend_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配煤方案明细表';



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
  PRIMARY KEY (`id`),
  UNIQUE KEY `case_code` (`case_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史案例表';

LOCK TABLES `case_sample` WRITE;
/*!40000 ALTER TABLE `case_sample` DISABLE KEYS */;

INSERT INTO `case_sample` (`id`, `case_code`, `case_name`, `order_desc`, `blend_desc`, `result_desc`, `quality_result`, `cost_result`, `effectiveness_eval`, `status`, `create_time`, `update_time`)
VALUES
	(1,'C001','低硫动力煤配煤案例','需求5000吨，硫分不高于0.8%，热值不低于5000大卡。','采用弱粘煤+贫煤+少量不粘煤配比方案。','方案满足热值和硫分要求，成本适中。','灰分17.2%，硫分0.58%，热值5080kcal/kg',2460000.00,'良好',1,'2026-04-18 16:51:28','2026-04-18 16:51:28'),
	(2,'C002','常规动力煤供应案例','需求3000吨，热值不低于4600大卡。','采用不粘煤为主，配入少量长焰煤。','方案成本较低，库存消耗合理。','灰分18.8%，硫分1.24%，热值4720kcal/kg',1320000.00,'良好',1,'2026-04-18 16:51:28','2026-04-18 16:51:28'),
	(3,'C003','高热值配焦案例','需求2000吨，灰分不高于13%，热值高于5300大卡。','采用弱粘煤+1/3焦煤方案。','满足高热值要求，但成本较高。','灰分11.4%，硫分0.62%，热值5620kcal/kg',1325000.00,'优秀',1,'2026-04-18 16:51:28','2026-04-18 16:51:28');

/*!40000 ALTER TABLE `case_sample` ENABLE KEYS */;
UNLOCK TABLES;


# 转储表 coal_quality
# ------------------------------------------------------------

DROP TABLE IF EXISTS `coal_quality`;

CREATE TABLE `coal_quality` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `coal_id` bigint NOT NULL COMMENT '煤种ID',
  `batch_no` varchar(50) DEFAULT NULL COMMENT '批次号',
  `sample_time` datetime DEFAULT NULL COMMENT '采样时间',
  `ash_content` decimal(6,2) DEFAULT NULL COMMENT '灰分Ad',
  `sulfur_content` decimal(6,2) DEFAULT NULL COMMENT '硫分St,d',
  `moisture_content` decimal(6,2) DEFAULT NULL COMMENT '水分Mt',
  `volatile_content` decimal(6,2) DEFAULT NULL COMMENT '挥发分Vdaf',
  `calorific_value` decimal(10,2) DEFAULT NULL COMMENT '发热量Qnet',
  `fixed_carbon` decimal(6,2) DEFAULT NULL COMMENT '固定碳',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1有效，0无效',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `fk_quality_coal` (`coal_id`),
  CONSTRAINT `fk_quality_coal` FOREIGN KEY (`coal_id`) REFERENCES `coal_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='煤质指标表';

LOCK TABLES `coal_quality` WRITE;
/*!40000 ALTER TABLE `coal_quality` DISABLE KEYS */;

INSERT INTO `coal_quality` (`id`, `coal_id`, `batch_no`, `sample_time`, `ash_content`, `sulfur_content`, `moisture_content`, `volatile_content`, `calorific_value`, `fixed_carbon`, `status`, `create_time`, `update_time`)
VALUES
	(1,1,'B202604001','2026-04-01 08:00:00',18.50,0.50,8.20,31.30,3150.00,42.00,1,'2026-04-18 16:50:55','2026-04-18 16:50:55'),
	(2,2,'B202604002','2026-04-01 08:30:00',20.80,2.50,7.60,29.10,4100.00,42.50,1,'2026-04-18 16:50:55','2026-04-18 16:50:55'),
	(3,3,'B202604003','2026-04-01 09:00:00',16.20,1.60,6.80,27.50,4850.00,49.50,1,'2026-04-18 16:50:55','2026-04-18 16:50:55'),
	(4,4,'B202604004','2026-04-01 09:30:00',12.80,0.80,6.20,24.50,5220.00,56.50,1,'2026-04-18 16:50:55','2026-04-18 16:50:55'),
	(5,5,'B202604005','2026-04-01 10:00:00',11.50,0.30,5.50,18.00,5050.00,65.00,1,'2026-04-18 16:50:55','2026-04-18 16:50:55'),
	(6,6,'B202604006','2026-04-01 10:30:00',9.20,0.45,4.80,28.00,6450.00,58.00,1,'2026-04-18 16:50:55','2026-04-18 16:50:55');

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
	(1,'CT001','朔州4#长焰煤','长焰煤','山西朔州',420.00,'铁路',1,'低硫动力煤样例','2026-04-18 16:50:39','2026-04-18 16:50:39'),
	(2,'CT002','朔州9#长焰煤','长焰煤','山西朔州',360.00,'铁路',1,'高硫动力煤样例','2026-04-18 16:50:39','2026-04-18 16:50:39'),
	(3,'CT003','大同不粘煤','不粘煤','山西大同',460.00,'铁路',1,'中热值动力煤样例','2026-04-18 16:50:39','2026-04-18 16:50:39'),
	(4,'CT004','大同弱粘煤','弱粘煤','山西大同',520.00,'铁路',1,'配焦/高热值样例','2026-04-18 16:50:39','2026-04-18 16:50:39'),
	(5,'CT005','河南二1贫煤','贫煤','河南',500.00,'公路',1,'低硫高热值动力煤样例','2026-04-18 16:50:39','2026-04-18 16:50:39'),
	(6,'CT006','大屯1/3焦煤','1/3焦煤','江苏徐州',780.00,'铁路',1,'高热值低硫焦煤样例','2026-04-18 16:50:39','2026-04-18 16:50:39');

/*!40000 ALTER TABLE `coal_type` ENABLE KEYS */;
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
  PRIMARY KEY (`id`),
  KEY `fk_inventory_coal` (`coal_id`),
  CONSTRAINT `fk_inventory_coal` FOREIGN KEY (`coal_id`) REFERENCES `coal_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存信息表';

LOCK TABLES `inventory` WRITE;
/*!40000 ALTER TABLE `inventory` DISABLE KEYS */;

INSERT INTO `inventory` (`id`, `coal_id`, `warehouse_code`, `stock_quantity`, `available_quantity`, `update_time`, `status`, `remark`)
VALUES
	(1,1,'W001',12000.00,10000.00,'2026-04-10 08:00:00',1,'长焰煤库存充足'),
	(2,2,'W002',9000.00,8500.00,'2026-04-10 08:05:00',1,'高硫煤库存较多'),
	(3,3,'W003',15000.00,13200.00,'2026-04-10 08:10:00',1,'不粘煤主力库存'),
	(4,4,'W004',8000.00,7600.00,'2026-04-10 08:15:00',1,'弱粘煤库存中等'),
	(5,5,'W005',6000.00,5200.00,'2026-04-10 08:20:00',1,'低硫贫煤库存偏紧'),
	(6,6,'W006',3000.00,2600.00,'2026-04-10 08:25:00',1,'焦煤库存较少');

/*!40000 ALTER TABLE `inventory` ENABLE KEYS */;
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
	(1,'O202604001','华东热电厂',5000.00,18.00,0.80,8.50,25.00,5000.00,3,'2026-04-20','pending','低硫动力煤订单','2026-04-18 16:51:12','2026-04-18 16:51:12'),
	(2,'O202604002','北方建材公司',3000.00,20.00,1.50,9.00,28.00,4600.00,2,'2026-04-22','pending','常规动力煤订单','2026-04-18 16:51:12','2026-04-18 16:51:12'),
	(3,'O202604003','某钢铁焦化厂',2000.00,13.00,0.90,7.00,24.00,5300.00,3,'2026-04-18','pending','高热值低灰配煤订单','2026-04-18 16:51:12','2026-04-18 16:51:12'),
	(4,'O202604004','区域供暖中心',4500.00,19.00,1.20,8.80,27.00,4800.00,1,'2026-04-25','pending','供暖季补充订单','2026-04-18 16:51:12','2026-04-18 16:51:12'),
	(5,'O202604005','南方电力公司',3500.00,17.50,0.60,8.00,24.00,5100.00,3,'2026-04-19','pending','高优先级低硫高热值订单','2026-04-18 16:51:12','2026-04-18 16:51:12');

/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `rule_code` (`rule_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='规则知识表';

LOCK TABLES `rule_knowledge` WRITE;
/*!40000 ALTER TABLE `rule_knowledge` DISABLE KEYS */;

INSERT INTO `rule_knowledge` (`id`, `rule_code`, `rule_name`, `rule_type`, `rule_content`, `applicable_scope`, `priority_level`, `status`, `source_desc`, `create_time`, `update_time`)
VALUES
	(1,'R001','高硫煤限配规则','配比约束','当订单硫分上限小于等于0.8%时，高硫煤种配比不得超过10%。','低硫订单',5,1,'测试规则','2026-04-18 16:51:19','2026-04-18 16:51:19'),
	(2,'R002','低硫煤优先规则','经验规则','当目标发热量高于5000且硫分要求严格时，优先调用低硫贫煤和弱粘煤。','高热值低硫订单',4,1,'测试规则','2026-04-18 16:51:19','2026-04-18 16:51:19'),
	(3,'R003','库存保护规则','库存约束','可用库存低于3000吨的煤种仅在高优先级订单中参与配煤。','全部订单',5,1,'测试规则','2026-04-18 16:51:19','2026-04-18 16:51:19'),
	(4,'R004','高热值补偿规则','质量约束','当基础动力煤热值不足时，可引入高热值煤种进行补偿，但成本需同步评估。','热值不足场景',3,1,'测试规则','2026-04-18 16:51:19','2026-04-18 16:51:19'),
	(5,'R005','高灰分限制规则','质量约束','当订单灰分上限小于18%时，灰分高于20%的煤种不得作为主配煤种。','低灰订单',5,1,'测试规则','2026-04-18 16:51:19','2026-04-18 16:51:19');

/*!40000 ALTER TABLE `rule_knowledge` ENABLE KEYS */;
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
	(1,'admin','123456','系统管理员','admin','13800000000','admin@test.com',1,'2026-04-18 16:51:35','2026-04-18 16:51:35');

/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;



/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
