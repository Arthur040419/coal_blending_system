-- 完整数据链条升级：矿区来源 -> 原煤生产 -> 洗选加工 -> 产品批次 -> 最终质检 -> 发运交付 -> 批次血缘。

CREATE TABLE IF NOT EXISTS `mine_source` (
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

CREATE TABLE IF NOT EXISTS `raw_coal_batch` (
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

CREATE TABLE IF NOT EXISTS `wash_process_batch` (
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
  KEY `idx_wash_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='洗选加工批次表';

CREATE TABLE IF NOT EXISTS `wash_input_detail` (
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

CREATE TABLE IF NOT EXISTS `product_batch` (
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
  KEY `idx_product_coal_id` (`coal_id`),
  KEY `idx_product_order_id` (`order_id`),
  KEY `idx_product_plan_id` (`plan_id`),
  KEY `idx_product_type` (`product_type`),
  KEY `idx_product_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品批次表';

CREATE TABLE IF NOT EXISTS `final_product_inspection` (
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

CREATE TABLE IF NOT EXISTS `shipment_delivery` (
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

CREATE TABLE IF NOT EXISTS `batch_lineage` (
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

ALTER TABLE `coal_quality`
  ADD COLUMN `sample_stage` varchar(50) DEFAULT 'coal_type' COMMENT '采样阶段：geological/raw_coal/wash_feed/clean_coal/mixed_product/final_product' AFTER `batch_no`,
  ADD COLUMN `related_batch_no` varchar(64) DEFAULT NULL COMMENT '关联批次号' AFTER `sample_stage`,
  ADD COLUMN `sample_point` varchar(100) DEFAULT NULL COMMENT '采样点' AFTER `sample_time`,
  ADD COLUMN `report_no` varchar(64) DEFAULT NULL COMMENT '化验报告号',
  ADD COLUMN `standard_basis` varchar(200) DEFAULT NULL COMMENT '采样/制样/化验依据';

ALTER TABLE `inventory`
  ADD COLUMN `material_stage` varchar(50) DEFAULT 'coal_type' COMMENT '物料阶段：raw_coal/product_batch/final_product/coal_type',
  ADD COLUMN `raw_batch_no` varchar(64) DEFAULT NULL COMMENT '原煤批次号',
  ADD COLUMN `product_batch_no` varchar(64) DEFAULT NULL COMMENT '产品批次号',
  ADD COLUMN `locked_quantity` decimal(12,2) DEFAULT '0.00' COMMENT '锁定量';

ALTER TABLE `blend_plan`
  ADD COLUMN `final_product_batch_no` varchar(64) DEFAULT NULL COMMENT '最终产品批次号',
  ADD COLUMN `trace_status` varchar(30) DEFAULT 'not_executed' COMMENT '追溯状态：not_executed/executed/inspected/shipped';

ALTER TABLE `blend_plan_detail`
  ADD COLUMN `product_batch_id` bigint DEFAULT NULL COMMENT '产品批次ID',
  ADD COLUMN `product_batch_no` varchar(64) DEFAULT NULL COMMENT '产品批次号',
  ADD COLUMN `inventory_id` bigint DEFAULT NULL COMMENT '库存ID',
  ADD COLUMN `quality_snapshot_json` json DEFAULT NULL COMMENT '生成方案时的煤质快照';

ALTER TABLE `rule_knowledge`
  ADD COLUMN `business_stage` varchar(50) DEFAULT 'blending' COMMENT '业务阶段',
  ADD COLUMN `quality_indicator` varchar(200) DEFAULT NULL COMMENT '相关质量指标',
  ADD COLUMN `material_type` varchar(50) DEFAULT NULL COMMENT '适用物料类型';

ALTER TABLE `case_sample`
  ADD COLUMN `business_stage` varchar(50) DEFAULT 'blending' COMMENT '案例业务阶段',
  ADD COLUMN `related_batch_no` varchar(64) DEFAULT NULL COMMENT '关联批次号',
  ADD COLUMN `related_order_id` bigint DEFAULT NULL COMMENT '关联订单ID';
