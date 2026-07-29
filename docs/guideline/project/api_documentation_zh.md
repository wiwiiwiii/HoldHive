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

### 2.3 资产类型

MVP 支持六类可展示资产：

| 类型 | 含义 | 估值方式 |
| --- | --- | --- |
| `STOCK` | 普通股票，例如 `AAPL`、`600519` | 通过行情适配器或演示价格估值 |
| `ETF` | 场内基金 / 交易所交易基金，例如 `VOO`、`SPY`、`QQQ` | 与股票使用同一套行情和估值公式 |
| `MUTUAL_FUND` | 场外基金，例如开放式基金、货币基金 | MVP 使用手工、演示或缓存净值；实时净值为 P1 |
| `CRYPTO` | 加密资产，例如 `BTC`、`ETH` | MVP 先支持演示/缓存价格；实时接口为 P1 |
| `CASH` | 现金余额，例如 `USD` | 以组合基准币种固定按 `1.00000000` 估值 |
| `BANK_DEPOSIT` | 银行存款，例如活期或定期存款本金 | 以组合基准币种固定按 `1.00000000` 估值 |

本期不支持债券、基金穿透、基金申赎流水、存款利息自动计提、现金流水、链上钱包、交易所账户同步或多币种汇率换算。

### 2.4 数据状态

价格状态使用以下枚举：

| 状态 | 含义 |
| --- | --- |
| `LIVE` | 从外部数据服务获得的新鲜价格 |
| `CACHED` | 使用仍在允许时效内的缓存价格 |
| `DEMO` | 使用演示数据，不代表真实市场价格 |
| `FIXED` | 不依赖外部行情的固定估值，MVP 用于 `CASH` 和 `BANK_DEPOSIT` |
| `UNAVAILABLE` | 没有可用价格，该持仓不计入当前估值 |

### 2.5 错误响应

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

### 2.6 错误码

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
  "provider": "EASTMONEY",
  "providerQuoteId": "105.AAPL",
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

当价格不可用时，`currentPrice`、`marketValue`、`unrealizedGainLoss`、`unrealizedGainLossPercent`、`allocationPercent` 和 `priceObservedAt` 为 `null`，`priceStatus` 为 `UNAVAILABLE`。`CASH` 和 `BANK_DEPOSIT` 不请求外部行情，`currentPrice` 固定为 `1.00000000`，`priceStatus` 为 `FIXED`。

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
      "assetType": "STOCK",
      "marketValue": 2102.50000000,
      "allocationPercent": 44.91934622
    }
  ],
  "unpricedHoldings": [
    {
      "holdingId": 103,
      "ticker": "UNKNOWN",
      "assetType": "STOCK",
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
      "provider": "EASTMONEY",
      "providerQuoteId": "105.AAPL",
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
  "assetType": "STOCK",
  "ticker": "AAPL",
  "exchangeCode": "NASDAQ",
  "providerQuoteId": "105.AAPL",
  "quantity": 10.00000000,
  "averagePurchasePrice": 175.50000000
}
```

规则：

- `assetType` 必填，允许 `STOCK`、`ETF`、`MUTUAL_FUND`、`CRYPTO`、`CASH`、`BANK_DEPOSIT`；缺省策略如需保留，只能在后端明确按 `STOCK` 处理并返回标准化结果。
- `ticker` 必填，去除首尾空格后转为大写，长度为 1-32。
- `exchangeCode` 可选；缺省为 `UNKNOWN`。若市场数据服务要求交易所，服务端返回字段错误。
- `providerQuoteId` 可选；搜索接口返回该字段时应随创建请求提交，后端仍需重新校验。
- `quantity` 必填且大于 0。
- `averagePurchasePrice` 必填且大于等于 0。
- 同一组合、assetType、ticker 和 exchangeCode 已存在时不自动合并，返回 `409`，避免未经用户确认地改变平均成本。
- `ETF` 表示场内基金，像股票一样有交易所、ticker 和盘中价格。
- `MUTUAL_FUND` 表示场外基金，`ticker` 使用基金代码或自定义代码；MVP 使用手工、演示或缓存净值，不承诺盘中实时价格。
- `CASH` 的 `ticker` 使用币种代码，例如 `USD`；`exchangeCode` 使用 `CASH`；`quantity` 表示现金金额，`averagePurchasePrice` 固定为 `1.00000000`。
- `BANK_DEPOSIT` 的 `ticker` 使用可读代码，例如 `HSBC_USD` 或 `USD_DEPOSIT`；`exchangeCode` 使用 `BANK`；`quantity` 表示存款本金，`averagePurchasePrice` 固定为 `1.00000000`。MVP 不自动计算利息或到期收益。

现金请求示例：

```json
{
  "assetType": "CASH",
  "ticker": "USD",
  "exchangeCode": "CASH",
  "quantity": 4500.00000000,
  "averagePurchasePrice": 1.00000000
}
```

银行存款请求示例：

```json
{
  "assetType": "BANK_DEPOSIT",
  "ticker": "HSBC_USD",
  "exchangeCode": "BANK",
  "displayName": "HSBC USD Deposit",
  "quantity": 3000.00000000,
  "averagePurchasePrice": 1.00000000
}
```

成功响应：`201 Created`

```http
Location: /api/v1/holdings/101
```

响应体为创建后的 `Holding`。如果持仓已保存但暂时取不到价格，仍返回 `201`，其中 `priceStatus` 为 `UNAVAILABLE`。

失败：

- `400 VALIDATION_FAILED`
- `409 HOLDING_ALREADY_EXISTS`

### 4.4 修改持仓数量和平均成本

```http
PATCH /api/v1/holdings/{holdingId}?priceMode=DEMO_ALLOWED
Content-Type: application/json
```

请求：

```json
{
  "quantity": 8.00000000,
  "averagePurchasePrice": 320.00000000
}
```

可选查询参数：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `priceMode` | `BEST_AVAILABLE` | 控制响应体估值口径；本地演示价格使用 `DEMO_ALLOWED` |

规则：

- 只修改持仓的 `quantity` 和 `averagePurchasePrice`，不改变 `assetType`、`ticker`、`exchangeCode` 或 `providerQuoteId`。
- `quantity` 必填且大于 0。
- `averagePurchasePrice` 必填且大于等于 0。
- `CASH` 和 `BANK_DEPOSIT` 始终保持 `averagePurchasePrice = 1.00000000`，即使请求里提交了其他值。
- 成功响应为更新后的 `Holding`，并按请求的 `priceMode` 返回 `currentPrice`、`marketValue`、`priceStatus` 等估值字段。

失败：

- `400 VALIDATION_FAILED`
- `404 HOLDING_NOT_FOUND`

### 4.5 删除持仓

```http
DELETE /api/v1/holdings/{holdingId}
```

- 成功：`204 No Content`，无响应体。
- 不存在：`404 HOLDING_NOT_FOUND`。

重复删除返回 `404`，客户端可以据此刷新本地列表。删除持仓不会删除证券主数据或价格历史。

### 4.6 查询组合摘要

```http
GET /api/v1/portfolio/summary
```

可选查询参数：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `priceMode` | `BEST_AVAILABLE` | `BEST_AVAILABLE`、`LIVE_ONLY` 或 `DEMO_ALLOWED` |

成功响应：`200 OK`，响应为 `PortfolioSummary`。

即使部分价格不可用也返回 `200` 和 `valuationStatus = PARTIAL`，因为持仓和部分估值仍可使用。只有调用方明确要求 `LIVE_ONLY` 且实时价格服务整体不可用时，才返回 `503 PRICE_SERVICE_UNAVAILABLE`。

### 4.7 搜索证券

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
      "assetType": "STOCK"
    }
  ],
  "source": "MIXED",
  "cached": false
}
```

搜索结果只用于辅助新增持仓。最终保存时，后端仍需重新校验 `providerQuoteId` 是否可报价。

### 4.8 批量获取报价

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
  "provider": "MIXED",
  "priceMode": "BEST_AVAILABLE",
  "quotes": [
    {
      "provider": "EASTMONEY",
      "providerQuoteId": "105.AAPL",
      "ticker": "AAPL",
      "displayName": "苹果",
      "currency": "USD",
      "currentPrice": 333.02000000,
      "priceStatus": "LIVE",
      "priceObservedAt": "2026-07-25T00:00:00Z"
    }
  ],
  "unavailable": []
}
```

`providerQuoteIds` 中某个标的不可用时，不应让整个响应失败；该项进入 `unavailable`，并在 portfolio summary 中体现为 `PARTIAL` 或 `UNAVAILABLE`。

### 4.9 查询基金穿透

```http
GET /api/v1/funds/{instrumentId}/lookthrough
```

该接口用于解释 `ETF` 或 `MUTUAL_FUND` 的底层持仓。基金穿透只用于提示和分析，不改变基金本身的持仓数量、成本或估值。

成功响应：`200 OK`

```json
{
  "fundInstrumentId": 102,
  "ticker": "VOO",
  "displayName": "Vanguard S&P 500 ETF",
  "assetType": "ETF",
  "asOfDate": "2026-06-30",
  "source": "DEMO_DISCLOSURE",
  "coveragePercent": 41.15000000,
  "holdings": [
    {
      "ticker": "AAPL",
      "displayName": "Apple Inc.",
      "assetType": "STOCK",
      "weightPercent": 7.12000000
    }
  ],
  "warnings": [
    "Fund holdings are based on the latest available disclosure and may lag current positions."
  ]
}
```

- 已知演示基金按 ticker 返回披露样例。
- 真实数据库中的基金 instrument id 也可查询；未知基金返回空持仓和 warning。
- 非基金或不存在的 instrument 返回 `404 FUND_LOOKTHROUGH_NOT_FOUND`。

### 4.10 查询组合穿透暴露

```http
GET /api/v1/portfolio/exposure?lookthrough=true&priceMode=DEMO_ALLOWED
```

可选查询参数：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `lookthrough` | `false` | `true` 时将基金披露持仓拆到底层资产，并保留未披露残余 |
| `priceMode` | `BEST_AVAILABLE` | 与 holdings/summary 相同 |

成功响应：`200 OK`

```json
{
  "portfolioId": 1,
  "portfolioName": "My Portfolio",
  "baseCurrency": "USD",
  "lookthrough": true,
  "priceMode": "DEMO_ALLOWED",
  "totalMarketValue": 2500.00000000,
  "items": [
    {
      "ticker": "AAPL",
      "displayName": "Apple Inc.",
      "assetType": "STOCK",
      "directMarketValue": 1500.00000000,
      "fundLookthroughMarketValue": 100.00000000,
      "totalExposureValue": 1600.00000000,
      "exposurePercent": 64.00000000,
      "sources": ["DIRECT", "FUND:VOO"]
    }
  ],
  "warnings": [
    "AAPL appears both as direct holding and inside fund holdings."
  ]
}
```

前端可用该接口在 Dashboard 或 Analysis 页面提示基金与直持股票重叠；不要在浏览器里自行拆分基金权重。

### 4.11 健康检查

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

### 4.12 组合分析

```http
POST /api/v1/portfolio/analysis
```

请求体：

```json
{
  "baseCurrency": "CNY",
  "holdings": [
    { "ticker": "600519", "assetType": "STOCK", "quantity": 100, "marketValue": 180000, "costBasis": 150000 }
  ]
}
```

成功响应：`200 OK`，字段说明：

| 字段 | 说明 |
| --- | --- |
| `overview` | 总市值 + 按资产类型的配置占比 |
| `concentration` | HHI 集中度指数、最大持仓、Top5 占比、风险等级 |
| `fundOverlap` | 基金重仓股与直接持股的重叠情况 |
| `lookThrough` | 基金穿透后按标的合并的有效敞口及穿透后 HHI |
| `sectorExposure` | 穿透后按行业汇总的占比及行业 HHI |
| `profitLoss` | 每笔持仓的浮动盈亏及百分比收益率 |
| `llmInsights` | AI 基于以上数字生成的解读文本（JSON），仅解读不重新计算 |
| `warning` | AI 生成失败时的原因（如 `LLM_UNAVAILABLE`）；此时 `llmInsights` 为 `null`，但其余字段仍正常返回 |

这是新增接口，不影响 3.1/3.2 已有的 `Holding`/`PortfolioSummary` 数据模型。

> TODO：以上响应字段拆分较细（6 个独立结果对象），后续计划根据前端实际使用情况做简化/合并。

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
- `CASH` 和 `BANK_DEPOSIT` 固定按 `currentPrice = 1.00000000` 计入总市值和 allocation 分母。
- `MUTUAL_FUND` 使用最新可用净值或演示净值；如果净值缺失，按 `UNAVAILABLE` 处理。
- `totalCostBasis` 包括全部持仓成本，便于用户理解投入；若摘要为部分估值，UI 必须明确提示总盈亏并非完整组合结果。
- 中间计算不提前舍入，API 最多返回 8 位小数，UI 再按币种规则展示。
- MVP 的表现只是当前快照未实现盈亏，不是时间加权或资金加权收益。

## 6. 一致性、缓存与失败处理

- 创建、修改和删除持仓成功后，前端重新获取 holdings 和 summary，不在客户端自行推算权威结果。
- 市场价格调用设置短超时，并通过适配器与核心持仓服务隔离。
- `BEST_AVAILABLE` 顺序建议为：新鲜外部价格、有效缓存、明确允许的演示价格、不可用。
- 演示价格永远返回 `priceStatus = DEMO`，页面必须可见地标注。
- 外部价格失败不能回滚已经成功保存的持仓。
- 未预期异常写入带 `traceId` 的服务日志，对外只返回通用错误。

## 7. 后续扩展接口

以下接口不属于两天编码 MVP，只有 P0 全部完成后才进入新 Story：

| 方法与路径 | 用途 |
| --- | --- |
| `GET /api/v1/portfolios` | 多组合列表 |
| `POST /api/v1/portfolios` | 创建组合 |
| `GET /api/v1/portfolios/{id}/transactions` | 查询不可变交易流水 |
| `POST /api/v1/portfolios/{id}/transactions` | 记录买入、卖出、股息等交易 |
| `GET /api/v1/portfolios/{id}/performance` | 查询历史估值与表现 |
| `GET /api/v1/portfolios/{id}/insights` | 获取规则型集中度或再平衡提示 |
| `POST /api/v1/portfolio/ai-analysis` | 最后阶段调用大模型生成组合解读，不作为买卖建议 |

API 演进使用 `/api/v1` 保持兼容。新增可选字段属于兼容变更；删除字段、改变字段含义或收紧已有输入规则需要新版本或迁移期。

### 7.1 基金穿透展示规则

基金本身也是一个投资组合，可能持有股票、债券、现金或其他基金。HoldHive 不能把基金简单当成普通股票处理后就结束；当用户添加 `ETF` 或 `MUTUAL_FUND` 时，前端应提示“基金可能包含股票，可能与已有直持股票形成重叠暴露”。穿透信息不得写进主持仓表，也不得改变基金本身的估值结果，它只用于解释和分析。

当前可用接口见 4.9 和 4.10。前端实现时遵守：

- 新增或编辑 `ETF`、`MUTUAL_FUND` 时显示基金重叠风险提示。
- Dashboard 或 Analysis 页面使用 `GET /api/v1/portfolio/exposure?lookthrough=true` 展示穿透暴露，不在浏览器里自行计算基金权重。
- `sources` 包含 `DIRECT` 和 `FUND:{ticker}` 时，UI 应显示“直接持仓 + 基金内含”的合并含义。
- `warnings` 直接展示为用户友好的提示；不要把基金披露缺失解释为接口失败。
- 穿透数据通常有披露滞后，界面应展示 `source` 或 warning，不把它当作实时持仓。

### 7.2 大模型组合分析接口

大模型分析放在最后阶段。它只读取后端整理后的结构化组合快照、基金穿透摘要和风险提示，不直接读取数据库表、不调用第三方行情、不保存用户密钥。输出必须带免责声明：结果用于解释当前组合结构，不构成投资建议。

```http
POST /api/v1/portfolio/ai-analysis
Content-Type: application/json
```

请求：

```json
{
  "portfolioId": 1,
  "includeFundLookthrough": true,
  "language": "zh-CN"
}
```

成功响应：

```json
{
  "portfolioId": 1,
  "generatedAt": "2026-07-24T08:30:00Z",
  "provider": "LLM_PROVIDER",
  "model": "configured-model-name",
  "disclaimer": "This analysis is educational and does not constitute investment advice.",
  "summary": "Your portfolio is diversified across stocks, funds, crypto, cash and deposits, but fund lookthrough shows some overlap with direct stock holdings.",
  "keyFindings": [
    {
      "title": "Fund overlap",
      "detail": "AAPL appears directly and inside VOO, so effective exposure is higher than the direct holding alone."
    },
    {
      "title": "Liquidity buffer",
      "detail": "Cash and bank deposits provide stable value but reduce growth exposure."
    }
  ],
  "dataLimitations": [
    "Fund holdings may lag the latest disclosure.",
    "Crypto prices may use demo or cached data in MVP."
  ]
}
```

安全规则：

- API key 只放在后端环境变量，例如 `LLM_API_KEY`，不进入前端。
- 后端向大模型发送最小必要字段，不发送用户姓名、账户号、数据库连接信息或原始异常。
- 输出必须经过后端 schema 校验；无法解析时返回可理解错误，不把原始模型输出直接展示给用户。
- 前端必须把大模型结论标记为“解释性分析”，不能显示成买入、卖出或收益预测。

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
