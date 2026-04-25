# coal_blending_system

本科毕业设计：煤矿智能配煤管理系统后端。

## 大模型解释生成

实现说明见仓库根目录 `docs/系统接入大模型解释生成的实现方案.md`。配煤主流程在 `/blendPlan/generate` 评分完成后调用 **OpenAI Chat Completions 兼容接口**（如 Ollama、国内兼容网关），将结果写入 `blend_plan.explanation`、`risk_tip`、`optimize_suggestion` 等字段。

### 数据库

首次部署请在目标库执行：

- `db/patch_blend_plan_ai_columns.sql`（新增 AI 相关列）
- `db/patch_blend_plan_rule_basis.sql`（第2步：新增 `rule_basis` 规则依据列）
- `db/patch_blend_plan_core_columns.sql`（第1阶段：新增可行性、约束摘要、评分明细、风险等级列）
- `db/patch_blend_plan_feedback.sql`（第2阶段：新增方案执行反馈与案例回流表）
- `db/patch_feedback_case_fk_set_null.sql`（修正反馈回流案例外键，允许案例维护时保留反馈记录）
- `db/patch_rag_phase4_integration.sql`（RAG 第四阶段：新增检索与生成追溯日志表）
- `db/patch_full_chain_data_upgrade.sql`（完整数据链条：矿区来源、原煤生产、洗选加工、产品批次、最终质检、发运交付、批次血缘）
- 可选：`db/seed_llm_model_config_example.sql`（示例模型配置，默认不启用）

### 配置

- 在表 `model_config` 中维护 **`status = 1`** 且 **`api_url` 非空** 的一条记录；`api_url` 为完整地址，例如 `http://127.0.0.1:11434/v1/chat/completions`（Ollama）。
- `application.yml` 中 `coal.llm`：无密钥或离线开发可设环境变量 **`COAL_LLM_ENABLED=false`**，将直接使用兜底文案且不发起 HTTP。

## 知识库（第 1 步落地）

说明见 `docs/第1步知识库落地的详细实现方案.md`。`/blendPlan/generate` 会调用 **规则匹配**、**案例检索**、**知识组装**，在返回体中附带 `matchedRules`（含 `hitReason`）、`matchedCases`（含 `matchReason`/`summary`）、`knowledgeSummary`、`knowledgeContext`（含可拼 Prompt 的 `orderText`/`inventoryText`/`rulesText`/`casesText`）。

第 2 步「知识库与大模型结合」见 `docs/第2步知识库与大模型结合的实现方案.md`：在确定推荐方案后组装 **`planText`** 与方案相关 **库存叙述**，使用 **知识增强四段式 Prompt**（方案说明 / 规则依据 / 风险提示 / 优化建议），解析结果写入 `blend_plan.rule_basis` 等字段。

RAG 第四阶段已接入 `/blendPlan/generate`：系统会根据订单约束从 `rag_knowledge` 检索规则、案例、术语和文档知识，并将 RAG 知识块并入大模型 Prompt。模型被要求严格输出 JSON：`ruleBasis`、`caseReference`、`recommendReason`、`riskTip`、`finalExplanation`。接口返回体新增 `ragRetrieveResult` 和 `ragExplanation`，同时向 `rag_retrieval_log` 写入关键词、命中知识 ID、最终 Prompt 与模型输出，便于方案追溯。

可选执行 `db/seed_knowledge_step1.sql` 补充规则 R006～R012 与案例 C004～C010（按 `rule_code`/`case_code` 幂等更新）。

## 完整数据链条

说明见 `docs/coal_blending_full_chain_upgrade_plan.md`。系统新增 `/mineSource`、`/rawCoalBatch`、`/washProcess`、`/productBatch`、`/finalInspection`、`/shipmentDelivery`、`/trace` 等接口，并在前端新增“全链路数据”页面，支持“矿区来源 → 原煤生产 → 洗选加工 → 产品批次 → 最终质检 → 发运交付 → 批次追溯”的演示链路。
