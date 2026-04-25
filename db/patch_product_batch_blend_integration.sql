-- 产品批次接入配煤生成：为 product_batch 增加 coal_id，兼容现有 blend_plan_detail.coal_id 外键。
ALTER TABLE `product_batch`
  ADD COLUMN `coal_id` bigint DEFAULT NULL COMMENT '关联煤种ID，用于兼容煤种级配煤' AFTER `wash_batch_id`,
  ADD KEY `idx_product_coal_id` (`coal_id`);
