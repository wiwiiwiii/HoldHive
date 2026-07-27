# HoldHive REST API 文档

## 1. 文档范围

本文定义 HoldHive MVP 的 REST API 契约，并标记后续扩展接口。实现后应使用 OpenAPI/Swagger 维护机器可读版本；本文和 OpenAPI 必须保持一致。

- Base URL：`/api/v1`
- 数据格式：`application/json`
- 字符编码：UTF-8
- 时间格式：ISO 8601 UTC，例如 `2026-07-24T08:30:00Z`
- 金额和数量：JSON number；服务端必须使用定点数处理
- 认证：MVP 无认证，仅允许在本地或受控演示环境运行

## 2. 通用约定

### 2.1 标识符

资源 ID 在示例中使用正整数。客户端必须将 ID 当作不透明标识，不推断创建顺序或业务含义。

### 2.2 数值与币种

- `quantity` 必须大于 `0`，最多 8 位小数。
- `averagePurchasePrice` 必须大于等于 `0`，最多 8 位小数。
- `currentPrice` 和金额字段最多返回 8 位小数；UI 通常显示 2 位。
- 币种使用 ISO 4217 三位代码，例如 `USD`。
- 百分比直接返回百分数，例如 `12.34` 表示 `12.34%`，不是 `0.1234`。

### 2.3 数据状态

价格状态使用以下枚举：

| 状态 | 含义 |
| --- | --- |
| `LIVE` | 从外部数据服务获得的新鲜价格 |
| `CACHED` | 使用仍在允许时效内的缓存价格 |
| `DEMO` | 使用演示数据，不代表真实市场价格 |
| `UNAVAILABLE` | 没有可用价格，该持仓不计入当前估值 |

### 2.4 错误响应

所有非 `2xx` 响应使用统一结构：

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "fieldErrors": [
    {
      "field": "quantity",
      "message": "must be greater than 0"
    }
  ],
  "traceId": "8f0c2d15d1e44dc8",
  "timestamp": "2026-07-24T08:30:00Z"
}
```

生产响应不得包含堆栈、SQL、内部类名或密钥。`traceId` 用于关联服务端日志。

### 2.5 错误码

| HTTP | `code` | 使用场景 |
| --- | --- | --- |
| `400` | `VALIDATION_FAILED` | 字段缺失、格式或范围错误 |
| `400` | `MALFORMED_JSON` | JSON 无法解析 |
| `404` | `HOLDING_NOT_FOUND` | 持仓不存在 |
| `409` | `HOLDING_ALREADY_EXISTS` | 同一组合已存在相同证券 |
| `409` | `CONCURRENT_MODIFICATION` | 资源版本冲突，供后续更新接口使用 |
| `503` | `PRICE_SERVICE_UNAVAILABLE` | 请求要求实时价格但价格服务不可用 |
| `500` | `INTERNAL_ERROR` | 未预期服务端错误 |

## 3. 数据模型

### 3.1 Holding

```json
{
  "id": 101,
  "ticker": "AAPL",
  "exchangeCode": "NASDAQ",
  "displayName": "Apple Inc.",
  "assetType": "STOCK",
  "currency": "USD",
  "quantity": 10.00000000,
  "averagePurchasePrice": 175.50000000,
  "currentPrice": 210.25000000,
  "marketValue": 2102.50000000,
  "costBasis": 1755.00000000,
  "unrealizedGainLoss": 347.50000000,
  "unrealizedGainLossPercent": 19.80056980,
  "allocationPercent": 42.50000000,
  "priceStatus": "LIVE",
  "priceObservedAt": "2026-07-24T08:29:00Z",
  "createdAt": "2026-07-24T08:00:00Z",
  "updatedAt": "2026-07-24T08:00:00Z"
}
```

当价格不可用时，`currentPrice`、`marketValue`、`unrealizedGainLoss`、`unrealizedGainLossPercent`、`allocationPercent` 和 `priceObservedAt` 为 `null`，`priceStatus` 为 `UNAVAILABLE`。

### 3.2 PortfolioSummary

```json
{
  "portfolioId": 1,
  "portfolioName": "My Portfolio",
  "baseCurrency": "USD",
  "holdingCount": 3,
  "pricedHoldingCount": 2,
  "valuationStatus": "PARTIAL",
  "totalCostBasis": 4200.00000000,
  "totalMarketValue": 4680.50000000,
  "totalUnrealizedGainLoss": 480.50000000,
  "totalUnrealizedGainLossPercent": 11.44047619,
  "priceAsOf": "2026-07-24T08:29:00Z",
  "allocations": [
    {
      "holdingId": 101,
      "ticker": "AAPL",
      "marketValue": 2102.50000000,
      "allocationPercent": 44.91934622
    }
  ],
  "unpricedHoldings": [
    {
      "holdingId": 103,
      "ticker": "UNKNOWN",
      "reason": "PRICE_UNAVAILABLE"
    }
  ]
}
```

`valuationStatus` 取值：

- `COMPLETE`：所有持仓都有有效价格。
- `PARTIAL`：部分持仓没有价格，总市值只包括有效持仓。
- `UNAVAILABLE`：没有任何持仓可以估值。
- `EMPTY`：组合没有持仓。

## 4. MVP 接口

### 4.1 查询全部持仓

```http
GET /api/v1/holdings
```

可选查询参数：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `sort` | `ticker,asc` | 支持 `ticker`、`marketValue`、`unrealizedGainLoss` |
| `priceMode` | `BEST_AVAILABLE` | `BEST_AVAILABLE`、`LIVE_ONLY` 或 `DEMO_ALLOWED` |

成功响应：`200 OK`

```json
{
  "items": [
    {
      "id": 101,
      "ticker": "AAPL",
      "exchangeCode": "NASDAQ",
      "displayName": "Apple Inc.",
      "assetType": "STOCK",
      "currency": "USD",
      "quantity": 10.00000000,
      "averagePurchasePrice": 175.50000000,
      "currentPrice": 210.25000000,
      "marketValue": 2102.50000000,
      "costBasis": 1755.00000000,
      "unrealizedGainLoss": 347.50000000,
      "unrealizedGainLossPercent": 19.80056980,
      "allocationPercent": 42.50000000,
      "priceStatus": "LIVE",
      "priceObservedAt": "2026-07-24T08:29:00Z",
      "createdAt": "2026-07-24T08:00:00Z",
      "updatedAt": "2026-07-24T08:00:00Z"
    }
  ],
  "count": 1
}
```

空组合返回 `200` 和 `{"items": [], "count": 0}`，不返回 `404`。

### 4.2 查询单个持仓

```http
GET /api/v1/holdings/{holdingId}
```

- 成功：`200 OK`，响应为 `Holding`。
- 不存在：`404 HOLDING_NOT_FOUND`。

### 4.3 创建持仓

```http
POST /api/v1/holdings
Content-Type: application/json
```

请求：

```json
{
  "ticker": "AAPL",
  "exchangeCode": "NASDAQ",
  "quantity": 10.00000000,
  "averagePurchasePrice": 175.50000000
}
```

规则：

- `ticker` 必填，去除首尾空格后转为大写，长度为 1-32。
- `exchangeCode` 可选；缺省为 `UNKNOWN`。若市场数据服务要求交易所，服务端返回字段错误。
- `quantity` 必填且大于 0。
- `averagePurchasePrice` 必填且大于等于 0。
- 同一组合、ticker 和 exchangeCode 已存在时不自动合并，返回 `409`，避免未经用户确认地改变平均成本。

成功响应：`201 Created`

```http
Location: /api/v1/holdings/101
```

响应体为创建后的 `Holding`。如果持仓已保存但暂时取不到价格，仍返回 `201`，其中 `priceStatus` 为 `UNAVAILABLE`。

失败：

- `400 VALIDATION_FAILED`
- `409 HOLDING_ALREADY_EXISTS`

### 4.4 删除持仓

```http
DELETE /api/v1/holdings/{holdingId}
```

- 成功：`204 No Content`，无响应体。
- 不存在：`404 HOLDING_NOT_FOUND`。

重复删除返回 `404`，客户端可以据此刷新本地列表。删除持仓不会删除证券主数据或价格历史。

### 4.5 查询组合摘要

```http
GET /api/v1/portfolio/summary
```

可选查询参数：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `priceMode` | `BEST_AVAILABLE` | `BEST_AVAILABLE`、`LIVE_ONLY` 或 `DEMO_ALLOWED` |

成功响应：`200 OK`，响应为 `PortfolioSummary`。

即使部分价格不可用也返回 `200` 和 `valuationStatus = PARTIAL`，因为持仓和部分估值仍可使用。只有调用方明确要求 `LIVE_ONLY` 且实时价格服务整体不可用时，才返回 `503 PRICE_SERVICE_UNAVAILABLE`。

### 4.6 搜索证券

```http
GET /api/v1/market/search?query=AAPL
```

该接口由后端访问外部行情搜索源，例如东方财富搜索建议接口。前端不得直接请求第三方行情服务。

可选查询参数：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `query` | 必填 | 用户输入的 ticker 或名称片段 |
| `market` | 空 | 可选市场过滤，例如 `US`、`SH`、`SZ`、`HK` |

成功响应：`200 OK`

```json
{
  "query": "AAPL",
  "results": [
    {
      "ticker": "AAPL",
      "displayName": "苹果",
      "exchangeCode": "NASDAQ",
      "provider": "EASTMONEY",
      "providerQuoteId": "105.AAPL",
      "assetType": "US_STOCK"
    }
  ],
  "source": "EASTMONEY",
  "cached": false
}
```

搜索结果只用于辅助新增持仓。最终保存时，后端仍需重新校验 `providerQuoteId` 是否可报价。

### 4.7 批量获取报价

```http
GET /api/v1/market/quotes?providerQuoteIds=1.600519,0.000001,105.AAPL&priceMode=BEST_AVAILABLE
```

可选查询参数：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `providerQuoteIds` | 必填 | 第三方行情源的 quote id，多个值逗号分隔 |
| `priceMode` | `BEST_AVAILABLE` | `BEST_AVAILABLE`、`LIVE_ONLY` 或 `DEMO_ALLOWED` |

成功响应：`200 OK`

```json
{
  "provider": "EASTMONEY",
  "priceMode": "BEST_AVAILABLE",
  "quotes": [
    {
      "ticker": "AAPL",
      "displayName": "苹果",
      "providerQuoteId": "105.AAPL",
      "currency": "USD",
      "currentPrice": 333.02000000,
      "changeAmount": 11.36000000,
      "changePercent": 3.53,
      "openPrice": 321.79000000,
      "previousClose": 321.66000000,
      "dayHigh": 334.37000000,
      "dayLow": 321.62000000,
      "volume": 47489415,
      "priceStatus": "LIVE",
      "priceObservedAt": "2026-07-25T00:00:00Z",
      "fetchedAt": "2026-07-25T09:10:00Z"
    }
  ],
  "unavailable": []
}
```

`providerQuoteIds` 中某个标的不可用时，不应让整个响应失败；该项进入 `unavailable`，并在 portfolio summary 中体现为 `PARTIAL` 或 `UNAVAILABLE`。

### 4.8 健康检查

```http
GET /api/v1/health
```

成功响应：`200 OK`

```json
{
  "status": "UP",
  "database": "UP",
  "priceProvider": "DEGRADED",
  "timestamp": "2026-07-24T08:30:00Z"
}
```

该接口不得返回数据库连接串、凭据或内部网络地址。

## 5. 计算规则

```text
costBasis = quantity × averagePurchasePrice
marketValue = quantity × currentPrice
unrealizedGainLoss = marketValue - costBasis
unrealizedGainLossPercent = unrealizedGainLoss ÷ costBasis × 100
allocationPercent = marketValue ÷ totalPricedMarketValue × 100
```

- `costBasis = 0` 时，`unrealizedGainLossPercent` 返回 `null`。
- 价格不可用的持仓不计入 `totalMarketValue` 和 allocation 分母。
- `totalCostBasis` 包括全部持仓成本，便于用户理解投入；若摘要为部分估值，UI 必须明确提示总盈亏并非完整组合结果。
- 中间计算不提前舍入，API 最多返回 8 位小数，UI 再按币种规则展示。
- MVP 的表现只是当前快照未实现盈亏，不是时间加权或资金加权收益。

## 6. 一致性、缓存与失败处理

- 创建和删除持仓成功后，前端重新获取 holdings 和 summary，不在客户端自行推算权威结果。
- 市场价格调用设置短超时，并通过适配器与核心持仓服务隔离。
- `BEST_AVAILABLE` 顺序建议为：新鲜外部价格、有效缓存、明确允许的演示价格、不可用。
- 演示价格永远返回 `priceStatus = DEMO`，页面必须可见地标注。
- 外部价格失败不能回滚已经成功保存的持仓。
- 未预期异常写入带 `traceId` 的服务日志，对外只返回通用错误。

## 7. 后续扩展接口

以下接口不属于两天编码 MVP，只有 P0 全部完成后才进入新 Story：

| 方法与路径 | 用途 |
| --- | --- |
| `PATCH /api/v1/holdings/{id}` | 修改数量和平均成本，配合 `version` 乐观锁 |
| `GET /api/v1/portfolios` | 多组合列表 |
| `POST /api/v1/portfolios` | 创建组合 |
| `GET /api/v1/portfolios/{id}/transactions` | 查询不可变交易流水 |
| `POST /api/v1/portfolios/{id}/transactions` | 记录买入、卖出、股息等交易 |
| `GET /api/v1/portfolios/{id}/performance` | 查询历史估值与表现 |
| `GET /api/v1/portfolios/{id}/insights` | 获取规则型集中度或再平衡提示 |

API 演进使用 `/api/v1` 保持兼容。新增可选字段属于兼容变更；删除字段、改变字段含义或收紧已有输入规则需要新版本或迁移期。

## 8. 安全约束

- MVP 没有认证，因此不得公开部署或存储真实个人投资信息。
- 服务端必须限制请求体大小，并拒绝未知或非法字段。
- 所有数据库操作使用参数化查询或 ORM，不拼接用户输入。
- 日志不得记录密钥、完整请求体中的潜在敏感信息或内部异常堆栈到客户端。
- CORS 只允许实际前端来源；不要在公开环境使用无限制 `*`。

## 9. API 验收与测试矩阵

| 场景 | 预期结果 |
| --- | --- |
| 查询空组合 | `200`，空数组；摘要状态为 `EMPTY` |
| 创建合法持仓 | `201`，包含 `Location` 和持仓响应 |
| ticker 含小写和空格 | 标准化后保存为大写 |
| quantity 为 0、负数或缺失 | `400 VALIDATION_FAILED`，定位 `quantity` |
| averagePurchasePrice 为负数 | `400 VALIDATION_FAILED` |
| 重复持仓 | `409 HOLDING_ALREADY_EXISTS` |
| 查询不存在的持仓 | `404 HOLDING_NOT_FOUND` |
| 删除存在的持仓 | `204`，随后查询返回 `404` |
| 删除不存在的持仓 | `404 HOLDING_NOT_FOUND` |
| 单个证券价格不可用 | `200`，该项为 `UNAVAILABLE`，摘要为 `PARTIAL` |
| 所有价格不可用 | `200`，摘要为 `UNAVAILABLE`；`LIVE_ONLY` 可返回 `503` |
| 使用演示价格 | 响应明确返回 `DEMO`，UI 显示演示标签 |
| 成本为 0 | 盈亏金额可计算，盈亏率为 `null` |
| 未预期异常 | `500 INTERNAL_ERROR`，响应含 `traceId` 且无内部堆栈 |

## 10. 完成标准

- [ ] 所有 MVP 接口均有 OpenAPI schema、示例和状态码说明。
- [ ] Controller、Service 和数据访问层的职责清晰。
- [ ] 接口测试覆盖成功、校验、冲突、不存在和价格服务失败。
- [ ] 后端总体行覆盖率至少 70%。
- [ ] API 与数据库文档中的字段、精度和约束一致。
- [ ] 前端不依赖未文档化字段或错误格式。
- [ ] 产品负责人和技术评审方已确认 MVP 接口和演示数据方案。
