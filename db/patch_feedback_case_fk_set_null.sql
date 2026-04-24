-- 修正第2阶段反馈表与案例表的外键行为：
-- 删除案例时保留反馈记录，将 feedback.case_id 自动置空，避免外键阻止案例维护。
ALTER TABLE `blend_plan_feedback`
  DROP FOREIGN KEY `fk_feedback_case`;

ALTER TABLE `blend_plan_feedback`
  ADD CONSTRAINT `fk_feedback_case`
  FOREIGN KEY (`case_id`) REFERENCES `case_sample` (`id`)
  ON DELETE SET NULL;
