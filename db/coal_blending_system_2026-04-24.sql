# ************************************************************
# Sequel Ace SQL dump
# 版本号： 20094
#
# https://sequel-ace.com/
# https://github.com/Sequel-Ace/Sequel-Ace
#
# 主机: 127.0.0.1 (MySQL 9.3.0)
# 数据库: coal_blending_system
# 生成时间: 2026-04-24 12:32:18 +0000
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `plan_code` (`plan_code`),
  KEY `fk_plan_order` (`order_id`),
  CONSTRAINT `fk_plan_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配煤方案主表';



# 转储表 blend_plan_detail
# ------------------------------------------------------------

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



# 转储表 blend_plan_feedback
# ------------------------------------------------------------

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



# 转储表 case_sample
# ------------------------------------------------------------

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



# 转储表 coal_quality
# ------------------------------------------------------------

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



# 转储表 coal_type
# ------------------------------------------------------------

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



# 转储表 inventory
# ------------------------------------------------------------

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



# 转储表 model_config
# ------------------------------------------------------------

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



# 转储表 rag_knowledge
# ------------------------------------------------------------

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



# 转储表 rag_retrieval_log
# ------------------------------------------------------------

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
  KEY `idx_rag_log_biz` (`biz_type`, `biz_id`),
  KEY `idx_rag_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG检索与生成日志表';



# 转储表 rule_knowledge
# ------------------------------------------------------------

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



# 转储表 sys_user
# ------------------------------------------------------------

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




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
