# HoldHive 实时股价 API 方案

## 1. 决策摘要

本项目需要的是“足够稳定、可解释、适合本地联调”的报价能力，不是交易级行情系统。前端不直接请求第三方行情接口，统一请求后端；后端通过 `PricingAdapter` 对接外部行情、缓存和演示数据。

推荐路径：

1. **MVP 免费主路径：东方财富公共行情接口**。已在本机验证可返回 A 股、美股和港股示例；无需 API key，适合本项目快速联调。
2. **免费 fallback：腾讯行情接口或新浪行情接口**。当东方财富不可用时，仅用于 A 股备用解析。
3. **免费官方备选：Tiingo Starter**。需要注册 token，官方额度是 `50 requests/hour`、`1,000 requests/day`；它是免费且合规的备选，但要把缓存 TTL 调到 90-120 秒，避免四人频繁刷新触发小时额度。
4. **可选研究/后备：AKShare / AKTools**。AKShare 覆盖面广，适合调研和手工验证；AKTools 可把 AKShare 暴露为 HTTP API。但它会引入 Python/FastAPI/Uvicorn 运行时，不适合作为当前 Java 后端两天编码的默认主链路。
5. **不进入主链路：Alpha Vantage 免费档和 Yahoo Finance 网页接口**。Alpha Vantage 官方免费额度只有 `25 requests/day`，明显不适合联调；本机在 2026-07-25 调用 Yahoo Chart 接口返回 `Edge: Too Many Requests`，也不满足稳定要求。
6. **付费只作为长期选项**。Tiingo Power、Finnhub 付费/团队 token 或其他商业 API 适合公开部署或长期运行，不是本项目默认方案。

所有外部行情都必须经过后端缓存。前端只能调用 HoldHive 自己的 `/api/v1/market/*` 或 `/api/v1/portfolio/summary`，避免暴露 token、绕过缓存或被 CORS/限流影响。

“频率不太低”的项目标准定义为：四人本地联调时，Dashboard 每 30-60 秒刷新一次、搜索框带防抖、手动刷新有冷却，不能因为几轮演示就触发 429。按这个标准，**东方财富公共接口 + 后端批量请求 + 30-60 秒缓存** 是最佳免费方案；如果必须使用官方免费 token，Tiingo Starter 也可用，但缓存建议提高到 90-120 秒。Alpha Vantage 免费档只有 25 次/日，排除出主链路。

## 2. 候选接口与使用结论

| 接口 | 覆盖范围 | 使用结论 | 频率判断 |
| --- | --- | --- | --- |
| [东方财富 push2 批量报价](https://push2.eastmoney.com/api/qt/ulist.np/get?fltt=2&invt=2&fields=f12,f14,f2,f3,f4,f5,f6,f13,f15,f16,f17,f18,f20,f21,f124&secids=1.600519,0.000001,105.AAPL) | A 股、美股，港股需结合单标的接口验证 | **P0 推荐**，作为默认实时/准实时报价源 | 无需 key，未公开正式 SLA；本项目必须缓存并限制后端请求 |
| [东方财富单标的报价](https://push2.eastmoney.com/api/qt/stock/get?fltt=2&invt=2&fields=f43,f44,f45,f46,f47,f48,f49,f57,f58,f59,f60,f86,f107&secid=105.AAPL) | 单只证券，适合搜索后校验 | **P0 推荐**，用于新增持仓时校验代码和名称 | 无需 key，适合低频校验 |
| [东方财富搜索建议](https://searchapi.eastmoney.com/api/suggest/get?input=AAPL&type=14&token=D43BF722C8E33BD5A6040B1F8FD653E5) | ticker/name 到 `QuoteID` 映射 | **P0 推荐**，把用户输入转成 provider quote id | 用户输入时防抖，不在每次键盘输入都请求 |
| [腾讯 A 股行情](https://qt.gtimg.cn/q=sh600519,sz000001) | A 股 | **P1 fallback**，解析成本略高 | 无需 key；仅失败兜底 |
| [新浪 A 股行情](https://hq.sinajs.cn/list=sh600519,sz000001) | A 股 | **P1 fallback**，请求建议带 Referer | 无需 key；仅失败兜底 |
| [Tiingo Equity Realtime / IEX API](https://www.tiingo.com/documentation/equity-realtime-stock-data) | 美股和 ETF，需 token | **官方免费备选**，适合需要正式 API 条款时使用 | Starter 免费但只有 50/hour、1,000/day；必须缓存 |
| [Tiingo Pricing](https://www.tiingo.com/about/pricing) | 额度参考 | **选型依据** | Starter 为 50/hour、1,000/day；Power 为 10,000/hour、100,000/day |
| [AKShare](https://akshare.akfamily.xyz/introduction.html) | A 股、港股、美股、基金、期货、指数等多类财经数据 | **P2 可选研究/后备**，不作为默认 Java 主链路 | Python 库，主要用于学术研究；接口依赖公开数据源，需经常跟进版本 |
| [AKTools HTTP](https://akshare.akfamily.xyz/deploy_http.html) | 将 AKShare 封装成本地 HTTP API | **P2 可选**，仅在团队接受额外 Python 服务时使用 | 依赖 AKTools、AKShare、FastAPI、Uvicorn、Typer；增加联调与启动复杂度 |
| [Alpha Vantage Free API](https://www.alphavantage.co/support/) | 美股等，需 key | **不推荐主链路** | 免费仅 25/day，不满足联调刷新 |
| [Finnhub Stock Quote API](https://finnhub.io/docs/api/quote) | 美股、部分全球市场，需 token | **长期备选**，接口简单 | 超出套餐会 429，另有 30 calls/second 上限；免费额度需以账号页为准 |
| Yahoo Chart / Quote 网页接口 | 美股等 | **不作为默认源** | 本机实测返回 `Too Many Requests`，不满足稳定联调要求 |

## 3. 东方财富接口用法

### 3.1 secid 规则

HoldHive 不应只保存用户输入的 ticker，还应保存 provider quote id，避免每次查询都重新搜索。

| 用户输入 | 市场 | 东方财富 `secid` / `QuoteID` | 验证状态 |
| --- | --- | --- | --- |
| `600519.SH` 或 `600519` | 上海 A 股 | `1.600519` | 已验证 |
| `000001.SZ` 或 `000001` | 深圳 A 股 | `0.000001` | 已验证 |
| `AAPL` | 美股 NASDAQ | `105.AAPL` | 已验证 |
| `00700.HK` | 港股 | `116.00700` | 单标的接口已验证；批量接口联调时再确认 |

用户新增持仓时，后端执行：

1. 标准化输入，例如去空格、转大写、识别 `.SH` / `.SZ` / `.HK` 后缀。
2. 可直接映射的 A 股使用规则映射。
3. 美股或不确定市场调用东方财富搜索建议接口，读取 `QuoteID`、`Name`、`JYS` 和 `SecurityTypeName`。
4. 保存 `instrument.provider = EASTMONEY` 和 `instrument.provider_quote_id = 105.AAPL`。
5. 报价失败时仍允许保存持仓，但返回 `priceStatus = UNAVAILABLE`。

### 3.2 批量报价

```http
GET https://push2.eastmoney.com/api/qt/ulist.np/get?fltt=2&invt=2&fields=f12,f14,f2,f3,f4,f5,f6,f13,f15,f16,f17,f18,f20,f21,f124&secids=1.600519,0.000001,105.AAPL
```

关键字段映射：

| 字段 | 含义 | HoldHive 字段 |
| --- | --- | --- |
| `f12` | 证券代码 | `ticker` |
| `f13` | 市场编号 | `providerMarketCode` |
| `f14` | 中文或本地显示名 | `displayName` |
| `f2` | 最新价 | `currentPrice` |
| `f3` | 涨跌幅百分数 | `changePercent` |
| `f4` | 涨跌额 | `changeAmount` |
| `f5` | 成交量 | `volume` |
| `f6` | 成交额 | `turnover` |
| `f15` | 最高价 | `dayHigh` |
| `f16` | 最低价 | `dayLow` |
| `f17` | 开盘价 | `openPrice` |
| `f18` | 昨收价 | `previousClose` |
| `f124` | Unix 秒级行情时间 | `priceObservedAt` |

字段为 `-`、空值或明显无效时，后端应将该持仓报价标记为 `UNAVAILABLE`，不得把未知价格当作 `0`。

### 3.3 单标的报价

```http
GET https://push2.eastmoney.com/api/qt/stock/get?fltt=2&invt=2&fields=f43,f44,f45,f46,f47,f48,f49,f57,f58,f59,f60,f86,f107&secid=105.AAPL
```

关键字段映射：

| 字段 | 含义 | HoldHive 字段 |
| --- | --- | --- |
| `f57` | 证券代码 | `ticker` |
| `f58` | 名称 | `displayName` |
| `f43` | 最新价 | `currentPrice` |
| `f44` | 最高价 | `dayHigh` |
| `f45` | 最低价 | `dayLow` |
| `f46` | 开盘价 | `openPrice` |
| `f60` | 昨收价 | `previousClose` |
| `f86` | Unix 秒级行情时间 | `priceObservedAt` |
| `f107` | 市场编号 | `providerMarketCode` |

单标的接口适合新增持仓时校验一个 ticker 是否存在，或者在批量接口对某市场返回异常时做 fallback。

### 3.4 搜索建议

```http
GET https://searchapi.eastmoney.com/api/suggest/get?input=AAPL&type=14&token=D43BF722C8E33BD5A6040B1F8FD653E5
```

前端搜索框不直接调用该接口。后端提供 `/api/v1/market/search`，并加 300-500ms 防抖和服务端短缓存。返回候选项时只暴露必要字段：

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
  ]
}
```

## 4. 腾讯与新浪 fallback

腾讯接口：

```http
GET https://qt.gtimg.cn/q=sh600519,sz000001
```

腾讯返回 `~` 分隔字符串。MVP 只在东方财富失败时解析：名称、代码、最新价、昨收、开盘、成交量、成交额、行情时间。解析失败时直接降级为缓存或演示价格，不在页面暴露原始字符串。

新浪接口：

```bash
curl -H "Referer: https://finance.sina.com.cn" "https://hq.sinajs.cn/list=sh600519,sz000001"
```

新浪返回逗号分隔字符串。MVP 只作为 A 股备用源，不做港股、美股支持。

## 5. 官方 API 备选

### 5.1 Tiingo Starter 免费备选

Tiingo 提供带 token 的官方 API 文档和价格页。其文档说明使用账号 token 认证，并按小时、天和月度带宽限制；同时明确“不按分钟或秒限流”。价格页显示 Starter 为 `50 requests/hour`、`1,000 requests/day`，Power 为 `10,000 requests/hour`、`100,000 requests/day`。因此 Starter 只适合个人测试，Power 更适合四人小组联调和稳定演示。

如果坚持免费，Tiingo Starter 的使用方式是：

- 只作为东方财富失败时的美股备用源。
- 后端批量合并 symbol，不允许每个组件单独请求。
- `MARKET_CACHE_TTL_SECONDS` 设置为 `90` 或 `120`。
- 手动刷新按钮冷却设置为 `30` 秒。
- 超出额度时立刻降级到缓存或 `DEMO`，不要在前端重复重试。

示例：

```http
GET https://api.tiingo.com/iex/?tickers=AAPL,MSFT&token=${TIINGO_TOKEN}
```

后端配置：

```text
MARKET_PROVIDER=TIINGO
TIINGO_TOKEN=<team-token>
MARKET_CACHE_TTL_SECONDS=30
```

### 5.2 Finnhub

Finnhub 的 Quote API 简单，适合美股价格查询。Finnhub 文档说明 GET 请求可用 `token` 参数或 `X-Finnhub-Token` 认证，超出额度返回 `429`，且所有套餐之上还有 `30 API calls/second` 上限。这个秒级上限不低，但套餐自身额度仍要以账号实际页面为准；多人联调时必须用后端缓存，不能让每个浏览器直接请求 Finnhub。

示例：

```http
GET https://finnhub.io/api/v1/quote?symbol=AAPL&token=${FINNHUB_TOKEN}
```

返回字段通常包括 `c` 最新价、`d` 涨跌额、`dp` 涨跌幅、`h` 最高、`l` 最低、`o` 开盘、`pc` 昨收和 `t` 时间戳。后端必须转换成统一 `MarketQuote`。

### 5.3 Alpha Vantage 不推荐作为主链路

Alpha Vantage 提供免费 API key，但官方支持页说明免费股票 API 只有 `25 requests/day`。这个额度适合一天内少量手工验证，不适合四人开发时反复刷新 Dashboard，因此不进入默认方案。

### 5.4 AKShare / AKTools 判断

结论：可以作为“数据调研工具”或“P2 后备行情服务”，但不建议作为 HoldHive MVP 的默认实时股价源。

理由：

- AKShare 是 Python 财经数据接口库，不是可直接被 Java/Spring Boot 调用的远程官方行情 API。
- 官方说明 AKShare 数据来自公开数据源，主要用于学术研究；网页变化可能导致接口需要持续维护和升级。
- AKTools 可以将 AKShare 暴露为 HTTP API，但会额外引入 Python、AKTools、FastAPI、Uvicorn、Typer 等运行时。当前项目已经明确“不依赖 Docker”，再增加一个 Python HTTP 服务会提高成员本机启动和联调成本。
- 两天编码期内，成员 B 若同时维护 Java pricing adapter 和 Python/AKTools 服务，职责边界会变重，不利于快速稳定交付。

可接受用法：

```text
MARKET_PROVIDER=EASTMONEY
MARKET_FALLBACK_PROVIDERS=TENCENT,SINA,DEMO
# P2 optional only
AKTOOLS_BASE_URL=http://127.0.0.1:8081
```

如果团队后续决定引入 AKTools，必须满足：

1. 成员 D 把 Python 版本、安装命令和启动命令加入 `CONTRIBUTING.md`。
2. 成员 B 只通过 `AkToolsPricingAdapter` 调用本地 HTTP，不在 Java 服务里直接运行 Python 脚本。
3. 失败时仍按 `BEST_AVAILABLE -> cache -> demo -> unavailable` 降级。
4. PR 里明确说明为什么东方财富、腾讯/新浪和 Tiingo 不足以满足当期需求。

## 6. 后端统一接口

第三方接口只由后端访问。前端需要行情时调用 HoldHive 自己的 API。

### 6.1 搜索证券

```http
GET /api/v1/market/search?query=AAPL
```

响应：

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

### 6.2 批量获取报价

```http
GET /api/v1/market/quotes?providerQuoteIds=1.600519,0.000001,105.AAPL&priceMode=BEST_AVAILABLE
```

响应：

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

### 6.3 前端使用规则

- Dashboard 初次加载时请求 `/api/v1/portfolio/summary`，不要并发打第三方行情。
- 添加持仓时可请求 `/api/v1/market/search`，搜索框至少 300ms 防抖。
- 用户手动刷新报价时，请求 `/api/v1/market/quotes` 或刷新 summary；按钮 10 秒冷却。
- 如果后端返回 `DEMO`、`CACHED` 或 `UNAVAILABLE`，UI 必须显示状态标签。

## 7. 缓存、限流和失败策略

### 7.1 后端缓存

| 场景 | TTL 建议 | 说明 |
| --- | --- | --- |
| 交易时段 Dashboard | 30-60 秒 | 足够演示实时感，避免多人刷新打爆接口 |
| 非交易时段 | 5-30 分钟 | 行情不频繁变化，减少请求 |
| 搜索建议 | 10-30 分钟 | ticker 映射基本稳定 |
| 失败后的 last-known price | 最长 24 小时 | 必须标记 `CACHED`，不能伪装为实时 |

### 7.2 后端限流

- 单个 provider 默认最多 `1` 次外部请求/秒。
- 批量报价优先合并请求，一次最多 50 个 `providerQuoteId`。
- 同一用户或同一浏览器手动刷新至少 10 秒间隔。
- 连续 3 次 provider 失败后进入 60 秒熔断，只返回缓存或 `UNAVAILABLE`。

### 7.3 失败降级顺序

```text
BEST_AVAILABLE = fresh external quote
              -> fresh cache
              -> last-known cache with CACHED status
              -> explicit demo quote if DEMO_ALLOWED
              -> UNAVAILABLE
```

任何降级都必须在响应中暴露 `priceStatus`、`provider`、`fetchedAt` 和 `priceObservedAt`。

## 8. 配置建议

```text
MARKET_PROVIDER=EASTMONEY
MARKET_FALLBACK_PROVIDERS=TENCENT,SINA,DEMO
MARKET_CACHE_TTL_SECONDS=45
MARKET_SEARCH_CACHE_TTL_SECONDS=1800
MARKET_REQUEST_TIMEOUT_MS=2500
MARKET_RATE_LIMIT_PER_SECOND=1
MARKET_DEMO_ALLOWED=true
```

如果使用官方 token 接口：

```text
MARKET_PROVIDER=TIINGO
TIINGO_TOKEN=<team-token>
FINNHUB_TOKEN=<team-token-if-used>
```

不要把 token 写入 Git。仓库只提交 `.env.example`，真实值由每位成员本机配置。

## 9. 测试与验收

后端测试不得依赖真实行情服务。真实接口只在手工联调或专门的 integration profile 下运行。

必须覆盖：

- 东方财富正常返回 A 股、美股报价时能转换成统一 DTO。
- 单只证券价格为 `-`、空或缺字段时返回 `UNAVAILABLE`。
- Provider 超时或返回非 JSON 时，服务不崩溃，并按降级顺序返回缓存或不可用状态。
- 前端不会直接访问第三方 URL。
- 手动刷新按钮有冷却，重复点击不会触发多次后端刷新。
- 演示价格永远显示 `DEMO`，缓存价格永远显示 `CACHED`。

## 10. 实测记录

实测时间：2026-07-25，地点：当前本机网络。

| 接口 | 示例 | 结果 |
| --- | --- | --- |
| 东方财富批量报价 | `1.600519,0.000001,105.AAPL` | 返回 `rc=0` 和最新价字段 |
| 东方财富单标的 | `105.AAPL`、`116.00700` | 返回 `rc=0` 和最新价字段 |
| 东方财富搜索 | `AAPL`、`00700` | 返回 `QuoteID` 映射 |
| 腾讯行情 | `sh600519,sz000001` | 返回行情字符串 |
| 新浪行情 | `sh600519,sz000001` | 返回行情字符串 |
| Yahoo Chart | `AAPL` | 返回 `Edge: Too Many Requests`，不作为默认源 |

以上公共网页接口没有正式 SLA。它们满足本项目本地训练和演示要求，但不适合公开生产服务。若项目需要公开部署或长期稳定运行，必须改用 Tiingo、Finnhub 或其他正式商业行情服务。

## 11. 最终推荐

两天编码项目按以下免费优先组合实现，兼顾可用性和请求频率：

| 场景 | 推荐实现 | 原因 |
| --- | --- | --- |
| 默认本地演示 | 东方财富 `search + stock/get + ulist` | 本机已验证，无 key，A 股/美股/港股覆盖足够演示 |
| A 股备用 | 腾讯或新浪接口 | 返回简单，作为东方财富失败兜底 |
| 免费官方备用 | Tiingo Starter | 免费且有官方文档；用 90-120 秒缓存规避 50/hour 限制 |
| 可选研究/后备 | AKShare / AKTools | 覆盖面广，但引入 Python HTTP 服务和学术研究用途限制，不作为默认主链路 |
| 不推荐免费源 | Alpha Vantage 免费档 | 25/day 太低，不适合联调 |
| 长期/公开部署 | Tiingo Power 或 Finnhub 团队 token | 有文档、认证和明确额度，适合项目需要正式 API 时切换 |
| 断网或 provider 失败 | DemoProvider + MySQL 缓存 | 保证展示不被外部服务阻断 |

不要在前端直接访问 Yahoo、东方财富、腾讯、新浪、Tiingo 或 Finnhub。所有第三方请求统一进后端 `PricingAdapter`，并通过 `/api/v1/market/search`、`/api/v1/market/quotes` 和 `/api/v1/portfolio/summary` 暴露给前端。
