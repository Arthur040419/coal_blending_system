ALTER TABLE `blend_plan`
  ADD COLUMN `candidate_materials_json` json DEFAULT NULL COMMENT '本次生成候选物料短名单快照' AFTER `generation_config_json`,
  ADD COLUMN `matched_rules_json` json DEFAULT NULL COMMENT '本次生成命中规则快照' AFTER `candidate_materials_json`,
  ADD COLUMN `matched_cases_json` json DEFAULT NULL COMMENT '本次生成参考案例快照' AFTER `matched_rules_json`,
  ADD COLUMN `rag_retrieve_result_json` json DEFAULT NULL COMMENT '本次生成RAG检索结果快照' AFTER `matched_cases_json`;
