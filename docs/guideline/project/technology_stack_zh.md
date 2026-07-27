# HoldHive 技术栈定案

## 1. 决策摘要

HoldHive 采用前后端分离的模块化单体架构。目标不是追求最多技术，而是在两天编码时间内交付可运行、可测试、可解释的 MVP。

| 范围 | 技术选型 | 用途 |
| --- | --- | --- |
| 前端语言 | TypeScript 5.x | 页面与 API 类型安全 |
| 前端框架 | React 18+ | Dashboard、表单和状态渲染 |
| 前端构建 | Vite 5+ | 本地开发、构建和测试集成 |
| 前端样式 | CSS Modules 或普通 CSS | 控制依赖和样式作用域 |
| 图表 | Recharts 2.x | 资产配置图 |
| 动画 | Framer Motion 或 CSS transition | 页面进入、数字刷新、Toast、表格行变化 |
| 图标 | Lucide React | 导航、按钮、状态标签 |
| 提示反馈 | React Hot Toast 或自建 Toast | 添加、删除、错误和价格状态提示 |
| 数字动画 | react-countup 或轻量自定义 hook | 总市值、盈亏和持仓数变化 |
| 后端语言 | Java 21 LTS | REST API 与业务逻辑 |
| 后端框架 | Spring Boot 3.x | Web、校验、数据访问和配置 |
| 实时股价源 | 东方财富公共接口；Tiingo/Finnhub 为官方备选 | A 股、美股和演示报价 |
| 构建工具 | Maven 3.9+ | 依赖、测试和打包 |
| 数据库 | MySQL 8.4 LTS | 持仓、证券和价格快照 |
| 数据访问 | Spring Data JPA / Hibernate | Repository 与实体映射 |
| 数据库迁移 | Flyway | 版本化建表和数据迁移 |
| API 文档 | springdoc-openapi / Swagger UI | OpenAPI 文档与调试 |
| 后端测试 | JUnit 5、Mockito、AssertJ、MockMvc | 单元与 Web 层测试 |
| 覆盖率 | JaCoCo | 后端行覆盖率门槛 70% |
| 前端测试 | Vitest、React Testing Library | 组件和交互测试 |
| E2E 冒烟测试 | Playwright | 最核心用户流程，时间允许时启用 |
| 本地数据库 | 本机 MySQL 8.x | 每位成员独立启动数据库，不依赖 Docker |
| CI | GitHub Actions | 构建、测试、覆盖率和前端检查 |

版本号以初始化项目当天最新的兼容稳定版本为准，并锁定在依赖文件中。不得在编码最后一天进行大版本升级。

## 2. 选型前置条件

项目规则要求：前端框架应至少有两名队员已经掌握；否则团队需共同学习 3-4 小时并记录过程。

因此 Day 1 必须执行一次技术能力检查：

- 若至少两人能独立使用 React、TypeScript、组件、Props、Hook 和表单事件，使用本文默认方案。
- 若不足两人具备上述能力，前端立即改为原生 HTML、CSS、JavaScript 和 Chart.js；API、后端、数据库与测试方案不变。
- 不允许在 Day 3 才因框架困难切换技术栈。

后端优先采用团队已掌握并能在两天内稳定交付的技术。如果最终后端框架不是 Java/Spring，应在 Day 1 经技术评审方确认后等价替换，并写一条 ADR 说明原因。数据库定为 MySQL；若项目环境的 MySQL 小版本不同，应验证 Flyway 迁移兼容性，而不是临时切换数据库产品。

## 3. 前端技术栈

### 3.1 核心工具

**React + TypeScript + Vite**

- React 负责组件化 Dashboard、持仓表、添加表单、删除确认和状态反馈。
- TypeScript 为 API DTO、表单值和组件属性提供静态检查。
- Vite 提供快速本地启动和简单构建配置。

**样式：CSS Modules 或普通 CSS**

本期不引入大型 UI 框架。团队建立少量 CSS 变量统一颜色、间距、字体和边框，通过原生语义 HTML 保证表单和表格可访问。

**图表：Recharts**

只实现一个资产配置环形图。Recharts 与 React 组件模型一致，适合直接根据 summary allocations 渲染。必须同时提供文字图例，不能让图表成为唯一的信息载体。

### 3.2 前端额外库建议

这些库不是必须全部引入，但它们能直接改善图表、用户提示和现代交互体验。两天编码时间内建议只选必要项，不堆依赖。

| 能力 | 推荐库 | 是否建议 | 用途 |
| --- | --- | --- | --- |
| 资产配置 donut | Recharts | P0 推荐 | 与 React 数据流一致，快速实现 Pie/Donut、Legend、Tooltip |
| 组合趋势 line/area | Recharts | P1 推荐 | 如果有演示历史数据，用 AreaChart 展示组合价值趋势 |
| 原生 JS 备选图表 | Chart.js | 备选 | 如果放弃 React，则用 Chart.js 实现 donut 和 line |
| 页面/卡片过渡 | Framer Motion | P1 推荐 | `AnimatePresence`、卡片进入、表格行添加删除过渡 |
| 简单过渡 | CSS transition/keyframes | P0 推荐 | 没时间引入 Framer Motion 时，用 CSS 完成 hover、fade、highlight |
| 图标 | lucide-react | P0 推荐 | Dashboard、Holdings、Plus、Trash、Alert、Refresh 等图标 |
| Toast | react-hot-toast | P0 推荐 | 添加成功、删除成功、API 失败、演示价格提示 |
| 数字变化 | react-countup 或自定义 hook | P1 推荐 | 总市值、盈亏、持仓数刷新时更直观 |
| 精确数值格式化 | Intl.NumberFormat | P0 必须 | 金额、百分比和币种展示；优先原生 API |
| 日期格式化 | Intl.DateTimeFormat 或 date-fns | P1 可选 | 价格更新时间和图表横轴；简单场景优先原生 API |

推荐前端最小依赖组合：

```text
React + TypeScript + Vite
Recharts
lucide-react
react-hot-toast
CSS Modules / plain CSS
```

如果 Day 3 仍未完成 P0，不再引入 Framer Motion、react-countup 或 Playwright，优先保证 CRUD、图表和联调稳定。

### 3.3 前端不采用的技术

- 不使用 Redux：MVP 状态范围小，使用组件状态和自定义 Hook 足够。
- 不使用 Next.js：本期不需要 SSR、SEO 或复杂路由。
- 不同时使用多个组件库或图表库。
- 不为单页 Dashboard 引入复杂路由。
- 不在前端重复实现后端的估值公式。

### 3.4 建议目录

```text
frontend/
├── src/
│   ├── api/             # HTTP 客户端和 DTO
│   ├── components/      # 通用展示与交互组件
│   ├── features/
│   │   └── portfolio/   # Dashboard、持仓、摘要和图表
│   ├── hooks/           # 数据加载与交互 Hook
│   ├── styles/          # 全局变量和基础样式
│   ├── test/            # 测试初始化和 fixtures
│   ├── App.tsx
│   └── main.tsx
├── package.json
└── vite.config.ts
```

### 3.5 前端数据策略

- 使用浏览器 `fetch` 的薄封装，不额外引入 Axios。
- API 类型集中放在 `src/api`，组件不得直接拼接 URL。
- 页面加载时并行请求 holdings 和 summary，分别处理失败状态。
- 创建或删除成功后重新请求服务端数据，避免客户端自行计算权威市值。
- 表单提交期间禁用按钮，防止重复请求。

### 3.6 前端依赖边界

前端依赖方向必须保持单向：页面组合组件调用 feature hook，feature hook 调用 `src/api`，`src/api` 再调用后端 REST API。底层模块不得反向依赖上层页面，也不得让通用组件知道 portfolio 业务细节。

```text
App.tsx
  -> features/portfolio/PortfolioDashboard
  -> features/portfolio/hooks
  -> api/portfolioApi + api/types
  -> backend REST API

components/
  -> styles/
  -> no business API calls
```

具体规则：

- `components/` 只能放可复用 UI，不直接读取 portfolio 数据，不调用 `fetch`。
- `features/portfolio/components/` 可以展示持仓、摘要和图表，但数据获取统一走 `features/portfolio/hooks/`。
- `features/portfolio/hooks/` 负责加载、刷新、提交和删除动作，不直接处理 DOM 或样式。
- `api/` 只负责 HTTP、DTO 和错误转换，不包含 React 组件或图表逻辑。
- `styles/` 只提供 token、主题和基础样式，不引入业务字段。
- 图表组件接收已经整理好的 allocation 数据，不自己计算权威市值；权威计算来自后端 summary。
- 新增库时优先放在使用范围最小的模块内，不因为一个页面需求把依赖扩散到全局。

## 4. 后端技术栈

### 4.1 核心工具

**Java 21 + Spring Boot 3 + Maven**

- Spring Web 提供 REST Controller。
- Spring Validation 处理请求字段校验。
- Spring Data JPA 提供持久化访问。
- Flyway 管理数据库版本，保证四名成员和 CI 使用同一套建表、改表和 seed 流程。
- springdoc-openapi 生成 Swagger UI。

采用模块化单体，不拆微服务。外部价格服务通过接口和适配器隔离，以便在真实数据、缓存和演示价格之间切换。实时股价接口、链接、用法、缓存和限流策略见 [实时股价 API 方案](./market_data_api_zh.md)。

### 4.2 建议包结构

```text
backend/src/main/java/.../holdhive/
├── portfolio/
│   ├── api/             # Controller、请求和响应 DTO
│   ├── application/     # 用例服务和事务边界
│   ├── domain/          # 业务对象、计算和领域异常
│   └── persistence/     # JPA Entity 和 Repository
├── pricing/
│   ├── application/     # 价格查询服务
│   ├── domain/          # PriceQuote、PriceStatus
│   └── infrastructure/  # 外部 API、缓存、演示适配器
├── common/
│   ├── error/           # 统一异常响应
│   └── config/          # CORS、OpenAPI 等配置
└── HoldHiveApplication.java
```

按业务能力而非传统全局 `controller/service/repository` 大目录拆分，使 portfolio 和 pricing 可以独立理解与测试。

### 4.3 后端职责边界

- Controller 只负责 HTTP 映射、请求校验和响应状态。
- Application Service 负责用例编排、事务和权限边界。
- Domain 负责估值公式和业务规则，不依赖 HTTP 或数据库。
- Repository 负责持久化，不承载业务计算。
- Pricing Adapter 负责外部调用和数据转换，不修改持仓。
- 所有金额使用 `BigDecimal`，禁止使用 `double`。

### 4.4 后端依赖方向

后端采用模块化单体，但模块内必须保持清晰依赖方向。外层可以依赖内层，内层不能反向依赖外层；接口定义放在靠近业务调用方的位置，实现放在基础设施层。

```text
api
  -> application
  -> domain

application
  -> persistence repository interface / Spring Data repository
  -> pricing application interface

pricing application
  -> pricing domain
  -> pricing infrastructure adapter

persistence
  -> entity / database mapping
```

禁止依赖：

- `domain` 禁止 import `org.springframework.*`、`jakarta.persistence.*`、HTTP client 或外部 API SDK。
- `api` 禁止直接写估值公式或直接调用外部行情 API。
- `persistence` 禁止返回 HTTP DTO；它只返回 Entity 或 repository 查询结果。
- `pricing.infrastructure` 禁止调用 HoldingRepository 修改持仓；它只返回报价结果。
- `common` 禁止依赖 portfolio 或 pricing 的业务类，否则会变成隐藏的全局业务层。

推荐做法：

- 复杂计算先放在 `PortfolioCalculator` 这类纯 domain 类中，用普通单元测试覆盖。
- 外部行情通过 `PricingAdapter` 接口隔离，真实实现、演示实现和测试 stub 可替换。
- Controller response DTO 由 mapper 组装，避免把 Entity 直接暴露给前端。
- 每个 service 方法只负责一个用例，例如 create holding、delete holding、get summary，不做“万能服务”。
- 若两个模块需要共享字段，优先通过 DTO 或小型 value object 表达，不共享大型可变对象。

### 4.5 配置策略

使用 Spring Profile：

| Profile | 数据库/价格来源 | 用途 |
| --- | --- | --- |
| `local` | 本机 MySQL + 演示或外部价格 | 本地开发 |
| `test` | H2 MySQL compatibility mode；CI 可另接 GitHub MySQL service | 自动测试 |
| `demo` | MySQL + 固定演示价格 | 稳定演示 |

数据库 URL、用户名、密码和外部 API 配置通过环境变量提供。仓库只提交 `.env.example`，不得提交真实 `.env`。

## 5. 数据库与迁移

默认使用 MySQL 8.4 LTS 和 InnoDB。它提供事务、外键、`CHECK` 约束、`DECIMAL` 定点数和复合索引，能够支持当前持仓模型以及后续交易流水和历史价格。数据库字符集统一为 `utf8mb4`，排序规则在首次迁移中固定，避免不同环境产生字符串比较差异。

Flyway 是数据库结构的唯一版本入口。所有表、索引、约束和初始演示数据都通过 `db/migration` 下的 SQL 文件进入数据库；本地启动、CI 和 QA 环境按相同版本顺序执行 migration，避免手工建表导致环境不一致。

Flyway 迁移建议：

```text
backend/src/main/resources/db/migration/
├── V1__create_portfolio_tables.sql
├── V2__create_price_snapshot_table.sql
└── V3__seed_demo_portfolio.sql
```

- `V3` 只在 `demo` 配置中执行或通过独立演示数据加载器完成。
- 已在共享环境执行的迁移文件不得修改；变更使用新版本迁移。
- 生产代码不得依赖 Hibernate 自动建表，`ddl-auto` 使用 `validate`。

详细表设计见 [数据库设计](./database_design_zh.md)。

## 6. 测试技术栈与分层

### 6.1 后端测试

| 测试层 | 工具 | 测试重点 | 是否为 MVP 必须 |
| --- | --- | --- | --- |
| Domain 单元测试 | JUnit 5 + AssertJ | 市值、成本、盈亏、占比、除零和部分价格 | 必须 |
| Service 单元测试 | JUnit 5 + Mockito | 创建、重复、删除、不存在、价格失败 | 必须 |
| Controller 测试 | MockMvc | 状态码、校验、JSON、统一错误结构 | 必须 |
| Repository 测试 | `@DataJpaTest` | 唯一约束、查询和映射 | 必须 |
| 数据库集成测试 | 本机 MySQL 或 GitHub Actions MySQL service | Flyway 迁移、约束和 MySQL 特有行为 | 时间允许；至少在合并前运行一次 |

Mockito 用于隔离 Service 层和适配器层依赖，例如 Repository、PricingAdapter 或 HTTP client。它不用于替代数据库约束、Flyway migration、Repository 查询和 Controller JSON 契约测试；这些仍由 `@DataJpaTest`、本机 MySQL 或 CI MySQL service、MockMvc 覆盖。

JaCoCo 在 Maven `verify` 阶段检查后端行覆盖率不低于 70%。覆盖率只是一条门槛，PR 仍需人工检查关键分支是否真正被验证。

推荐命令：

```bash
cd backend
./mvnw clean verify
```

### 6.2 前端测试

使用 Vitest、React Testing Library 和 `@testing-library/user-event`：

- 摘要组件正确展示完整、部分和空估值。
- 添加表单拒绝空 ticker、非正数量和负成本。
- 提交中按钮禁用，失败后保留输入。
- 删除前显示确认，取消不调用 API，确认成功后刷新。
- 价格状态为 `DEMO` 或 `UNAVAILABLE` 时出现明确标签。
- 图表同时提供可读的文字图例。

推荐命令：

```bash
cd frontend
npm ci
npm run test -- --run
npm run build
```

### 6.3 E2E 测试

Playwright 只覆盖一条高价值烟雾流程：

```text
打开空组合 -> 添加 AAPL -> 看到摘要和配置图 -> 删除 AAPL -> 回到空状态
```

若 Day 4 前 P0 尚未完成，优先修复功能和补后端测试，不引入 Playwright。团队仍需按相同步骤执行并记录人工验收。

### 6.4 测试数据

- 单元测试使用 fixture/builder 创建数据，避免每个测试复制大段对象。
- 测试不访问真实 Yahoo 或其他外部服务，价格适配器使用 stub 或 Mockito mock。
- Demo 使用虚构持仓和固定价格，确保结果可重复。
- 每个测试独立运行，不依赖执行顺序或其他测试留下的数据。

## 7. API 与前后端契约

后端以 OpenAPI 为契约来源，接口设计见 [REST API 文档](./api_documentation_zh.md)。

推荐流程：

1. Day 2 冻结 MVP request/response schema。
2. 前端根据 JSON 示例定义 TypeScript 类型并使用 mock 数据开发。
3. 后端实现后通过 Swagger 和集成测试验证契约。
4. 联调发现契约变化时，先更新 API 文档和双方类型，再修改实现。

本期不引入自动代码生成，避免两天编码周期增加工具链风险。

## 8. 本机协作、变更同步与联调

### 8.1 基本原则

四名成员都在自己的机器上开发，但不共享某个人的本地数据库，也不依赖某个人一直运行后端。每位成员均可从 `qa` 分支在本机启动完整系统：MySQL、Spring Boot API 和 React 前端。Git、OpenAPI、Flyway 和演示数据才是团队共享的事实来源。

```text
成员 A / B（后端）              成员 C（前端）                 成员 D（QA/交付）
feature/* 本地实现              feature/* 本地实现             qa 本地验收
        │                              │                              │
        ├── 更新 OpenAPI / migration ──┼── 更新 TypeScript DTO / mock ─┤
        │                              │                              │
        └──────────── PR 人工审查后合并到 qa ───────────────────────────┘
                                       │
                         所有人 pull qa 并在本机启动完整应用
```

### 8.2 代码、数据库和接口的同步边界

| 变化类型 | 唯一事实来源 | 作者必须同步提交的内容 | 接收方动作 |
| --- | --- | --- | --- |
| API 字段、路径、状态码 | OpenAPI 文件和 [REST API 文档](./api_documentation_zh.md) | OpenAPI、后端 DTO/测试、前端 mock/类型的更新或兼容说明 | 前端更新类型和 mock，QA 更新验收用例 |
| 数据库结构 | Flyway migration | 新的 `V<n>__*.sql`、实体映射和迁移测试 | 每人 pull 后重新启动本地服务，让 Flyway 自动执行 |
| 估值或校验规则 | 后端 domain 测试和 API schema | 规则说明、单元测试、错误码或响应字段 | 前端只展示服务端结果，不复制计算公式 |
| 前端页面需求 | User Story、验收标准和 API mock | 组件测试、状态说明、所需接口字段 | 后端确认接口能满足展示需求 |

所有破坏性变化必须先创建或更新 User Story，再提出“契约变更 PR”。两天项目中优先采用向后兼容方式，例如新增可选字段；不得悄悄重命名、删除或改变已被前端使用字段的语义。

### 8.3 每日同步节奏

| 时间点 | 时长 | 参与者 | 输出 |
| --- | --- | --- | --- |
| 上午开始 | 10 分钟 | 全员 | 昨日变更、当天接口依赖、阻塞项 |
| 开发前 | 5 分钟 | A、B、C | 检查 OpenAPI、示例 JSON、错误码是否一致 |
| 中午 | 15 分钟 | A、B、C、D | 后端展示已完成 endpoint；前端确认 mock 能否替换为真实 API |
| 下午集成窗口 | 30-45 分钟 | 全员 | 合并已审查 PR 至 `qa`，每人 pull 后本机跑完整流程 |
| 收工前 | 10 分钟 | 全员 | 记录未完成接口、已知缺陷、迁移版本和次日计划 |

`qa` 只接收已经通过本地测试和人工审查的 PR。若 `qa` 红灯，停止合并新功能，先恢复可运行状态。

### 8.4 前端无等待开发

前端不需要等待后端完成才开始：

1. Day 2 先冻结 `GET /holdings`、`POST /holdings`、`DELETE /holdings/{id}` 和 `GET /portfolio/summary` 的示例 JSON。
2. 前端依据相同 TypeScript DTO 使用 fixture/mock 数据完成 Dashboard、表单、空态和错误态。
3. API 适配层集中在 `src/api`，组件不直接调用 `fetch` 或硬编码 URL。
4. 后端 endpoint 合并到 `qa` 后，前端只替换 API 适配层的 mock 实现为真实调用，组件不应重写。
5. 每次契约变更均由前端在本机用真实 API 验证一次，并由 QA 记录结果。

建议通过环境变量切换数据源：

```text
# frontend/.env.development
VITE_API_MODE=live
VITE_API_BASE_URL=/api/v1

# frontend/.env.mock
VITE_API_MODE=mock
```

Vite 开发服务器将 `/api` 代理到本机 `http://localhost:8080`，避免 CORS 干扰本地调试。不要将某位成员的局域网 IP、个人设备或临时隧道写入默认配置。

### 8.5 每位成员的本机启动方式

| 角色 | 日常启动 | 联调时额外动作 |
| --- | --- | --- |
| 后端 A/B | 首次执行 `mysql -u root -p < scripts/mysql/init-local-mysql.sql`，再运行 Spring Boot | `./mvnw clean verify`；通过 Swagger 或 curl 展示真实响应 |
| 前端 C | `npm run dev`，默认 mock 模式 | pull `qa` 后切换 `VITE_API_MODE=live`，本机启动后端和 MySQL |
| QA/交付 D | pull `qa` 后启动三项服务 | 按验收清单执行浏览器流程和 API 请求，记录实际响应 |

前端 C 和 QA D 需要能够启动后端，即使他们不修改 Java。为降低门槛，后端 A/B 负责维护一条复制即可执行的启动命令和固定 demo 数据；这比远程访问他人电脑更可靠，也更符合可复现演示要求。

### 8.6 调试流程

1. 在浏览器 DevTools 的 Network 面板记录请求 URL、方法、状态码、请求体和响应体。
2. 前端将可复现步骤、时间和失败响应发到任务卡；不要只写“接口坏了”。
3. 后端在统一错误响应中返回 `traceId`，并用该 ID 在本机日志中定位对应请求。
4. 后端先用断点、日志和单元测试确认问题，再修复；必要时补一个回归测试。
5. 修复进入 `qa` 后，前端 C 与 QA D 都在自己机器上复测相同步骤，并在任务卡标记结果。

示例缺陷记录：

```text
HH-02 创建持仓失败
环境：qa，commit abc1234，VITE_API_MODE=live
步骤：提交 ticker=AAPL、quantity=0、averagePurchasePrice=175.50
预期：400 VALIDATION_FAILED，fieldErrors 指向 quantity
实际：500 INTERNAL_ERROR，traceId=8f0c2d15d1e44dc8
```

### 8.7 联调完成标准

- [ ] `qa` 的 OpenAPI、前端 DTO、后端 DTO 和 API 文档字段一致。
- [ ] 每人可在本机从零启动 MySQL、后端和前端，且不依赖他人电脑。
- [ ] 新的 Flyway migration 可在空 MySQL 数据库成功执行。
- [ ] mock 模式和真实 API 模式覆盖相同的关键页面状态。
- [ ] 创建、查询、删除和部分价格失败在 `qa` 上均由前端 C 与 QA D 验证。
- [ ] 每个已修复缺陷都有复现步骤、回归测试或明确的人工验收记录。

## 9. 本地开发与 CI

推荐仓库结构：

```text
HoldHive/
├── backend/
├── frontend/
├── docs/
│   ├── guideline/
│   └── design/
├── scripts/mysql/
└── README.md
```

本地数据库由每位成员机器上的 MySQL 提供；前后端仍可通过 IDE 或命令直接启动，以提高调试效率。CI 如需真实 MySQL，可使用 GitHub Actions 的 MySQL service container，不要求成员本机安装 Docker。

GitHub Actions 对每个 PR 执行：

1. 后端 `./mvnw clean verify`。
2. JaCoCo 70% 行覆盖率门槛。
3. 前端 `npm ci`、测试和生产构建。
4. 检查 Flyway migration 能在空数据库执行。

CI 失败的 PR 不得合并到 `qa`、`main` 或 `prod`。完整分支策略、required checks、PR 门禁和可执行 GitHub Actions YAML 模板见 [Git 分支与 GitHub 自动化流程](./git_branching_ci_zh.md)。

## 10. 明确不采用

- 微服务、消息队列、Kubernetes：当前规模没有收益。
- Redis：MVP 价格缓存可以先存 MySQL 或内存。
- GraphQL：REST 已满足固定页面需求。
- Redux：状态规模不足以证明必要性。
- Tailwind 与大型组件库并用：避免样式系统重复。
- 真实券商 OAuth、用户认证和云部署：超出 MVP 范围。
- 直接调用未经验证的市场数据源：Demo 必须有稳定降级方案。

## 11. Day 1 技术栈确认清单

- [ ] 至少两名成员可以使用 React + TypeScript；否则切换原生 JavaScript 方案。
- [ ] 技术评审方已确认前后端框架和 MySQL 符合项目要求。
- [ ] 四人可以运行 Java 21、Node.js 和 MySQL；Maven 通过 `backend/mvnw` 下载。
- [ ] 前后端骨架、健康检查和数据库连接已经打通。
- [ ] OpenAPI 示例和前端 mock 使用相同字段。
- [ ] `local`、`test`、`demo` 环境边界清晰。
- [ ] 测试命令和 JaCoCo 70% 门槛已在 CI 中验证。
- [ ] 技术栈及替代方案已经记录为 ADR，并由团队共同确认。
