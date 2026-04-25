-- 补充可用于“产品批次级配煤”的候选物料数据。
-- 适用场景：清空并重导部分业务数据后，生成方案提示“可用候选物料或煤质数据不足”。
--
-- 数据依据说明：
-- 1. 金鸡滩、营盘壕煤质参考公开研报摘录：
--    金鸡滩原煤硫分约0.74%、灰分约7.76%、发热量约6177-6325 kcal/kg；
--    营盘壕煤发热量约29.21 MJ/kg，折合约6988 kcal/kg，特低-低灰、低-中硫。
-- 2. 平朔动力煤质量参考公开港交所文件中平朔五号/六号煤质量指标：
--    平朔五号约4900 kcal/kg、灰分约27%、水分约9%、挥发分约28%；
--    平朔六号约5200 kcal/kg、硫分约1.1%、灰分约21%、水分约11.5%、挥发分约30%。
-- 3. 准东五彩湾煤质参考公开论文试验煤工业分析：
--    M_ad约11.17%、A_ad约6.43%、V_ad约27.91%、S_t,ad约0.51%；
--    另有文献描述五彩湾煤低灰、低硫、中高发热量、高挥发分。
--
-- 注意：
-- - 采购单价、批次号、产量、仓库号为系统演示数据；
-- - 质量指标尽量贴近公开资料，但仍应作为毕设样例数据使用，不代表企业真实化验单；
-- - 本脚本可重复执行，产品批次按 product_batch_no 做幂等更新。

START TRANSACTION;

-- 1. 修复现有仍有可用余量的精煤产品状态。
-- 生成逻辑只筛选 status='available' 的 clean_coal/mixed_product。
UPDATE product_batch
SET status = 'available',
    update_time = NOW(),
    remark = CONCAT(COALESCE(remark, ''), '；余量恢复为可参与配煤候选')
WHERE product_batch_no IN ('PB2026042501-CL', 'PB2026042502-CL')
  AND available_quantity > 0;

-- 2. 补充公开煤质参考下的可用产品批次。
INSERT INTO product_batch
(product_batch_no, wash_batch_id, coal_id, order_id, plan_id, product_type, product_name,
 quantity, available_quantity, warehouse_code,
 ash_content, sulfur_content, moisture_content, volatile_content, calorific_value,
 status, remark, create_time, update_time)
VALUES
('PB-REAL-JJT-20260427-CL', 1, 1, NULL, NULL, 'clean_coal', '金鸡滩低灰低硫精煤补充批次',
 6800.00, 6800.00, 'PROD-WH-JJT-REAL',
 7.80, 0.700, 6.80, 31.50, 6250.00,
 'available', '参考公开资料：金鸡滩煤矿原煤低灰低硫，发热量约6177-6325kcal/kg；用于低硫高热值订单候选。', NOW(), NOW()),
('PB-REAL-YPH-20260427-CL', 1, 3, NULL, NULL, 'clean_coal', '营盘壕高热值精煤补充批次',
 5600.00, 5600.00, 'PROD-WH-YPH-REAL',
 8.90, 0.680, 5.60, 32.80, 6988.00,
 'available', '参考公开资料：营盘壕煤发热量约29.21MJ/kg，折合约6988kcal/kg；用于高热值补偿。', NOW(), NOW()),
('PB-REAL-PS-20260427-CL', 2, 4, NULL, NULL, 'clean_coal', '平朔洗选动力煤补充批次',
 5200.00, 5200.00, 'PROD-WH-PS-REAL',
 21.00, 1.100, 11.50, 30.00, 5200.00,
 'available', '参考平朔动力煤公开指标构造：灰分约21%、硫分约1.1%、水分约11.5%、发热量约5200kcal/kg；适合宽硫成本型订单。', NOW(), NOW()),
('PB-REAL-WCW-20260427-CL', 3, 5, NULL, NULL, 'clean_coal', '准东五彩湾低硫动力煤补充批次',
 6200.00, 6200.00, 'PROD-WH-WCW-REAL',
 6.43, 0.510, 11.17, 27.91, 4850.00,
 'available', '参考准东五彩湾煤公开工业分析：低灰、低硫、水分较高；用于低成本候选并触发水分风险提示。', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  wash_batch_id = VALUES(wash_batch_id),
  coal_id = VALUES(coal_id),
  product_type = VALUES(product_type),
  product_name = VALUES(product_name),
  quantity = VALUES(quantity),
  available_quantity = VALUES(available_quantity),
  warehouse_code = VALUES(warehouse_code),
  ash_content = VALUES(ash_content),
  sulfur_content = VALUES(sulfur_content),
  moisture_content = VALUES(moisture_content),
  volatile_content = VALUES(volatile_content),
  calorific_value = VALUES(calorific_value),
  status = 'available',
  remark = VALUES(remark),
  update_time = NOW();

-- 3. 补充产品批次库存记录。
INSERT INTO inventory
(coal_id, warehouse_code, stock_quantity, available_quantity, update_time, status, remark,
 material_stage, raw_batch_no, product_batch_no, locked_quantity)
SELECT 1, 'PROD-WH-JJT-REAL', 6800.00, 6800.00, NOW(), 1,
       '金鸡滩低灰低硫精煤补充库存', 'product_batch', NULL, 'PB-REAL-JJT-20260427-CL', 0.00
WHERE NOT EXISTS (SELECT 1 FROM inventory WHERE product_batch_no = 'PB-REAL-JJT-20260427-CL');

INSERT INTO inventory
(coal_id, warehouse_code, stock_quantity, available_quantity, update_time, status, remark,
 material_stage, raw_batch_no, product_batch_no, locked_quantity)
SELECT 3, 'PROD-WH-YPH-REAL', 5600.00, 5600.00, NOW(), 1,
       '营盘壕高热值精煤补充库存', 'product_batch', NULL, 'PB-REAL-YPH-20260427-CL', 0.00
WHERE NOT EXISTS (SELECT 1 FROM inventory WHERE product_batch_no = 'PB-REAL-YPH-20260427-CL');

INSERT INTO inventory
(coal_id, warehouse_code, stock_quantity, available_quantity, update_time, status, remark,
 material_stage, raw_batch_no, product_batch_no, locked_quantity)
SELECT 4, 'PROD-WH-PS-REAL', 5200.00, 5200.00, NOW(), 1,
       '平朔洗选动力煤补充库存', 'product_batch', NULL, 'PB-REAL-PS-20260427-CL', 0.00
WHERE NOT EXISTS (SELECT 1 FROM inventory WHERE product_batch_no = 'PB-REAL-PS-20260427-CL');

INSERT INTO inventory
(coal_id, warehouse_code, stock_quantity, available_quantity, update_time, status, remark,
 material_stage, raw_batch_no, product_batch_no, locked_quantity)
SELECT 5, 'PROD-WH-WCW-REAL', 6200.00, 6200.00, NOW(), 1,
       '准东五彩湾低硫动力煤补充库存', 'product_batch', NULL, 'PB-REAL-WCW-20260427-CL', 0.00
WHERE NOT EXISTS (SELECT 1 FROM inventory WHERE product_batch_no = 'PB-REAL-WCW-20260427-CL');

-- 若库存记录已存在但被置为不可用，同步恢复。
UPDATE inventory
SET status = 1,
    available_quantity = CASE product_batch_no
        WHEN 'PB-REAL-JJT-20260427-CL' THEN 6800.00
        WHEN 'PB-REAL-YPH-20260427-CL' THEN 5600.00
        WHEN 'PB-REAL-PS-20260427-CL' THEN 5200.00
        WHEN 'PB-REAL-WCW-20260427-CL' THEN 6200.00
        ELSE available_quantity
    END,
    locked_quantity = 0.00,
    update_time = NOW()
WHERE product_batch_no IN (
    'PB-REAL-JJT-20260427-CL',
    'PB-REAL-YPH-20260427-CL',
    'PB-REAL-PS-20260427-CL',
    'PB-REAL-WCW-20260427-CL'
);

-- 4. 补充产品煤质检测记录，保证煤质数据可追溯。
INSERT INTO coal_quality
(coal_id, batch_no, sample_stage, related_batch_no, sample_time, sample_point,
 ash_content, sulfur_content, moisture_content, volatile_content, calorific_value, fixed_carbon,
 status, create_time, update_time, report_no, standard_basis)
SELECT 1, 'CL-PB-REAL-JJT-20260427', 'clean_coal', 'PB-REAL-JJT-20260427-CL', NOW(), 'PROD-WH-JJT-REAL 产品仓采样点',
       7.80, 0.70, 6.80, 31.50, 6250.00, 53.20,
       1, NOW(), NOW(), 'LAB-REAL-20260427-JJT-CL', '公开煤质资料参考 + GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'
WHERE NOT EXISTS (SELECT 1 FROM coal_quality WHERE report_no = 'LAB-REAL-20260427-JJT-CL');

INSERT INTO coal_quality
(coal_id, batch_no, sample_stage, related_batch_no, sample_time, sample_point,
 ash_content, sulfur_content, moisture_content, volatile_content, calorific_value, fixed_carbon,
 status, create_time, update_time, report_no, standard_basis)
SELECT 3, 'CL-PB-REAL-YPH-20260427', 'clean_coal', 'PB-REAL-YPH-20260427-CL', NOW(), 'PROD-WH-YPH-REAL 产品仓采样点',
       8.90, 0.68, 5.60, 32.80, 6988.00, 52.70,
       1, NOW(), NOW(), 'LAB-REAL-20260427-YPH-CL', '公开煤质资料参考 + GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'
WHERE NOT EXISTS (SELECT 1 FROM coal_quality WHERE report_no = 'LAB-REAL-20260427-YPH-CL');

INSERT INTO coal_quality
(coal_id, batch_no, sample_stage, related_batch_no, sample_time, sample_point,
 ash_content, sulfur_content, moisture_content, volatile_content, calorific_value, fixed_carbon,
 status, create_time, update_time, report_no, standard_basis)
SELECT 4, 'CL-PB-REAL-PS-20260427', 'clean_coal', 'PB-REAL-PS-20260427-CL', NOW(), 'PROD-WH-PS-REAL 产品仓采样点',
       21.00, 1.10, 11.50, 30.00, 5200.00, 43.00,
       1, NOW(), NOW(), 'LAB-REAL-20260427-PS-CL', '公开煤质资料参考 + GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'
WHERE NOT EXISTS (SELECT 1 FROM coal_quality WHERE report_no = 'LAB-REAL-20260427-PS-CL');

INSERT INTO coal_quality
(coal_id, batch_no, sample_stage, related_batch_no, sample_time, sample_point,
 ash_content, sulfur_content, moisture_content, volatile_content, calorific_value, fixed_carbon,
 status, create_time, update_time, report_no, standard_basis)
SELECT 5, 'CL-PB-REAL-WCW-20260427', 'clean_coal', 'PB-REAL-WCW-20260427-CL', NOW(), 'PROD-WH-WCW-REAL 产品仓采样点',
       6.43, 0.51, 11.17, 27.91, 4850.00, 54.49,
       1, NOW(), NOW(), 'LAB-REAL-20260427-WCW-CL', '公开煤质资料参考 + GB/T 19494.1 采样、GB/T 474 制样、GB/T 212 工业分析'
WHERE NOT EXISTS (SELECT 1 FROM coal_quality WHERE report_no = 'LAB-REAL-20260427-WCW-CL');

-- 5. 补充批次血缘，保证推荐方案可解释“煤从哪里来、经过了什么过程”。
INSERT INTO batch_lineage
(parent_batch_no, parent_batch_type, child_batch_no, child_batch_type, process_stage,
 quantity, ratio, operation_time, operator_name, remark, create_time)
SELECT 'WP2026042501', 'wash_batch', 'PB-REAL-JJT-20260427-CL', 'product_batch', 'wash_to_product',
       6800.00, 0.7200, NOW(), '系统补数', '金鸡滩低灰低硫精煤补充批次，来源于低硫高热值洗选链路', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM batch_lineage
    WHERE parent_batch_no = 'WP2026042501' AND child_batch_no = 'PB-REAL-JJT-20260427-CL'
);

INSERT INTO batch_lineage
(parent_batch_no, parent_batch_type, child_batch_no, child_batch_type, process_stage,
 quantity, ratio, operation_time, operator_name, remark, create_time)
SELECT 'WP2026042501', 'wash_batch', 'PB-REAL-YPH-20260427-CL', 'product_batch', 'wash_to_product',
       5600.00, 0.6800, NOW(), '系统补数', '营盘壕高热值精煤补充批次，作为高热值补偿候选', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM batch_lineage
    WHERE parent_batch_no = 'WP2026042501' AND child_batch_no = 'PB-REAL-YPH-20260427-CL'
);

INSERT INTO batch_lineage
(parent_batch_no, parent_batch_type, child_batch_no, child_batch_type, process_stage,
 quantity, ratio, operation_time, operator_name, remark, create_time)
SELECT 'WP2026042502', 'wash_batch', 'PB-REAL-PS-20260427-CL', 'product_batch', 'wash_to_product',
       5200.00, 0.5800, NOW(), '系统补数', '平朔高灰原煤经洗选后形成动力煤产品批次', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM batch_lineage
    WHERE parent_batch_no = 'WP2026042502' AND child_batch_no = 'PB-REAL-PS-20260427-CL'
);

INSERT INTO batch_lineage
(parent_batch_no, parent_batch_type, child_batch_no, child_batch_type, process_stage,
 quantity, ratio, operation_time, operator_name, remark, create_time)
SELECT 'WP2026042601', 'wash_batch', 'PB-REAL-WCW-20260427-CL', 'product_batch', 'wash_to_product',
       6200.00, 0.8000, NOW(), '系统补数', '准东五彩湾低硫动力煤补充批次，水分较高需在配煤中约束', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM batch_lineage
    WHERE parent_batch_no = 'WP2026042601' AND child_batch_no = 'PB-REAL-WCW-20260427-CL'
);

COMMIT;

-- 6. 执行后验证：至少应返回 4 条以上 clean_coal/mixed_product 可用候选。
SELECT
  pb.product_batch_no,
  pb.product_name,
  pb.coal_id,
  ct.coal_name,
  pb.product_type,
  pb.status,
  pb.available_quantity,
  pb.ash_content,
  pb.sulfur_content,
  pb.moisture_content,
  pb.calorific_value
FROM product_batch pb
JOIN coal_type ct ON ct.id = pb.coal_id
WHERE pb.status = 'available'
  AND pb.available_quantity > 0
  AND pb.product_type IN ('clean_coal', 'mixed_product')
  AND ct.blendable_flag = 1
ORDER BY pb.available_quantity DESC;

