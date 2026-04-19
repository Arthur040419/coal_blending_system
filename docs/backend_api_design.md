# 煤矿智能配煤管理系统 — 后端 REST 接口设计说明

本文档依据《后端需求文档》《后端技术实现方案》以及数据库脚本 `db/coal_blending_system_2026-04-18.sql` 中的表结构，对除已实现示例外**其余业务接口**进行统一设计，便于前后端联调与分阶段实现。

**约定**：除特别说明外，下文「返回示例」均为 HTTP 200 且业务成功时的**完整响应体**（含 `code`、`message`、`data`）。失败时 `code` 非 200，`message` 为原因说明，`data` 多为 `null`。

---

## 1. 设计约定

### 1.1 基础路径与版本

- 建议服务根路径：`http://{host}:{port}`（默认端口以 `application.yml` 为准，如 `8080`）。
- 本文档路径均为**相对路径**；若后续增加全局前缀（如 `/api`），在网关或 `context-path` 中统一配置即可。

### 1.2 统一响应结构

与需求文档一致，所有业务接口返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| `code` 取值（建议） | 含义 |
|---------------------|------|
| 200 | 成功 |
| 400 | 参数错误、业务校验失败 |
| 401 | 未登录或登录失效（接入 JWT 后使用） |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 系统异常 |

**失败返回示例**（结构一致，仅 `code`/`message`/`data` 变化）：

```json
{
  "code": 400,
  "message": "用户名或密码错误",
  "data": null
}
```

### 1.3 分页规范

与技术方案一致，列表类查询统一使用 **Query** 参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `current` | long | 否 | 当前页，默认 `1` |
| `size` | long | 否 | 每页条数，默认 `10` |

**分页 `data` 建议结构**（与 MyBatis-Plus `IPage` 对齐）：

```json
{
  "records": [],
  "total": 100,
  "size": 10,
  "current": 1,
  "pages": 10
}
```

### 1.4 REST 与动词约定

| 操作 | HTTP 方法 | URL 模式 |
|------|-----------|----------|
| 分页列表 | `GET` | `/{resource}/page` |
| 详情 | `GET` | `/{resource}/detail/{id}` |
| 新增 | `POST` | `/{resource}/add` |
| 修改 | `PUT` | `/{resource}/update` |
| 删除 | `DELETE` | `/{resource}/delete/{id}` |

新增、修改请求体统一使用 **`Content-Type: application/json`**。

### 1.5 认证（毕设简化阶段）

**阶段 A**：`POST /auth/login`；`GET /auth/me` 可配合请求头 `X-User-Id: 1`（联调用）。

**阶段 B**：`Authorization: Bearer {jwt}`。

### 1.6 与数据表的对应关系

| 业务模块 | 主表 |
|----------|------|
| 用户与认证 | `sys_user` |
| 煤种 | `coal_type` |
| 煤质 | `coal_quality` |
| 库存 | `inventory` |
| 订单 | `orders` |
| 规则知识 | `rule_knowledge` |
| 历史案例 | `case_sample` |
| 配煤方案 | `blend_plan`、`blend_plan_detail` |
| 模型配置 | `model_config` |

### 1.7 通用返回说明

- **仅状态类操作成功**（如删除、更新状态）：`data` 可为 `null` 或 `true`，实现时二选一并在 Swagger 固定。
- **日期时间**：JSON 中日期建议 `yyyy-MM-dd`，日期时间建议 `yyyy-MM-dd'T'HH:mm:ss` 或 `yyyy-MM-dd HH:mm:ss`（与前端约定一致即可）。

---

## 2. 健康检查与煤种分页（已实现）

### 2.1 健康检查

**请求示例**

```http
GET /health HTTP/1.1
Host: localhost:8080
```

```bash
curl -s "http://localhost:8080/health"
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UP",
    "application": "coal-blending-backend"
  }
}
```

### 2.2 煤种分页

**请求示例**

```http
GET /coalType/page?current=1&size=10&keyword=长焰 HTTP/1.1
Host: localhost:8080
```

```bash
curl -s "http://localhost:8080/coalType/page?current=1&size=10&keyword=长焰"
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "coalCode": "CT001",
        "coalName": "朔州4#长焰煤",
        "coalCategory": "长焰煤",
        "sourceArea": "山西朔州",
        "purchasePrice": 420.0,
        "transportMode": "铁路",
        "blendableFlag": 1,
        "remark": "低硫动力煤样例",
        "createTime": "2026-04-18T16:50:39",
        "updateTime": "2026-04-18T16:50:39"
      }
    ],
    "total": 6,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

## 3. 用户与认证模块（`sys_user`）

### 3.1 登录 `POST /auth/login`

**请求示例**

```http
POST /auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"username":"admin","password":"123456"}
```

```bash
curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

**返回示例（成功）**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "系统管理员",
    "role": "admin",
    "phone": "13800000000",
    "email": "admin@test.com",
    "status": 1
  }
}
```

### 3.2 获取当前用户 `GET /auth/me`

**请求示例**（阶段 A：带头）

```http
GET /auth/me HTTP/1.1
Host: localhost:8080
X-User-Id: 1
```

```bash
curl -s "http://localhost:8080/auth/me" -H "X-User-Id: 1"
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "系统管理员",
    "role": "admin",
    "phone": "13800000000",
    "email": "admin@test.com",
    "status": 1
  }
}
```

### 3.3 用户分页 `GET /user/page`

**请求示例**

```bash
curl -s "http://localhost:8080/user/page?current=1&size=10&keyword=admin&role=admin&status=1"
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "username": "admin",
        "realName": "系统管理员",
        "role": "admin",
        "phone": "13800000000",
        "email": "admin@test.com",
        "status": 1,
        "createTime": "2026-04-18T16:51:35",
        "updateTime": "2026-04-18T16:51:35"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 3.4 新增用户 `POST /user/add`

**请求示例**

```json
{
  "username": "zhangsan",
  "password": "123456",
  "realName": "张三",
  "role": "user",
  "phone": "13900000001",
  "email": "zhangsan@example.com",
  "status": 1
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "username": "zhangsan",
    "realName": "张三",
    "role": "user",
    "phone": "13900000001",
    "email": "zhangsan@example.com",
    "status": 1,
    "createTime": "2026-04-19T10:00:00",
    "updateTime": "2026-04-19T10:00:00"
  }
}
```

（实现也可仅返回 `id` 或 `true`，需在接口文档中固定。）

### 3.5 修改用户 `PUT /user/update`

**请求示例**

```json
{
  "id": 2,
  "realName": "张三三",
  "phone": "13900000002",
  "email": "zs@example.com",
  "role": "user",
  "status": 1
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 3.6 启用/禁用用户 `PUT /user/status`

**请求示例**

```json
{
  "id": 2,
  "status": 0
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 4. 煤种管理模块（`coal_type`）

### 4.1 煤种详情 `GET /coalType/detail/{id}`

**请求示例**

```bash
curl -s "http://localhost:8080/coalType/detail/1"
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "coalCode": "CT001",
    "coalName": "朔州4#长焰煤",
    "coalCategory": "长焰煤",
    "sourceArea": "山西朔州",
    "purchasePrice": 420.0,
    "transportMode": "铁路",
    "blendableFlag": 1,
    "remark": "低硫动力煤样例",
    "createTime": "2026-04-18T16:50:39",
    "updateTime": "2026-04-18T16:50:39"
  }
}
```

### 4.2 新增煤种 `POST /coalType/add`

**请求示例**

```json
{
  "coalCode": "CT007",
  "coalName": "示例煤种",
  "coalCategory": "动力煤",
  "sourceArea": "山西",
  "purchasePrice": 400.0,
  "transportMode": "铁路",
  "blendableFlag": 1,
  "remark": "接口设计示例"
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 7,
    "coalCode": "CT007",
    "coalName": "示例煤种",
    "coalCategory": "动力煤",
    "sourceArea": "山西",
    "purchasePrice": 400.0,
    "transportMode": "铁路",
    "blendableFlag": 1,
    "remark": "接口设计示例",
    "createTime": "2026-04-19T10:05:00",
    "updateTime": "2026-04-19T10:05:00"
  }
}
```

### 4.3 修改煤种 `PUT /coalType/update`

**请求示例**

```json
{
  "id": 7,
  "purchasePrice": 410.0,
  "remark": "价格调整"
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 4.4 删除煤种 `DELETE /coalType/delete/{id}`

**请求示例**

```bash
curl -s -X DELETE "http://localhost:8080/coalType/delete/7"
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 5. 煤质管理模块（`coal_quality`）

### 5.1 煤质分页 `GET /coalQuality/page`

**请求示例**

```bash
curl -s "http://localhost:8080/coalQuality/page?current=1&size=10&coalId=1&status=1"
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "coalId": 1,
        "batchNo": "B202604001",
        "sampleTime": "2026-04-01T08:00:00",
        "ashContent": 18.5,
        "sulfurContent": 0.5,
        "moistureContent": 8.2,
        "volatileContent": 31.3,
        "calorificValue": 3150.0,
        "fixedCarbon": 42.0,
        "status": 1,
        "createTime": "2026-04-18T16:50:55",
        "updateTime": "2026-04-18T16:50:55"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 5.2 按煤种列表 `GET /coalQuality/listByCoal/{coalId}`

**请求示例**

```bash
curl -s "http://localhost:8080/coalQuality/listByCoal/1"
```

**返回示例**（`data` 为数组）

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "coalId": 1,
      "batchNo": "B202604001",
      "sampleTime": "2026-04-01T08:00:00",
      "ashContent": 18.5,
      "sulfurContent": 0.5,
      "moistureContent": 8.2,
      "volatileContent": 31.3,
      "calorificValue": 3150.0,
      "fixedCarbon": 42.0,
      "status": 1,
      "createTime": "2026-04-18T16:50:55",
      "updateTime": "2026-04-18T16:50:55"
    }
  ]
}
```

### 5.3 最新煤质 `GET /coalQuality/latest/{coalId}`

**请求示例**

```bash
curl -s "http://localhost:8080/coalQuality/latest/1"
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "coalId": 1,
    "batchNo": "B202604001",
    "sampleTime": "2026-04-01T08:00:00",
    "ashContent": 18.5,
    "sulfurContent": 0.5,
    "moistureContent": 8.2,
    "volatileContent": 31.3,
    "calorificValue": 3150.0,
    "fixedCarbon": 42.0,
    "status": 1,
    "createTime": "2026-04-18T16:50:55",
    "updateTime": "2026-04-18T16:50:55"
  }
}
```

### 5.4 煤质详情 / 新增 / 修改 / 删除

**详情请求**

```bash
curl -s "http://localhost:8080/coalQuality/detail/1"
```

**详情返回**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "coalId": 1,
    "batchNo": "B202604001",
    "sampleTime": "2026-04-01T08:00:00",
    "ashContent": 18.5,
    "sulfurContent": 0.5,
    "moistureContent": 8.2,
    "volatileContent": 31.3,
    "calorificValue": 3150.0,
    "fixedCarbon": 42.0,
    "status": 1,
    "createTime": "2026-04-18T16:50:55",
    "updateTime": "2026-04-18T16:50:55"
  }
}
```

**新增请求 `POST /coalQuality/add`**

```json
{
  "coalId": 1,
  "batchNo": "B202604100",
  "sampleTime": "2026-04-19T09:00:00",
  "ashContent": 18.0,
  "sulfurContent": 0.48,
  "moistureContent": 8.0,
  "volatileContent": 31.0,
  "calorificValue": 3180.0,
  "fixedCarbon": 42.5,
  "status": 1
}
```

**新增返回**

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 10 }
}
```

**修改请求 `PUT /coalQuality/update`**

```json
{
  "id": 10,
  "ashContent": 17.8,
  "status": 1
}
```

**修改/删除返回**（与煤种类似，可为 `data: null`）

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 6. 库存管理模块（`inventory`）

### 6.1 库存分页 `GET /inventory/page`

**请求示例**

```bash
curl -s "http://localhost:8080/inventory/page?current=1&size=10&coalId=1&status=1"
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "coalId": 1,
        "warehouseCode": "W001",
        "stockQuantity": 12000.0,
        "availableQuantity": 10000.0,
        "updateTime": "2026-04-10T08:00:00",
        "status": 1,
        "remark": "长焰煤库存充足"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 6.2 按煤种查库存 `GET /inventory/byCoal/{coalId}`

**返回示例**（列表）

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "coalId": 1,
      "warehouseCode": "W001",
      "stockQuantity": 12000.0,
      "availableQuantity": 10000.0,
      "updateTime": "2026-04-10T08:00:00",
      "status": 1,
      "remark": "长焰煤库存充足"
    }
  ]
}
```

### 6.3 可用库存汇总 `GET /inventory/available/{coalId}`

**返回示例**（与前端约定二选一：汇总对象或明细列表；此处为汇总示例）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "coalId": 1,
    "totalAvailableQuantity": 10000.0,
    "warehouses": [
      {
        "warehouseCode": "W001",
        "availableQuantity": 10000.0
      }
    ]
  }
}
```

### 6.4 新增库存 `POST /inventory/add`

**请求示例**

```json
{
  "coalId": 1,
  "warehouseCode": "W010",
  "stockQuantity": 5000.0,
  "availableQuantity": 4800.0,
  "status": 1,
  "remark": "新仓入库"
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 10 }
}
```

---

## 7. 订单管理模块（`orders`）

### 7.1 订单分页 `GET /order/page`

**请求示例**

```bash
curl -s "http://localhost:8080/order/page?current=1&size=10&keyword=热电厂&orderStatus=pending"
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "orderCode": "O202604001",
        "customerName": "华东热电厂",
        "demandQuantity": 5000.0,
        "targetAsh": 18.0,
        "targetSulfur": 0.8,
        "targetMoisture": 8.5,
        "targetVolatile": 25.0,
        "targetCalorific": 5000.0,
        "priorityLevel": 3,
        "deliveryDate": "2026-04-20",
        "orderStatus": "pending",
        "remark": "低硫动力煤订单",
        "createTime": "2026-04-18T16:51:12",
        "updateTime": "2026-04-18T16:51:12"
      }
    ],
    "total": 5,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 7.2 订单详情 `GET /order/detail/{id}`

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "orderCode": "O202604001",
    "customerName": "华东热电厂",
    "demandQuantity": 5000.0,
    "targetAsh": 18.0,
    "targetSulfur": 0.8,
    "targetMoisture": 8.5,
    "targetVolatile": 25.0,
    "targetCalorific": 5000.0,
    "priorityLevel": 3,
    "deliveryDate": "2026-04-20",
    "orderStatus": "pending",
    "remark": "低硫动力煤订单",
    "createTime": "2026-04-18T16:51:12",
    "updateTime": "2026-04-18T16:51:12"
  }
}
```

### 7.3 新增订单 `POST /order/add`

**请求示例**

```json
{
  "orderCode": "O202604100",
  "customerName": "示例客户",
  "demandQuantity": 1000.0,
  "targetAsh": 19.0,
  "targetSulfur": 1.0,
  "targetMoisture": 9.0,
  "targetVolatile": 26.0,
  "targetCalorific": 4700.0,
  "priorityLevel": 2,
  "deliveryDate": "2026-05-01",
  "orderStatus": "pending",
  "remark": "新增订单示例"
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 6 }
}
```

### 7.4 更新订单状态 `PUT /order/status`

**请求示例**

```json
{
  "id": 1,
  "orderStatus": "generated"
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 8. 规则知识管理模块（`rule_knowledge`）

### 8.1 规则分页 `GET /ruleKnowledge/page`

**请求示例**

```bash
curl -s "http://localhost:8080/ruleKnowledge/page?current=1&size=10&ruleType=质量约束&status=1"
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 4,
        "ruleCode": "R004",
        "ruleName": "高热值补偿规则",
        "ruleType": "质量约束",
        "ruleContent": "当基础动力煤热值不足时，可引入高热值煤种进行补偿，但成本需同步评估。",
        "applicableScope": "热值不足场景",
        "priorityLevel": 3,
        "status": 1,
        "sourceDesc": "测试规则",
        "createTime": "2026-04-18T16:51:19",
        "updateTime": "2026-04-18T16:51:19"
      }
    ],
    "total": 5,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 8.2 按类型 `GET /ruleKnowledge/byType?ruleType=经验规则`

**返回示例**（数组）

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 2,
      "ruleCode": "R002",
      "ruleName": "低硫煤优先规则",
      "ruleType": "经验规则",
      "ruleContent": "当目标发热量高于5000且硫分要求严格时，优先调用低硫贫煤和弱粘煤。",
      "applicableScope": "高热值低硫订单",
      "priorityLevel": 4,
      "status": 1,
      "sourceDesc": "测试规则",
      "createTime": "2026-04-18T16:51:19",
      "updateTime": "2026-04-18T16:51:19"
    }
  ]
}
```

### 8.3 启用规则列表 `GET /ruleKnowledge/enabled`

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "ruleCode": "R001",
      "ruleName": "高硫煤限配规则",
      "ruleType": "配比约束",
      "ruleContent": "当订单硫分上限小于等于0.8%时，高硫煤种配比不得超过10%。",
      "applicableScope": "低硫订单",
      "priorityLevel": 5,
      "status": 1,
      "sourceDesc": "测试规则",
      "createTime": "2026-04-18T16:51:19",
      "updateTime": "2026-04-18T16:51:19"
    }
  ]
}
```

### 8.4 新增规则 `POST /ruleKnowledge/add`

**请求示例**

```json
{
  "ruleCode": "R100",
  "ruleName": "示例规则",
  "ruleType": "经验规则",
  "ruleContent": "示例规则正文……",
  "applicableScope": "全部订单",
  "priorityLevel": 1,
  "status": 1,
  "sourceDesc": "手工录入"
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 10 }
}
```

---

## 9. 历史案例管理模块（`case_sample`）

### 9.1 案例分页 `GET /caseSample/page`

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "caseCode": "C001",
        "caseName": "低硫动力煤配煤案例",
        "orderDesc": "需求5000吨，硫分不高于0.8%，热值不低于5000大卡。",
        "blendDesc": "采用弱粘煤+贫煤+少量不粘煤配比方案。",
        "resultDesc": "方案满足热值和硫分要求，成本适中。",
        "qualityResult": "灰分17.2%，硫分0.58%，热值5080kcal/kg",
        "costResult": 2460000.0,
        "effectivenessEval": "良好",
        "status": 1,
        "createTime": "2026-04-18T16:51:28",
        "updateTime": "2026-04-18T16:51:28"
      }
    ],
    "total": 3,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 9.2 关键词检索 `GET /caseSample/search?keyword=低硫`

**返回示例**（可与分页结构一致；此处为列表）

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "caseCode": "C001",
      "caseName": "低硫动力煤配煤案例",
      "orderDesc": "需求5000吨，硫分不高于0.8%，热值不低于5000大卡。",
      "blendDesc": "采用弱粘煤+贫煤+少量不粘煤配比方案。",
      "resultDesc": "方案满足热值和硫分要求，成本适中。",
      "qualityResult": "灰分17.2%，硫分0.58%，热值5080kcal/kg",
      "costResult": 2460000.0,
      "effectivenessEval": "良好",
      "status": 1,
      "createTime": "2026-04-18T16:51:28",
      "updateTime": "2026-04-18T16:51:28"
    }
  ]
}
```

### 9.3 新增案例 `POST /caseSample/add`

**请求示例**

```json
{
  "caseCode": "C100",
  "caseName": "示例案例",
  "orderDesc": "订单描述……",
  "blendDesc": "配煤描述……",
  "resultDesc": "结果描述……",
  "qualityResult": "灰分18%，硫分0.9%",
  "costResult": 1000000.0,
  "effectivenessEval": "良好",
  "status": 1
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": { "id": 10 }
}
```

---

## 10. 智能配煤与方案模块（`blend_plan`、`blend_plan_detail`）

### 10.1 生成配煤方案 `POST /blendPlan/generate`

**请求示例**

```json
{
  "orderId": 1,
  "createBy": 1
}
```

**返回示例**（字段与实现算法相关，此处为**联调级完整形态示例**）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "order": {
      "id": 1,
      "orderCode": "O202604001",
      "customerName": "华东热电厂",
      "demandQuantity": 5000.0,
      "targetAsh": 18.0,
      "targetSulfur": 0.8,
      "targetMoisture": 8.5,
      "targetVolatile": 25.0,
      "targetCalorific": 5000.0,
      "priorityLevel": 3,
      "deliveryDate": "2026-04-20",
      "orderStatus": "pending",
      "remark": "低硫动力煤订单"
    },
    "constraints": {
      "demandQuantity": 5000.0,
      "maxAsh": 18.0,
      "maxSulfur": 0.8,
      "maxMoisture": 8.5,
      "referenceVolatile": 25.0,
      "minCalorific": 5000.0,
      "priorityLevel": 3
    },
    "recommendedPlan": {
      "plan": {
        "id": 101,
        "planCode": "P20260419103000001",
        "orderId": 1,
        "planName": "推荐方案-A",
        "totalCost": 2100000.0,
        "qualityScore": 88.5,
        "costScore": 82.0,
        "stabilityScore": 80.0,
        "overallScore": 85.2,
        "planStatus": "generated",
        "explanation": "为满足订单低硫要求，优先选用低硫煤种；热值通过弱粘煤补偿。",
        "riskTip": "请关注高价煤种占比。",
        "createBy": 1,
        "createTime": "2026-04-19T10:30:00",
        "updateTime": "2026-04-19T10:30:00"
      },
      "details": [
        {
          "id": 1001,
          "planId": 101,
          "coalId": 5,
          "coalName": "河南二1贫煤",
          "blendRatio": 0.45,
          "useQuantity": 2250.0,
          "predictedAsh": 12.5,
          "predictedSulfur": 0.35,
          "predictedMoisture": 6.0,
          "predictedVolatile": 19.0,
          "predictedCalorific": 5100.0,
          "unitCost": 500.0,
          "remark": null
        },
        {
          "id": 1002,
          "planId": 101,
          "coalId": 4,
          "coalName": "大同弱粘煤",
          "blendRatio": 0.55,
          "useQuantity": 2750.0,
          "predictedAsh": 14.0,
          "predictedSulfur": 0.55,
          "predictedMoisture": 7.0,
          "predictedVolatile": 26.0,
          "predictedCalorific": 5050.0,
          "unitCost": 520.0,
          "remark": null
        }
      ]
    },
    "candidatePlans": [
      {
        "plan": {
          "id": 102,
          "planCode": "P20260419103000002",
          "orderId": 1,
          "planName": "候选方案-B",
          "totalCost": 2050000.0,
          "qualityScore": 84.0,
          "costScore": 88.0,
          "stabilityScore": 78.0,
          "overallScore": 83.5,
          "planStatus": "generated",
          "explanation": "成本略优，硫分余量较小。",
          "riskTip": null,
          "createBy": 1,
          "createTime": "2026-04-19T10:30:01",
          "updateTime": "2026-04-19T10:30:01"
        },
        "details": []
      }
    ],
    "matchedRules": [
      {
        "id": 2,
        "ruleCode": "R002",
        "ruleName": "低硫煤优先规则",
        "ruleType": "经验规则",
        "hitReason": "订单目标热值≥5000 且硫分约束较严"
      }
    ],
    "matchedCases": [
      {
        "id": 1,
        "caseCode": "C001",
        "caseName": "低硫动力煤配煤案例",
        "summary": "历史相似低硫动力煤订单"
      }
    ],
    "explainSummary": "综合质量、成本与库存，推荐方案-A 得分最高；候选方案-B 成本更优可作为备选。"
  }
}
```

说明：`coalName` 为 VO 扩展字段；若实现阶段未做关联，可仅返回 `coalId`。

### 10.2 方案分页 `GET /blendPlan/page`

**请求示例**

```bash
curl -s "http://localhost:8080/blendPlan/page?current=1&size=10&orderId=1&planStatus=generated"
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 101,
        "planCode": "P20260419103000001",
        "orderId": 1,
        "planName": "推荐方案-A",
        "totalCost": 2100000.0,
        "qualityScore": 88.5,
        "costScore": 82.0,
        "stabilityScore": 80.0,
        "overallScore": 85.2,
        "planStatus": "generated",
        "explanation": "为满足订单低硫要求……",
        "riskTip": "请关注高价煤种占比。",
        "createBy": 1,
        "createTime": "2026-04-19T10:30:00",
        "updateTime": "2026-04-19T10:30:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 10.3 方案详情 `GET /blendPlan/detail/{id}`

**返回示例**（可与 10.1 中单方案结构一致）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 101,
    "planCode": "P20260419103000001",
    "orderId": 1,
    "planName": "推荐方案-A",
    "totalCost": 2100000.0,
    "qualityScore": 88.5,
    "costScore": 82.0,
    "stabilityScore": 80.0,
    "overallScore": 85.2,
    "planStatus": "generated",
    "explanation": "为满足订单低硫要求……",
    "riskTip": "请关注高价煤种占比。",
    "createBy": 1,
    "createTime": "2026-04-19T10:30:00",
    "updateTime": "2026-04-19T10:30:00"
  }
}
```

### 10.4 方案明细 `GET /blendPlan/details/{planId}`

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1001,
      "planId": 101,
      "coalId": 5,
      "blendRatio": 0.45,
      "useQuantity": 2250.0,
      "predictedAsh": 12.5,
      "predictedSulfur": 0.35,
      "predictedMoisture": 6.0,
      "predictedVolatile": 19.0,
      "predictedCalorific": 5100.0,
      "unitCost": 500.0,
      "remark": null
    }
  ]
}
```

### 10.5 按订单查方案 `GET /blendPlan/byOrder/{orderId}`

**返回示例**（数组，按创建时间降序）

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 102,
      "planCode": "P20260419103000002",
      "orderId": 1,
      "planName": "候选方案-B",
      "overallScore": 83.5,
      "planStatus": "generated",
      "createTime": "2026-04-19T10:30:01"
    },
    {
      "id": 101,
      "planCode": "P20260419103000001",
      "orderId": 1,
      "planName": "推荐方案-A",
      "overallScore": 85.2,
      "planStatus": "selected",
      "createTime": "2026-04-19T10:30:00"
    }
  ]
}
```

### 10.6 选择推荐方案 `PUT /blendPlan/select`

**请求示例**

```json
{
  "planId": 101
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 10.7 历史记录 `GET /blendPlan/history`

**请求示例**

```bash
curl -s "http://localhost:8080/blendPlan/history?current=1&size=10&createTimeBegin=2026-04-01&createTimeEnd=2026-04-30"
```

**返回示例**（与 `page` 相同分页结构）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "size": 10,
    "current": 1,
    "pages": 0
  }
}
```

---

## 11. 模型配置模块（`model_config`）

### 11.1 列表 `GET /modelConfig/list`

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "modelName": "示例大模型",
      "modelType": "LLM",
      "apiUrl": "https://api.example.com/v1/chat",
      "apiKey": "sk-******",
      "temperature": 0.7,
      "topP": 0.9,
      "status": 1,
      "remark": "毕设占位",
      "createTime": "2026-04-19T10:00:00",
      "updateTime": "2026-04-19T10:00:00"
    }
  ]
}
```

### 11.2 分页 `GET /modelConfig/page`

**返回示例**（同 1.3 分页 `data` 包裹单条 `records`）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "modelName": "示例大模型",
        "modelType": "LLM",
        "apiUrl": "https://api.example.com/v1/chat",
        "apiKey": "sk-******",
        "temperature": 0.7,
        "topP": 0.9,
        "status": 1,
        "remark": "毕设占位",
        "createTime": "2026-04-19T10:00:00",
        "updateTime": "2026-04-19T10:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 11.3 修改配置 `PUT /modelConfig/update`

**请求示例**

```json
{
  "id": 1,
  "modelName": "示例大模型",
  "modelType": "LLM",
  "apiUrl": "https://api.example.com/v2/chat",
  "apiKey": "sk-new-key",
  "temperature": 0.5,
  "topP": 0.85,
  "status": 1,
  "remark": "更新接口地址"
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 11.4 启用/停用 `PUT /modelConfig/status`

**请求示例**

```json
{
  "id": 1,
  "status": 0
}
```

**返回示例**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 12. 接口清单汇总（便于排期）

| 模块 | 方法 | 路径 |
|------|------|------|
| 健康检查 | `GET` | `/health` |
| 登录 | `POST` | `/auth/login` |
| 当前用户 | `GET` | `/auth/me` |
| 用户 | `GET` | `/user/page` |
| 用户 | `POST` | `/user/add` |
| 用户 | `PUT` | `/user/update` |
| 用户 | `PUT` | `/user/status` |
| 煤种 | `GET` | `/coalType/page` |
| 煤种 | `GET` | `/coalType/detail/{id}` |
| 煤种 | `POST` | `/coalType/add` |
| 煤种 | `PUT` | `/coalType/update` |
| 煤种 | `DELETE` | `/coalType/delete/{id}` |
| 煤质 | `GET` | `/coalQuality/page` |
| 煤质 | `GET` | `/coalQuality/listByCoal/{coalId}` |
| 煤质 | `GET` | `/coalQuality/latest/{coalId}` |
| 煤质 | `GET` | `/coalQuality/detail/{id}` |
| 煤质 | `POST` | `/coalQuality/add` |
| 煤质 | `PUT` | `/coalQuality/update` |
| 煤质 | `DELETE` | `/coalQuality/delete/{id}` |
| 库存 | `GET` | `/inventory/page` |
| 库存 | `GET` | `/inventory/byCoal/{coalId}` |
| 库存 | `GET` | `/inventory/available/{coalId}` |
| 库存 | `GET` | `/inventory/detail/{id}` |
| 库存 | `POST` | `/inventory/add` |
| 库存 | `PUT` | `/inventory/update` |
| 库存 | `DELETE` | `/inventory/delete/{id}` |
| 订单 | `GET` | `/order/page` |
| 订单 | `GET` | `/order/detail/{id}` |
| 订单 | `POST` | `/order/add` |
| 订单 | `PUT` | `/order/update` |
| 订单 | `DELETE` | `/order/delete/{id}` |
| 订单 | `PUT` | `/order/status` |
| 规则 | `GET` | `/ruleKnowledge/page` |
| 规则 | `GET` | `/ruleKnowledge/byType` |
| 规则 | `GET` | `/ruleKnowledge/enabled` |
| 规则 | `GET` | `/ruleKnowledge/detail/{id}` |
| 规则 | `POST` | `/ruleKnowledge/add` |
| 规则 | `PUT` | `/ruleKnowledge/update` |
| 规则 | `DELETE` | `/ruleKnowledge/delete/{id}` |
| 案例 | `GET` | `/caseSample/page` |
| 案例 | `GET` | `/caseSample/search` |
| 案例 | `GET` | `/caseSample/detail/{id}` |
| 案例 | `POST` | `/caseSample/add` |
| 案例 | `PUT` | `/caseSample/update` |
| 案例 | `DELETE` | `/caseSample/delete/{id}` |
| 配煤 | `POST` | `/blendPlan/generate` |
| 方案 | `GET` | `/blendPlan/page` |
| 方案 | `GET` | `/blendPlan/detail/{id}` |
| 方案 | `GET` | `/blendPlan/details/{planId}` |
| 方案 | `GET` | `/blendPlan/byOrder/{orderId}` |
| 方案 | `PUT` | `/blendPlan/select` |
| 方案 | `GET` | `/blendPlan/history` |
| 模型配置 | `GET` | `/modelConfig/list` |
| 模型配置 | `GET` | `/modelConfig/page`（可选） |
| 模型配置 | `GET` | `/modelConfig/detail/{id}` |
| 模型配置 | `POST` | `/modelConfig/add`（可选） |
| 模型配置 | `PUT` | `/modelConfig/update` |
| 模型配置 | `PUT` | `/modelConfig/status` |

---

## 13. 扩展与实现建议（与需求一致）

1. **JWT**：登录改为颁发 Token 后，`/auth/me` 与各写操作接口统一走鉴权过滤器；本文档路径不变。
2. **Python / 大模型**：`/blendPlan/generate` 内部可拆 `IntelligentBlendService`，先走规则引擎，再异步或同步调用外部 HTTP，结果仍落入 `blend_plan` 表。
3. **DTO / VO**：技术方案建议请求用 DTO、响应用 VO；示例 JSON 字段可直接映射为 Java DTO/VO 属性名（驼峰）。
4. **Swagger**：实现阶段用 Springdoc 注解标注上述路径与模型，便于毕设演示「接口文档齐全」。

---

## 14. 文档修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-04-19 | 初稿：对齐需求文档 5.1～5.10、技术方案 8.x、数据库表结构 |
| v1.1 | 2026-04-19 | 补充各接口请求示例与返回示例（含统一 `Result` 包装及 `curl`/HTTP 示例） |
