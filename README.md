# 煤矿智能配煤管理系统 · 后端

本科毕业设计“基于大模型的煤矿智能配煤系统设计与实现”的后端服务。项目基于 Spring Boot 3、MyBatis-Plus、MySQL 8 实现，提供订单、煤种、煤质、库存、智能配煤、知识库、RAG、模型配置、方案追溯、模型效果评估和全链路批次数据接口。

## 系统组成

本仓库是后端项目，建议与以下两个兄弟项目配合使用：

- `../coal_blending_system_frontend`：Vue 3 + Vite + Element Plus 管理端。
- `../coal_blending_system_model_tuning`：模型调优实验项目，用于构建训练数据、LoRA/QLoRA 微调和模型效果对比。

核心能力：

- 基础数据管理：用户、订单、煤种、煤质、库存。
- 智能配煤：根据订单约束、库存、规则、案例和 RAG 知识生成候选方案并评分。
- 方案解释：接入 OpenAI Chat Completions 兼容接口，生成规则依据、案例参考、推荐理由、风险提示和最终解释。
- RAG 知识库：支持规则、案例、文档知识检索，并记录检索与生成追溯日志。
- 全链路数据：覆盖矿区来源、原煤生产、洗选加工、产品批次、最终质检、发运交付和批次追溯。
- 模型效果评估：记录同一订单在不同模型下的候选质量、成本、库存可执行性和综合效果。

## 环境要求

- JDK 17
- Maven 3.8+
- MySQL 8.x
- 可选：Ollama 或其他 OpenAI Chat Completions 兼容服务
- 可选：Qdrant，用于向量检索；未部署时可关闭向量检索或使用哈希向量兜底

## 数据库初始化

创建数据库：

```sql
CREATE DATABASE coal_blending_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

导入主数据快照：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p coal_blending_system \
  < db/coal_blending_system_2026-06-07.sql
```

默认演示账号在主 SQL 快照中已经包含：


| 账号         | 密码       | 角色    |
| ---------- | -------- | ----- |
| `admin`    | `123456` | 管理员   |
| `operator` | `123456` | 配煤业务员 |
| `quality`  | `123456` | 煤质检测员 |


## 配置

主要配置位于 `src/main/resources/application.yml`，常用环境变量如下：


| 变量                                     | 默认值                                | 说明                  |
| -------------------------------------- | ---------------------------------- | ------------------- |
| `DB_HOST`                              | `127.0.0.1`                        | MySQL 主机            |
| `DB_PORT`                              | `3306`                             | MySQL 端口            |
| `DB_NAME`                              | `coal_blending_system`             | 数据库名                |
| `DB_USER`                              | `root`                             | 数据库账号               |
| `DB_PASSWORD`                          | 项目内默认值                             | 数据库密码，部署时建议通过环境变量覆盖 |
| `SERVER_PORT`                          | `8080`                             | 后端服务端口              |
| `COAL_LLM_ENABLED`                     | `true`                             | 是否发起大模型 HTTP 请求     |
| `COAL_BLEND_ENABLE_SYSTEM_ENUMERATION` | `false`                            | 是否启用系统枚举候选方案        |
| `COAL_RAG_VECTOR_ENABLED`              | `true`                             | 是否启用 RAG 向量检索       |
| `COAL_RAG_QDRANT_URL`                  | `http://127.0.0.1:6333`            | Qdrant 地址           |
| `COAL_RAG_EMBEDDING_API_URL`           | `http://127.0.0.1:11434/api/embed` | Embedding 接口        |
| `COAL_RAG_EMBEDDING_MODEL`             | `bge-m3`                           | Embedding 模型        |


离线开发或暂时不接模型时，可以关闭大模型调用：

```bash
export COAL_LLM_ENABLED=false
```

## 启动后端

在本目录执行：

```bash
mvn spring-boot:run
```

或先打包再运行：

```bash
mvn clean package
java -jar target/coal-blending-backend-0.0.1-SNAPSHOT.jar
```

启动后访问：

- 健康检查：`http://127.0.0.1:8080/health`
- Swagger UI：`http://127.0.0.1:8080/swagger-ui.html`

前端开发环境默认通过 `/api` 代理到 `http://127.0.0.1:8080`。

## Docker Compose 部署

如果需要一次性启动 MySQL、后端、前端和 Qdrant，可以在部署目录准备如下结构：

```text
deploy/
├── docker-compose.yml
├── backend/              后端项目，对应本仓库内容
│   ├── Dockerfile
│   └── db/
│       └── coal_blending_system_2026-05-11.sql
└── frontend/             前端项目，对应 coal_blending_system_frontend
    └── Dockerfile
```

`docker-compose.yml` 示例：

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: coal-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: coal_blending_system
      TZ: Asia/Shanghai
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_0900_ai_ci
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./backend/db/coal_blending_system_2026-06-07.sql:/docker-entrypoint-initdb.d/init.sql:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-uroot", "-proot"]
      interval: 10s
      timeout: 5s
      retries: 10

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: coal-backend
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      TZ: Asia/Shanghai
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/coal_blending_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root
      COAL_LLM_ENABLED: "true"
      COAL_RAG_VECTOR_ENABLED: "true"
      COAL_RAG_QDRANT_URL: http://qdrant:6333
      COAL_RAG_COLLECTION: coal_rag_chunks
      COAL_RAG_EMBEDDING_API_URL: http://host.docker.internal:11434/api/embed
      COAL_RAG_EMBEDDING_MODEL: bge-m3
      COAL_RAG_VECTOR_SIZE: "1024"
      COAL_RAG_FALLBACK_HASH_EMBEDDING: "true"
    ports:
      - "8080:8080"

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: coal-frontend
    restart: unless-stopped
    depends_on:
      - backend
    ports:
      - "80:80"

  qdrant:
    image: qdrant/qdrant:latest
    container_name: coal-qdrant
    restart: unless-stopped
    ports:
      - "6333:6333"
    volumes:
      - qdrant-data:/qdrant/storage

volumes:
  mysql-data:
  qdrant-data:
```

启动：

```bash
docker compose up -d --build
```

查看日志：

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

访问地址：

- 前端：`http://127.0.0.1`
- 后端：`http://127.0.0.1:8080`
- Swagger UI：`http://127.0.0.1:8080/swagger-ui.html`
- Qdrant：`http://127.0.0.1:6333`

注意事项：

- 上面示例假设 Compose 文件和 `backend/`、`frontend/` 在同一级目录。如果把 Compose 文件直接放在本后端仓库根目录，需要把 SQL 挂载路径改为 `./db/coal_blending_system_2026-05-11.sql:/docker-entrypoint-initdb.d/init.sql:ro`，后端构建上下文也要相应调整。
- MySQL 只会在 `mysql-data` 数据卷首次创建时执行 `/docker-entrypoint-initdb.d/init.sql`。如果已经启动过旧数据卷，修改 SQL 文件不会自动重新导入，需要先备份数据后再处理数据卷。
- 后端容器访问 Compose 内的 MySQL 使用服务名 `mysql`，访问 Qdrant 使用服务名 `qdrant`。
- 示例中 Embedding 仍访问宿主机 Ollama：`http://host.docker.internal:11434/api/embed`。Linux 环境通过 `extra_hosts: host.docker.internal:host-gateway` 支持该地址。

## 大模型接入

系统从 `model_config` 表读取启用的模型配置。需要满足：

- `status = 1`
- `api_url` 非空
- 接口兼容 OpenAI Chat Completions，示例：`http://127.0.0.1:11434/v1/chat/completions`

可直接在前端“模型配置”页面维护，也可以执行 `db/seed_llm_model_config_example.sql` 后再修改。常见配置：

```text
model_name: qwen3:4b
api_url: http://127.0.0.1:11434/v1/chat/completions
api_key: 可为空，按服务要求填写
status: 启用
```

如果后端运行在 Docker 容器内，而 Ollama 运行在宿主机上，不要把 `api_url` 写成 `127.0.0.1` 或 `localhost`。可改为：

- Docker Desktop：`http://host.docker.internal:11434/v1/chat/completions`
- Linux Docker 默认网桥：通常为 `http://172.17.0.1:11434/v1/chat/completions`
- 同一 Compose 网络：`http://ollama:11434/v1/chat/completions`

Ollama 需要允许后端访问，例如设置：

```bash
OLLAMA_HOST=0.0.0.0:11434 ollama serve
```

日志中出现 `Calling LLM candidate endpoint` 或 `Calling LLM explanation endpoint` 才表示已经发起模型请求；如果只看到 `Skip ... LLM call`，优先检查 `COAL_LLM_ENABLED`、`model_config.status`、`model_config.api_url` 和模型类型。

## RAG 知识库

RAG 已接入 `/blendPlan/generate`。生成方案时，系统会根据订单约束检索规则、案例、术语和文档知识，并将知识块合入 Prompt。模型输出要求为 JSON：

```json
{
  "ruleBasis": "...",
  "caseReference": "...",
  "recommendReason": "...",
  "riskTip": "...",
  "finalExplanation": "..."
}
```

接口返回会包含 `ragRetrieveResult` 和 `ragExplanation`，同时写入 `rag_retrieval_log`，便于追溯关键词、命中知识、最终 Prompt 与模型输出。

常用接口：

```text
GET  /rag/health
GET  /rag/documents
GET  /rag/chunks
POST /rag/ingest/all
POST /rag/ingest/knowledge/{id}
POST /rag/ingest/rule/{id}
POST /rag/ingest/case/{id}
GET  /rag/retrieve/order/{orderId}
```

如果不部署 Qdrant，可设置：

```bash
export COAL_RAG_VECTOR_ENABLED=false
```

此时仍可使用数据库关键词和业务规则检索。

## 系统使用流程

推荐按下面顺序演示系统：

1. 登录前端：使用 `admin / 123456` 进入管理端。
2. 维护基础数据：在“煤种管理”“煤质管理”“库存管理”中确认煤种质量、价格、库存和产品批次可用。
3. 维护订单：在“订单管理”新增或选择一条待配煤订单，填写需求吨数、灰分、硫分、水分、挥发分、发热量和交付日期。
4. 维护知识：在“规则知识”“历史案例”“RAG 知识库”中确认可用规则、案例和知识块。
5. 配置模型：在“模型配置”中启用一个 OpenAI 兼容模型；离线演示可关闭 `COAL_LLM_ENABLED` 使用系统兜底说明。
6. 生成方案：进入“智能配煤”，选择订单并生成配煤方案。系统会调用候选生成、约束校验、评分排序、知识检索和解释生成流程。
7. 选择方案：查看各方案的预测煤质、成本、库存风险、综合评分、规则依据和风险提示，选择推荐方案。
8. 执行方案：执行后会扣减库存，并可生成最终产品批次和追溯数据。
9. 反馈回流：在方案反馈中录入实际灰分、硫分、水分、热值、成本和合格结果，可转换为历史案例。
10. 查看效果：在“方案追溯”“模型效果”“全链路数据”中查看方案历史、雷达图指标和批次上下游链路。

## 主要接口


| 模块   | 接口前缀                                                                                                         | 说明             |
| ---- | ------------------------------------------------------------------------------------------------------------ | -------------- |
| 认证   | `/auth`                                                                                                      | 登录、当前用户        |
| 用户   | `/user`                                                                                                      | 用户增删改查和状态维护    |
| 订单   | `/order`                                                                                                     | 订单分页、详情、增删改和状态 |
| 煤种   | `/coalType`                                                                                                  | 煤种基础资料         |
| 煤质   | `/coalQuality`                                                                                               | 化验指标和最新煤质      |
| 库存   | `/inventory`                                                                                                 | 库存、可用库存        |
| 智能配煤 | `/blendPlan`                                                                                                 | 生成、查询、选择、执行方案  |
| 方案反馈 | `/blendPlanFeedback`                                                                                         | 执行反馈和案例回流      |
| 规则知识 | `/ruleKnowledge`                                                                                             | 规则维护与启用规则      |
| 历史案例 | `/caseSample`                                                                                                | 案例维护和检索        |
| RAG  | `/rag`                                                                                                       | 知识入库、检索和健康检查   |
| 模型配置 | `/modelConfig`                                                                                               | 模型接口配置         |
| 模型效果 | `/experimentRecord`                                                                                          | 实验记录、雷达图和模型效果  |
| 全链路  | `/mineSource`、`/rawCoalBatch`、`/washProcess`、`/productBatch`、`/finalInspection`、`/shipmentDelivery`、`/trace` | 批次生产、质检、发运和追溯  |


前端请求普通业务接口时会自动携带 `X-User-Id` 请求头。直接调试接口时，除 `/auth/login` 外也建议带上：

```text
X-User-Id: 1
```

## 智能配煤生成逻辑

`POST /blendPlan/generate` 是核心接口。处理过程：

1. 读取订单、可用库存、产品批次、煤质快照。
2. 根据配置决定是否调用大模型生成候选方案。
3. 对候选方案做比例、库存、质量硬约束校验。
4. 计算质量、成本、库存稳定性、风险控制和综合评分。
5. 做 Pareto 排序与推荐模式判断。
6. 检索规则、案例和 RAG 知识，组装知识增强 Prompt。
7. 调用大模型生成解释，或在离线模式下写入兜底说明。
8. 保存 `blend_plan`、`blend_plan_detail`、`experiment_record` 和 `rag_retrieval_log`。

生成接口支持传入相同的 `experimentCode`，用于比较不同模型在同一订单上的效果。

## 常见问题

**后端连不上数据库**

检查 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`，并确认 MySQL 已导入主 SQL 快照。

**前端登录后接口返回 401**

确认前端本地存储中的用户信息未损坏，或退出后重新登录。直接调接口时需要携带 `X-User-Id`。

**模型没有被调用**

检查 `COAL_LLM_ENABLED=true`，并确认 `model_config` 中启用记录的 `api_url` 不为空。Docker 内访问宿主机 Ollama 时不要使用 `127.0.0.1`。

**RAG 检索没有向量结果**

确认 Qdrant 地址可访问、Embedding 服务可访问，并执行过 `/rag/ingest/all`。如果只是演示流程，可以关闭 `COAL_RAG_VECTOR_ENABLED` 使用关键词兜底。

## 相关文档

- `../docs/系统接入大模型解释生成的实现方案.md`
- `../docs/第1步知识库落地的详细实现方案.md`
- `../docs/第2步知识库与大模型结合的实现方案.md`
- `../docs/coal_blending_full_chain_upgrade_plan.md`

