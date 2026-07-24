好的，我为“投资组合管理器”项目设计一份完整的 REST API 接口文档。这份文档可以直接用于团队开发，也方便前后端联调。

---

# 投资组合管理器 REST API 文档

## 文档信息

| 项目 | 内容 |
| :--- | :--- |
| **项目名称** | PortfolioHub |
| **API 版本** | v1 |
| **基础路径** | `/api/v1` |
| **数据格式** | JSON |
| **字符编码** | UTF-8 |

---

## 1. 通用规范

### 1.1 统一响应格式

所有接口返回统一的 JSON 结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {  },
  "timestamp": "2026-07-24T14:30:00Z"
}
```

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `code` | Integer | 状态码，200 表示成功 |
| `message` | String | 响应消息 |
| `data` | Object/Array | 响应数据（成功时返回） |
| `timestamp` | String | 响应时间戳（ISO 8601 格式） |

### 1.2 常见 HTTP 状态码

| 状态码 | 含义 |
| :--- | :--- |
| 200 OK | 请求成功 |
| 201 Created | 资源创建成功 |
| 400 Bad Request | 请求参数错误 |
| 404 Not Found | 资源不存在 |
| 409 Conflict | 资源冲突（如重复添加） |
| 500 Internal Server Error | 服务器内部错误 |

### 1.3 错误响应格式

```json
{
  "code": 400,
  "message": "参数验证失败",
  "errors": [
    {
      "field": "quantity",
      "message": "数量必须大于0"
    }
  ],
  "timestamp": "2026-07-24T14:30:00Z"
}
```

---

## 2. 持仓管理接口 (Holdings)

### 2.1 获取所有持仓

**GET** `/portfolio/holdings`

获取当前投资组合中的所有持仓，包含实时价格和盈亏计算。

**请求参数**（Query String）：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `sortBy` | String | 否 | `id` | 排序字段：`id`、`ticker`、`quantity`、`currentPrice` |
| `direction` | String | 否 | `asc` | 排序方向：`asc` 或 `desc` |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "holdings": [
      {
        "id": 1,
        "ticker": "AAPL",
        "companyName": "Apple Inc.",
        "quantity": 100,
        "purchasePrice": 150.50,
        "currentPrice": 175.20,
        "marketValue": 17520.00,
        "totalCost": 15050.00,
        "profitLoss": 2470.00,
        "profitLossPercent": 16.41,
        "purchaseDate": "2026-01-15",
        "sector": "Technology",
        "lastUpdated": "2026-07-24T10:00:00Z"
      }
    ],
    "totalCount": 10,
    "totalMarketValue": 125000.00,
    "totalProfitLoss": 8500.00
  },
  "timestamp": "2026-07-24T14:30:00Z"
}
```

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | Long | 持仓ID |
| `ticker` | String | 股票代码 |
| `companyName` | String | 公司名称 |
| `quantity` | Integer | 持有数量 |
| `purchasePrice` | BigDecimal | 买入价（每股） |
| `currentPrice` | BigDecimal | 当前价（每股） |
| `marketValue` | BigDecimal | 市值 = 数量 × 当前价 |
| `totalCost` | BigDecimal | 总成本 = 数量 × 买入价 |
| `profitLoss` | BigDecimal | 盈亏 = 市值 - 总成本 |
| `profitLossPercent` | BigDecimal | 盈亏百分比 |
| `purchaseDate` | String | 买入日期 |
| `sector` | String | 所属行业 |
| `lastUpdated` | String | 价格最后更新时间 |

---

### 2.2 获取单个持仓

**GET** `/portfolio/holdings/{id}`

根据 ID 获取指定持仓的详细信息。

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `id` | Long | 是 | 持仓ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "ticker": "AAPL",
    "companyName": "Apple Inc.",
    "quantity": 100,
    "purchasePrice": 150.50,
    "currentPrice": 175.20,
    "marketValue": 17520.00,
    "totalCost": 15050.00,
    "profitLoss": 2470.00,
    "profitLossPercent": 16.41,
    "purchaseDate": "2026-01-15",
    "sector": "Technology",
    "lastUpdated": "2026-07-24T10:00:00Z"
  },
  "timestamp": "2026-07-24T14:30:00Z"
}
```

**错误响应**（404）：

```json
{
  "code": 404,
  "message": "持仓不存在，ID: 999",
  "timestamp": "2026-07-24T14:30:00Z"
}
```

---

### 2.3 新增持仓

**POST** `/portfolio/holdings`

向投资组合中添加一支新的持仓。

**请求体**（JSON）：

```json
{
  "ticker": "AAPL",
  "companyName": "Apple Inc.",
  "quantity": 100,
  "purchasePrice": 150.50,
  "purchaseDate": "2026-01-15",
  "sector": "Technology"
}
```

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `ticker` | String | 是 | 股票代码，3-10位大写字母 |
| `companyName` | String | 否 | 公司名称 |
| `quantity` | Integer | 是 | 持有数量，必须 > 0 |
| `purchasePrice` | BigDecimal | 是 | 买入价（每股），必须 > 0 |
| `purchaseDate` | String | 否 | 买入日期，格式 yyyy-MM-dd，默认为当天 |
| `sector` | String | 否 | 所属行业 |

**响应示例**（201 Created）：

```json
{
  "code": 201,
  "message": "持仓创建成功",
  "data": {
    "id": 1,
    "ticker": "AAPL",
    "companyName": "Apple Inc.",
    "quantity": 100,
    "purchasePrice": 150.50,
    "currentPrice": 150.50,
    "marketValue": 15050.00,
    "totalCost": 15050.00,
    "profitLoss": 0.00,
    "profitLossPercent": 0.00,
    "purchaseDate": "2026-01-15",
    "sector": "Technology",
    "lastUpdated": "2026-07-24T14:30:00Z"
  },
  "timestamp": "2026-07-24T14:30:00Z"
}
```

**错误响应**（409 Conflict）：

```json
{
  "code": 409,
  "message": "股票 AAPL 已存在于投资组合中，请使用更新接口修改数量",
  "timestamp": "2026-07-24T14:30:00Z"
}
```

---

### 2.4 更新持仓

**PUT** `/portfolio/holdings/{id}`

更新指定持仓的数量或买入价。

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `id` | Long | 是 | 持仓ID |

**请求体**（JSON）：

```json
{
  "quantity": 150,
  "purchasePrice": 160.00
}
```

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `quantity` | Integer | 否 | 新的持有数量 |
| `purchasePrice` | BigDecimal | 否 | 新的买入价 |

> **说明**：至少需要提供一个字段。如果都不提供，返回 400。

**响应示例**：

```json
{
  "code": 200,
  "message": "持仓更新成功",
  "data": {
    "id": 1,
    "ticker": "AAPL",
    "companyName": "Apple Inc.",
    "quantity": 150,
    "purchasePrice": 160.00,
    "currentPrice": 175.20,
    "marketValue": 26280.00,
    "totalCost": 24000.00,
    "profitLoss": 2280.00,
    "profitLossPercent": 9.50,
    "purchaseDate": "2026-01-15",
    "sector": "Technology",
    "lastUpdated": "2026-07-24T14:30:00Z"
  },
  "timestamp": "2026-07-24T14:30:00Z"
}
```

---

### 2.5 删除持仓

**DELETE** `/portfolio/holdings/{id}`

从投资组合中移除指定持仓。

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `id` | Long | 是 | 持仓ID |

**响应示例**（200 OK）：

```json
{
  "code": 200,
  "message": "持仓已成功删除",
  "data": null,
  "timestamp": "2026-07-24T14:30:00Z"
}
```

---

## 3. 投资组合总览接口 (Summary)

### 3.1 获取投资组合总览

**GET** `/portfolio/summary`

获取投资组合的整体统计分析数据，用于仪表板展示。

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalMarketValue": 125000.00,
    "totalCost": 116500.00,
    "totalProfitLoss": 8500.00,
    "totalProfitLossPercent": 7.30,
    "holdingCount": 10,
    "sectorDistribution": [
      {
        "sector": "Technology",
        "value": 52500.00,
        "percentage": 42.0
      },
      {
        "sector": "Healthcare",
        "value": 31200.00,
        "percentage": 24.96
      },
      {
        "sector": "Finance",
        "value": 25000.00,
        "percentage": 20.0
      },
      {
        "sector": "Other",
        "value": 16300.00,
        "percentage": 13.04
      }
    ],
    "topPerformers": [
      {
        "ticker": "NVDA",
        "profitLossPercent": 45.2,
        "companyName": "NVIDIA Corporation"
      },
      {
        "ticker": "AAPL",
        "profitLossPercent": 16.4,
        "companyName": "Apple Inc."
      }
    ],
    "worstPerformers": [
      {
        "ticker": "DIS",
        "profitLossPercent": -12.5,
        "companyName": "Walt Disney Co."
      }
    ]
  },
  "timestamp": "2026-07-24T14:30:00Z"
}
```

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `totalMarketValue` | BigDecimal | 总市值 |
| `totalCost` | BigDecimal | 总成本 |
| `totalProfitLoss` | BigDecimal | 总盈亏 |
| `totalProfitLossPercent` | BigDecimal | 总收益率 |
| `holdingCount` | Integer | 持仓数量 |
| `sectorDistribution` | Array | 行业分布数据 |
| `sectorDistribution[].sector` | String | 行业名称 |
| `sectorDistribution[].value` | BigDecimal | 该行业市值 |
| `sectorDistribution[].percentage` | BigDecimal | 该行业占比（%） |
| `topPerformers` | Array | 表现最好的前3支股票 |
| `worstPerformers` | Array | 表现最差的前3支股票 |

---

## 4. 业绩走势接口 (Performance)

### 4.1 获取历史业绩走势

**GET** `/portfolio/performance`

获取投资组合在指定时间段内的历史价值变化数据，用于折线图展示。

**请求参数**（Query String）：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| `period` | String | 否 | `1M` | 时间周期：`1W`（一周）、`1M`（一月）、`3M`（三月）、`6M`（半年）、`1Y`（一年） |
| `startDate` | String | 否 | - | 开始日期，格式 yyyy-MM-dd（若提供，则忽略 period） |
| `endDate` | String | 否 | 当天 | 结束日期，格式 yyyy-MM-dd |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "startDate": "2026-06-24",
    "endDate": "2026-07-24",
    "dataPoints": [
      {
        "date": "2026-06-24",
        "value": 118000.00
      },
      {
        "date": "2026-07-01",
        "value": 120500.00
      },
      {
        "date": "2026-07-08",
        "value": 119200.00
      },
      {
        "date": "2026-07-15",
        "value": 123000.00
      },
      {
        "date": "2026-07-22",
        "value": 124500.00
      },
      {
        "date": "2026-07-24",
        "value": 125000.00
      }
    ],
    "totalReturn": 7000.00,
    "totalReturnPercent": 5.93
  },
  "timestamp": "2026-07-24T14:30:00Z"
}
```

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `startDate` | String | 数据起始日期 |
| `endDate` | String | 数据结束日期 |
| `dataPoints` | Array | 每日数据点 |
| `dataPoints[].date` | String | 日期 |
| `dataPoints[].value` | BigDecimal | 当天总资产价值 |
| `totalReturn` | BigDecimal | 期间总收益 |
| `totalReturnPercent` | BigDecimal | 期间收益率（%） |

---

## 5. 价格刷新接口 (Price Refresh)

### 5.1 刷新所有持仓价格

**POST** `/portfolio/refresh-prices`

手动触发从 Yahoo Finance 获取所有持仓的最新价格。

**响应示例**：

```json
{
  "code": 200,
  "message": "价格刷新成功",
  "data": {
    "totalUpdated": 10,
    "successCount": 9,
    "failedCount": 1,
    "failedTickers": ["INVALID"],
    "updatedAt": "2026-07-24T14:35:00Z"
  },
  "timestamp": "2026-07-24T14:35:00Z"
}
```

---

## 6. 接口速查表

| 方法 | 路径 | 功能 | 请求体 |
| :--- | :--- | :--- | :--- |
| GET | `/portfolio/holdings` | 获取所有持仓 | 无 |
| GET | `/portfolio/holdings/{id}` | 获取单个持仓 | 无 |
| POST | `/portfolio/holdings` | 新增持仓 | JSON |
| PUT | `/portfolio/holdings/{id}` | 更新持仓 | JSON |
| DELETE | `/portfolio/holdings/{id}` | 删除持仓 | 无 |
| GET | `/portfolio/summary` | 获取总览数据 | 无 |
| GET | `/portfolio/performance` | 获取业绩走势 | 无 |
| POST | `/portfolio/refresh-prices` | 刷新所有价格 | 无 |

---

## 7. 附录：API 调用示例

### cURL 示例

**新增持仓：**
```bash
curl -X POST http://localhost:8080/api/v1/portfolio/holdings \
  -H "Content-Type: application/json" \
  -d '{
    "ticker": "AAPL",
    "companyName": "Apple Inc.",
    "quantity": 100,
    "purchasePrice": 150.50,
    "purchaseDate": "2026-01-15",
    "sector": "Technology"
  }'
```

**获取所有持仓：**
```bash
curl -X GET "http://localhost:8080/api/v1/portfolio/holdings?sortBy=currentPrice&direction=desc"
```

**获取业绩走势（过去3个月）：**
```bash
curl -X GET "http://localhost:8080/api/v1/portfolio/performance?period=3M"
```

---

这份接口文档涵盖了项目需求中的核心功能，并且前后端都可以基于它并行开发。需要我继续提供 **Spring Data JPA 对应的实体类和 Repository 代码**，还是 **前端调用这些 API 的 TypeScript 类型定义**？