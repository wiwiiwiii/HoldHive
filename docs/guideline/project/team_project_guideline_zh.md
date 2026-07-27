# HoldHive 团队项目执行指南

> 项目周期：两天规划 + 两天编码

配套设计文档：

- [技术栈定案](./technology_stack_zh.md)
- [成员目录分工速查](./member_directory_map_zh.md)
- [Git 分支与 GitHub 自动化流程](./git_branching_ci_zh.md)
- [实时股价 API 方案](./market_data_api_zh.md)
- [数据库设计](./database_design_zh.md)
- [REST API 文档](./api_documentation_zh.md)

## 1. 项目目标

HoldHive 是一个面向单一用户的投资组合管理应用。用户可以查看、添加和删除持仓，并查看组合市值、盈亏和图表化表现。

执行原则是先交付稳定、可解释、可演示的 MVP，再考虑增强功能。任何扩展都不得影响持仓管理、估值计算、图表展示和本地启动流程。

## 2. 竞品介绍和链接

以下调研基于各产品官网或官方帮助中心，重点观察大型投资组合产品如何解决“数据录入、组合总览、收益解释、风险认知”四类问题。竞品能力用于启发范围取舍，不代表 HoldHive 要在四天内复制这些产品。

| 产品 | 市场定位与代表能力 | HoldHive 可借鉴 | 本期不复制 |
| --- | --- | --- | --- |
| [Empower Personal Dashboard](https://www.empower.com/tools) | 聚合投资、现金和负债账户，并把资产配置、现金流、退休规划放进统一财富视图。 | 首页先给用户整体结论，再允许查看明细。 | 多机构账户聚合、预算及退休规划。 |
| [Morningstar Investor Portfolio](https://www.morningstar.com/tools/portfolio/all) | 通过 Portfolio X-Ray、基准对比、行业暴露和持仓重合分析解释组合风险。 | 图表必须回答一个问题，不能只展示数字；后续可扩展集中度分析。 | 评级体系、基金穿透和专业研究数据。 |
| [Sharesight](https://www.sharesight.com/portfolio-tracker/) | 覆盖多市场、多币种、股息、公司行动、收益归因和税务报告；强调准确的业绩追踪。 | 将投入成本、当前价值和盈亏分开，清楚说明计算口径。 | 税务、公司行动、多币种收益归因。 |
| [TradingView Portfolios](https://www.tradingview.com/support/solutions/43000760937-tradingview-portfolios-track-your-assets-know-your-trades/) | 将组合价值、收益历史、基准、资产/行业/币种配置，以及 Sharpe、Sortino 等风险指标集中展示。 | 采用“总览 - 持仓 - 分析”的渐进式信息架构。 | 专业技术指标、新闻、基准模拟和高级风险模型。 |
| [Yahoo Finance Portfolios](https://help.yahoo.com/kb/SLN7034.html) | 支持手工录入、CSV、券商连接、实际/模拟/观察组合，以及可定制视图。 | 新用户必须能手工快速建立第一个组合，不依赖复杂连接。 | 券商同步、交易批次、股息和现金流水。 |
| [Kubera](https://help.kubera.com/article/114-what-is-recap-in-kubera) | 从资产、债务、净值和配置角度提供全局财富趋势，并可汇总多个子组合。 | 产品语言应围绕“我的整体情况”，而不是堆砌证券术语。 | 实物资产、债务和家庭/实体级组合。 |
| [Portfolio Performance](https://help.portfolio-performance.info/en/) | 开源桌面工具，支持交易、历史价格、自定义分类、目标配置、再平衡、TTWROR 和 IRR。 | 计算需要可解释、可测试，并在界面说明口径。 | 完整交易账本、TTWROR/IRR 和高级再平衡。 |

### 竞品结论

1. **首页需要先回答“我现在怎么样”**：总价值、投入成本、盈亏和资产配置应在首屏出现。
2. **数据录入决定首次体验**：大型产品普遍支持连接、导入或手工录入；本期只做好最低风险的手工录入。
3. **分析价值来自解释而非指标数量**：一个清晰的配置图比十个无人理解的专业指标更适合 MVP。
4. **收益口径必须透明**：不同产品可能采用资金加权或时间加权收益；本期只计算基于当前价格和平均成本的未实现盈亏，并明确标注。
5. **可信度是金融产品的核心体验**：必须显示价格更新时间、数据来源、加载失败和演示数据状态，不能用静默降级制造虚假的精确感。

### HoldHive 的差异化机会

HoldHive 不追求“功能最多”，而定位为一个可解释、低门槛、适合投资初学者的组合快照：用户在一分钟内完成三件事——看懂组合价值、识别资产集中情况、维护一项持仓。它用更少的字段和更明确的语言换取更低的学习成本，也让团队能够在两天编码时间内交付一个可以完整解释和可靠演示的产品。

## 3. 产品设计

### 3.1 背景故事

刚开始工作的年轻职员 Alex 通过不同平台买了几只股票。他知道每只股票今天涨跌多少，却回答不了三个更重要的问题：自己一共投入了多少钱、整个组合现在值多少、风险是否过度集中在某一只股票上。

Alex 尝试过电子表格，但价格需要手动更新，公式也容易出错；成熟的财富管理工具能力强，却包含账户聚合、税务、退休规划和复杂风险指标，第一次使用时很难判断应该先看什么。于是他逐渐回到“分别打开几个应用看价格”的旧习惯，拥有大量行情信息，却没有形成组合视角。

HoldHive 的名字来自蜂巢：每项持仓像一个独立蜂房，只有汇集起来才能看见完整结构。Alex 只需录入证券代码、数量和平均买入价，系统就把分散的持仓整理成一张可信的组合快照。打开首页时，他首先看到总价值和盈亏，然后通过资产配置图识别集中风险；需要调整记录时，可以在同一页面完成添加或删除。

演示故事以 Alex 为主线：他首次打开空组合，加入三项持仓，看到组合价值和配置图形成；随后发现其中一项占比过高，并删除一条错误记录。整个过程展示 HoldHive 如何把“零散行情”变为“可理解的组合”。

### 3.2 产品愿景与价值主张

**愿景：** 让投资初学者无需掌握专业金融术语，也能快速建立并理解自己的投资组合快照。

**价值主张：** HoldHive 用最少的输入，清楚呈现“持有什么、现在值多少、盈亏如何、是否集中”，并对数据来源和计算口径保持透明。

**一句话介绍：** 把零散持仓放进一个蜂巢，一分钟看懂你的组合。

### 3.3 目标用户与非目标用户

**主要用户：** 持有少量股票、希望集中查看整体情况，但暂时不需要专业交易或税务系统的投资初学者。

**核心需求：**

- 快速录入持仓，不需要连接真实券商账户。
- 一眼看懂总投入、当前价值和未实现盈亏。
- 识别单一持仓占比过高的情况。
- 在市场数据不可用时知道发生了什么，而不是看到错误数字。

**非目标用户：** 高频交易者、专业基金经理、需要报税/审计报表的用户、需要多账户多币种精确归因的用户。本期也不提供认证、真实交易、买卖建议或自动调仓。

### 3.4 设计原则

1. **结论优先：** 首页先展示整体组合，再展示单项持仓。
2. **渐进披露：** 默认只显示关键指标，详细数据在表格中展开。
3. **透明可信：** 每个市场价格显示来源、时间和实时/演示状态。
4. **防止误操作：** 表单即时校验，删除需要二次确认。
5. **可解释优先：** 只展示团队可以解释、测试和演示的金融计算。
6. **无数据也是完整状态：** 空组合、加载中、失败和部分数据缺失均有明确界面。
7. **低耦合与明确依赖：** 每个模块只依赖更稳定、更底层的接口，不跨层直接调用实现细节；新增依赖必须说明调用方向、替换方式和测试隔离方式。

### 3.5 信息架构

MVP 采用单页 Dashboard，避免在两天编码时间内引入不必要的路由和状态复杂度：

```text
HoldHive Dashboard
├── 顶部栏：品牌、价格更新时间、数据状态
├── 组合摘要：总投入、当前价值、未实现盈亏、持仓数
├── 资产配置：按持仓市值展示的环形图和图例
├── 持仓列表：ticker、数量、均价、当前价、市值、盈亏、占比、操作
├── 添加持仓：按钮打开表单或侧栏
└── 全局反馈：成功提示、错误提示、删除确认
```

如果时间允许，再平衡提示放在摘要和图表之间，明确展示触发规则和计算依据。

### 3.6 核心用户流程

#### 流程 A：建立第一个组合

1. 用户看到空状态和“添加第一项持仓”按钮。
2. 输入 ticker、quantity、averagePurchasePrice。
3. 前端即时检查必填、数值范围和格式。
4. 后端再次验证并保存。
5. 页面刷新摘要、图表和持仓表，并显示成功反馈。

#### 流程 B：理解组合

1. 用户打开 Dashboard。
2. 首先读取当前价值和未实现盈亏。
3. 查看资产配置图，识别最大持仓。
4. 在持仓表中查看单项市值、盈亏和占比。
5. 如果价格缺失，系统明确标记受影响持仓，不把未知价格当作 0。

#### 流程 C：删除错误持仓

1. 用户在持仓行选择删除。
2. 确认框显示 ticker，并说明操作结果。
3. 用户确认后调用删除 API。
4. 成功后同步刷新摘要、图表和列表；失败则保留原数据并显示原因。

### 3.7 页面和状态设计

| 区域 | 展示内容 | 交互与状态 |
| --- | --- | --- |
| 顶部栏 | HoldHive 品牌、数据状态、最后更新时间 | 数据为 mock 时显示“演示数据”；失败时提供重试 |
| 摘要卡 | 总投入、当前价值、未实现盈亏、持仓数 | 盈亏同时使用正负号、文字和颜色，避免只依赖颜色 |
| 配置图 | 每项持仓的市值占比 | 图例含 ticker、比例和金额；无有效价格时解释无法计算 |
| 持仓表 | ticker、数量、均价、现价、市值、盈亏、占比 | 支持删除；移动端允许横向滚动或折叠次要列 |
| 添加表单 | ticker、数量、平均买入价 | 保留用户输入；错误定位到字段；提交时防止重复点击 |
| 空状态 | 产品价值说明和主行动按钮 | 不显示全是 0 的无意义图表 |
| 错误状态 | 可理解的原因和恢复动作 | API 失败可重试；表单失败不关闭表单 |

### 3.8 界面参考、现代交互与动画

`docs/design/assets/sample_screenshot.png` 展示了一个适合本项目的基础 Portfolio Dashboard：左侧导航、顶部组合 KPI、两张图表卡、中部持仓表和底部资产卡。HoldHive 不需要逐像素复刻，但应保留这种信息层级，并做得更现代、直观和用户友好。

竞品调研给出的界面方向如下：Empower 的个人 Dashboard 强调整体投资配置和账户级总览；Morningstar Portfolio X-Ray 用资产类别、地域、行业和 Top Holdings 等视角解释组合风险；Sharesight 的报表强调按日期范围查看组合 performance；TradingView Portfolios 同时提供组合价值/表现视图、distribution 视图和 holdings 数据视图。HoldHive 的 MVP 应吸收这些成熟产品的共同点：首屏先给组合结论，图表解释配置和变化，表格承担可审计明细，状态提示解释数据可信度。

#### 页面布局建议

| 区域 | 视觉目标 | MVP 实现 |
| --- | --- | --- |
| 顶部 KPI | 让用户 3 秒内看懂当前状态 | 总市值、未实现盈亏、持仓数、价格状态、添加持仓按钮 |
| 左侧导航 | 形成现代产品感，但不增加路由复杂度 | 保留 Dashboard；Holdings/Performance 可显示为禁用或作为页面锚点 |
| 图表卡片 | 比表格更快解释组合 | 必做资产配置 donut；时间允许加组合价值 line/area chart |
| 持仓表 | 提供可审计明细 | ticker、quantity、average price、current price、market value、P/L、allocation、delete |
| Insight 卡片 | 把指标翻译成人话 | 最大持仓、未定价持仓、演示价格、集中度提示 |
| 底部卡片 | 做二级摘要 | priced holdings、unpriced holdings、top holding、data source |

#### 图表设计

| 图表 | 优先级 | 前端库建议 | 数据来源 | 说明 |
| --- | --- | --- | --- | --- |
| Asset Allocation Donut | P0 | Recharts `PieChart` 或 Chart.js doughnut | `summary.allocations` | 展示各持仓市值占比，必须配文字图例 |
| Portfolio Value Trend | P1 | Recharts `AreaChart` / `LineChart` 或 Chart.js line | 演示历史估值或后续 `portfolio_valuation` | 用于增强展示，不作为 P0 阻塞 |
| Gain/Loss Bar | P1 | Recharts `BarChart` 或 Chart.js bar | holdings | 展示每个持仓盈亏，帮助识别贡献项 |
| Concentration Indicator | P1 | CSS progress/ring 或 Recharts radial | summary 最大占比 | 超过 40% 时提示，不做投资建议 |

图表必须同时提供文字图例和数值，不得只依赖颜色。正收益使用绿色、负收益使用红色，但必须同时使用 `+/-` 符号和文字说明，照顾色弱用户。

#### 用户友好提示

| 场景 | 文案示例 | 位置 |
| --- | --- | --- |
| 空组合 | `Add your first holding to create a portfolio snapshot.` | 空状态 |
| 添加成功 | `AAPL added. Portfolio totals refreshed.` | Toast |
| 删除成功 | `AAPL removed. Allocation and totals updated.` | Toast |
| 表单校验 | `Quantity must be greater than 0.` | 字段下方 |
| 重复持仓 | `AAPL already exists. Edit the existing holding instead of adding a duplicate.` | 表单顶部 |
| 演示价格 | `Demo prices are shown for training. They are not live market data.` | 顶部状态条 |
| 部分估值 | `Some holdings have no price. Totals only include priced holdings.` | 摘要区 |
| 价格失败 | `Price service is unavailable. Saved holdings are still safe.` | Banner |
| 集中度提示 | `AAPL is 46% of priced market value. Consider whether this concentration matches your plan.` | Insight 卡片 |

#### 动态与过渡

- 页面首次加载时，KPI、图表和表格使用 120-180ms 的轻微 fade/slide-in。
- 新增或删除持仓后，总市值、盈亏和持仓数做 300ms 数字高亮或 count-up。
- Donut 和 line chart 使用 400-700ms 内置动画；避免循环动画。
- 新增表格行使用浅色背景高亮 1 秒；删除必须先确认。
- 表单提交按钮显示 loading 并禁用，防止重复提交。
- 加载中使用 skeleton，比全屏 spinner 更稳定。
- 尊重 `prefers-reduced-motion`；系统要求减少动画时关闭非必要过渡。

不建议在 MVP 做大面积粒子、视频背景、复杂路由转场或遮挡数据的动效。金融 Dashboard 的美观来自清晰层级、稳定布局、准确反馈和细腻过渡，不是装饰性动画。

### 3.9 功能需求与验收标准

| ID | 优先级 | 用户故事 | 验收标准 |
| --- | --- | --- | --- |
| HH-01 | P0 | 作为用户，我可以查看全部持仓。 | 页面和 API 返回持仓；无数据时显示可行动的空状态。 |
| HH-02 | P0 | 作为用户，我可以添加持仓。 | ticker 非空并标准化为大写；quantity 大于 0；平均买入价大于等于 0；成功后所有区域同步更新。 |
| HH-03 | P0 | 作为用户，我可以删除错误持仓。 | 删除前确认；成功后记录消失并重新计算；不存在的 ID 返回明确错误。 |
| HH-04 | P0 | 作为用户，我可以查看组合摘要。 | 正确显示总投入、当前价值、未实现盈亏和持仓数；空组合不报错。 |
| HH-05 | P0 | 作为用户，我可以查看资产配置。 | 图表比例基于有效市值，合计受浮点舍入影响时约为 100%；缺失价格被明确标记。 |
| HH-06 | P0 | 作为用户，我知道数据是否可信。 | 显示价格来源、更新时间、实时/缓存/演示状态；市场服务失败不返回伪造的实时价格。 |
| HH-07 | P1 | 作为用户，我可以看到集中度提示。 | 任一持仓占有效总市值超过 40% 时显示解释性提示，并说明该提示仅用于风险认知。 |

### 3.10 计算口径

对每项持仓：

```text
投入成本 = quantity × averagePurchasePrice
当前市值 = quantity × currentPrice
未实现盈亏 = 当前市值 - 投入成本
未实现盈亏率 = 未实现盈亏 ÷ 投入成本 × 100%
持仓占比 = 当前市值 ÷ 全部有效持仓当前市值 × 100%
```

组合总值为所有具有有效价格的持仓市值之和。若任一持仓价格缺失，摘要必须显示“部分估值”，并列出未计入项。投入成本为 0 时不计算盈亏率，显示 `N/A`，避免除零。金额统一使用两位小数；内部计算不得过早舍入。

本期的“表现”是当前快照下的未实现盈亏，不等同于考虑资金流、股息、费用和持有期的真实投资回报率。界面和演示必须说明该限制。

### 3.11 API 边界建议

| 方法与路径 | 目的 | 关键响应 |
| --- | --- | --- |
| `GET /api/holdings` | 查询全部持仓及估值 | `200`；空组合返回空数组 |
| `POST /api/holdings` | 新增持仓 | `201`；无效字段返回 `400` |
| `DELETE /api/holdings/{id}` | 删除持仓 | `204`；不存在返回 `404` |
| `GET /api/portfolio/summary` | 获取摘要、配置和数据状态 | `200`；价格服务异常时返回部分结果和状态 |

错误响应采用一致结构，例如 `code`、`message`、`fieldErrors` 和 `timestamp`。技术堆栈确定后，字段名和完整 schema 应写入 OpenAPI，并由产品负责人和技术评审方确认。

### 3.12 非功能与风险设计

- **性能：** 演示数据规模下，主要查询应在正常本地环境中快速响应；外部价格请求设置超时，避免页面无限等待。
- **可访问性：** 表单包含 label；键盘可操作；焦点可见；盈亏不只依赖红绿颜色表达；图表提供文字图例。
- **数据安全：** MVP 无认证且只假设单用户，只能用于本地或受控演示环境；禁止部署为公开的真实资产管理服务。
- **隐私：** 不录入姓名、账户号、券商凭据或其他个人敏感信息；演示只使用虚构数据。
- **可靠性：** 数据库是持仓事实来源；市场价格失败不得破坏已保存持仓。
- **可维护性：** 市场数据通过适配器隔离，使真实数据和演示数据可以替换；业务计算保持纯函数并由单元测试覆盖。

### 3.13 成功指标与范围边界

本期成功不以功能数量衡量，而以以下结果衡量：

- 新用户可以在 60 秒内添加第一项持仓。
- 用户可以在首页回答“总值多少、盈亏多少、最大持仓是什么”。
- 全部 P0 验收标准通过，后端行覆盖率至少 70%。
- 价格服务断网时仍可演示已保存持仓，并明确解释数据状态。
- 四位成员都能解释产品取舍、计算公式和自己负责或审查的代码。

明确不在本期范围内：登录和多用户、真实交易、券商连接、CSV 导入、现金流水、股息、税务、公司行动、多币种、历史交易账本、基准比较、专业风险指标和个性化投资建议。

## 4. MVP 范围

### 必须完成

- 持久化保存持仓数据。
- REST API 支持持仓查询、新增和删除。
- UI 支持浏览组合、新增持仓和删除持仓。
- 展示组合总市值、总盈亏及至少一个图表。
- 后端单元测试行覆盖率不低于 70%。
- 提供 API 文档、架构图、ERD 和可运行说明。

### 可选增强

- 接入市场价格数据；外部服务不可用时使用明确标识的演示数据。
- 规则型再平衡提示，例如单一资产占比超过 40%。
- 规则型集中度提示、价格缓存状态和更完整的组合历史视图。

## 5. 最小数据模型

初版不要引入复杂金融模型，只保存最小持仓字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 持仓唯一标识 |
| `ticker` | 证券代码，例如 `AAPL` |
| `quantity` | 持仓数量，必须大于 0 |
| `averagePurchasePrice` | 平均买入价，必须大于等于 0 |
| `createdAt` | 创建时间 |

当前价格、市值和盈亏在查询时由价格服务及业务计算获得，不作为 MVP 的核心持久化字段。

## 6. 四人分工

| 成员 | 主责 | 主要交付物 | 审查责任 |
| --- | --- | --- | --- |
| 成员 A：后端与架构 | 数据模型、数据库、核心 API | ERD、CRUD API、数据库迁移、OpenAPI 文档 | 审查成员 B 的后端 PR |
| 成员 B：业务逻辑与质量 | 组合计算、价格服务、后端测试 | 市值/盈亏计算、异常处理、单元测试、覆盖率报告 | 审查成员 A 的后端 PR |
| 成员 C：前端与体验 | 页面、图表、前后端联调 | 持仓列表、表单、删除、汇总、图表 | 审查成员 D 的文档或配置 PR |
| 成员 D：产品、QA 与交付 | 需求管理、验收、集成、演示 | 看板、验收清单、ADR、测试记录、PPT | 审查成员 C 的前端 PR |

分工不等于隔离。每位成员必须至少提交一个功能 PR、人工审查一个队友 PR、参与测试或文档维护，并在最终展示中发言。

### 6.1 后端两人细分

后端两人不要按 `Controller / Service / Repository` 这种横切层分工，否则会频繁修改同一批文件，产生冲突并拖慢联调。最高效的分法是按“数据主链路”和“业务质量链路”拆分：

| 后端成员 | 核心职责 | 具体任务 | 交付边界 |
| --- | --- | --- | --- |
| 成员 A：API + 数据库负责人 | 让持仓数据可靠进出 MySQL | Flyway 建表、Entity/Repository 或 JDBC DAO、`GET /holdings`、`POST /holdings`、`DELETE /holdings/{id}`、基础 Swagger/OpenAPI | 前端可以通过 API 完成持仓查询、新增和删除 |
| 成员 B：业务计算 + 质量负责人 | 让数据被正确计算、校验、测试并稳定返回 | `PortfolioCalculator`、summary API、demo/market price adapter、统一错误响应、validation、JUnit/Mockito/MockMvc 测试、JaCoCo 覆盖率 | 前端可以拿到 summary、图表数据和稳定错误格式 |

协作规则：

- 成员 A 先打通 CRUD 主链路，即使价格先返回 `UNAVAILABLE`。
- 成员 B 的估值和占比计算先写成可测试的 domain/service 方法，不直接堆在 Controller 中。
- 成员 A 负责数据库 migration 追加和本地启动脚本；成员 B 负责测试矩阵和覆盖率门槛。
- 两人每天合并 2-3 个小 PR，不把所有后端代码攒到一个大 PR。
- API DTO、错误响应和字段名一旦给前端使用，修改前必须先通知前端并同步文档。
- 已经共享出去的 Flyway migration 不反复改，后续变化用新版本 migration。
- 每个后端 PR 互相 review：A 检查 B 的业务计算是否能接上数据库，B 检查 A 的 CRUD 是否覆盖错误和边界。

按天拆分建议：

| 时间 | 成员 A：API + 数据库 | 成员 B：业务计算 + 质量 |
| --- | --- | --- |
| Day 1-2 Planning | 定 MySQL 表结构、Flyway 文件、Holding/Instrument/PriceSnapshot 字段、ERD | 定错误响应、summary response、priceStatus、valuationStatus、计算规则、测试矩阵 |
| Day 3 Coding | 实现 migration、持仓实体/DAO、CRUD API、基础 API 文档 | 实现 portfolio summary、price adapter、validation、global exception handler、核心单元测试 |
| Day 4 Coding | 修 CRUD/数据库 bug、支持 Swagger/curl 联调、维护启动说明 | 补 MockMvc/API 错误测试、覆盖率到 70%+、支持图表字段和演示数据 |

一句话边界：成员 A 负责“数据能可靠进出 MySQL”，成员 B 负责“数据能被正确计算、校验、测试并稳定返回给前端”。

### 6.2 每人实现目录

以下目录结构用于编码任务、PR review 和联调验收。Day 1 创建项目骨架时应按本节先建立目录边界，不要求一次写满所有文件；Day 3-4 再按任务逐步实现。目录归属表示主要负责人，不表示其他成员不能修改；跨负责人目录的改动必须在 PR 描述中说明原因，并请对应负责人 review。

仓库中已为主要工作区保留入口说明：后端见 `backend/src/main/java/com/holdhive/*/README.md` 和 `backend/src/test/java/com/holdhive/README.md`；前端见 `frontend/src/README.md`、`frontend/src/api/README.md` 和 `frontend/src/features/portfolio/README.md`；成员拉取仓库后可先阅读 [成员目录分工速查](./member_directory_map_zh.md)，再进入自己的 feature 分支编码。

#### 目录责任总览

| 成员 | 主要实现目录 | 主要文件类型 | 不建议直接负责 |
| --- | --- | --- | --- |
| 成员 A | `backend/src/main/java/.../holdhive/portfolio/api`、`portfolio/persistence`、`src/main/resources/db/migration` | Controller、DTO、Entity、Repository、Flyway SQL、OpenAPI 基础配置 | 组合估值公式和价格适配器内部逻辑 |
| 成员 B | `backend/src/main/java/.../holdhive/portfolio/application`、`portfolio/domain`、`pricing`、`common/error` | Service、Calculator、Price Adapter、异常处理、后端测试 | Flyway 建表主迁移和前端页面组件 |
| 成员 C | `frontend/src/api`、`frontend/src/features/portfolio`、`frontend/src/components`、`frontend/src/styles` | React 组件、Hook、DTO、样式、图表、前端测试 | 后端数据库迁移和核心业务计算 |
| 成员 D | `.github/workflows`、`docs/qa`、`docs/demo`、`docs/adr`、`scripts/mysql`、`.env.example`、`docs/guideline` | CI 配置、验收清单、缺陷记录、演示脚本、ADR、交付文档 | 未经负责人确认直接改 API 字段或数据库结构 |

每个成员的 feature 分支建议和目录对应：成员 A 使用 `feature/backend-crud-db`，成员 B 使用 `feature/backend-summary-pricing`，成员 C 使用 `feature/frontend-dashboard`，成员 D 使用 `feature/qa-ci-docs`。所有分支最终通过 PR 合并到 `qa`，不直接推送到 `main` 或 `prod`。

后端包名前缀以实际 `groupId` 为准，示例中用 `.../holdhive` 表示。

#### 成员 A：后端 API 与数据库

成员 A 负责数据库结构、持仓 CRUD 主链路、JPA 映射和后端启动配置。目标是让持仓数据能够可靠写入 MySQL、从 MySQL 读出，并通过基础 REST API 暴露给前端。

```text
backend/
├── pom.xml                                      # 与成员 B 共同维护依赖和插件
├── src/main/java/.../holdhive/
│   ├── HoldHiveApplication.java                 # Spring Boot 入口
│   ├── common/
│   │   └── config/
│   │       ├── OpenApiConfig.java               # Swagger/OpenAPI 基础配置
│   │       └── CorsConfig.java                  # 本地前后端联调配置
│   └── portfolio/
│       ├── api/
│       │   ├── HoldingController.java           # GET/POST/DELETE holdings
│       │   └── dto/
│       │       ├── CreateHoldingRequest.java
│       │       ├── HoldingResponse.java
│       │       └── HoldingMapper.java
│       └── persistence/
│           ├── entity/
│           │   ├── PortfolioEntity.java
│           │   ├── InstrumentEntity.java
│           │   ├── HoldingEntity.java
│           │   └── PriceSnapshotEntity.java
│           └── repository/
│               ├── PortfolioRepository.java
│               ├── InstrumentRepository.java
│               ├── HoldingRepository.java
│               └── PriceSnapshotRepository.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-test.yml
│   └── db/migration/
│       ├── V1__create_portfolio_tables.sql
│       ├── V2__create_price_snapshot_table.sql
│       └── V3__seed_demo_portfolio.sql
└── src/test/java/.../holdhive/
    └── portfolio/persistence/
        ├── HoldingRepositoryTest.java
        └── FlywayMigrationTest.java
```

成员 A 的交付边界：

- MySQL 表结构、索引、外键和唯一约束可由 Flyway 从空库创建。
- `GET /api/v1/holdings`、`POST /api/v1/holdings`、`DELETE /api/v1/holdings/{id}` 可运行。
- Repository 测试覆盖唯一约束、基础查询和持仓删除。
- 本地启动说明中的数据库连接和 profile 可用。

#### 成员 B：后端业务计算与质量

成员 B 负责组合估值、价格适配器、统一错误响应和后端测试矩阵。目标是让 API 返回的市值、盈亏、占比、价格状态和错误格式稳定、可测试。

```text
backend/
├── src/main/java/.../holdhive/
│   ├── common/
│   │   └── error/
│   │       ├── ApiErrorResponse.java
│   │       ├── GlobalExceptionHandler.java
│   │       └── ErrorCode.java
│   ├── portfolio/
│   │   ├── api/
│   │   │   ├── PortfolioSummaryController.java
│   │   │   └── dto/
│   │   │       ├── PortfolioSummaryResponse.java
│   │   │       ├── AllocationResponse.java
│   │   │       └── UnpricedHoldingResponse.java
│   │   ├── application/
│   │   │   ├── HoldingCommandService.java
│   │   │   ├── HoldingQueryService.java
│   │   │   └── PortfolioSummaryService.java
│   │   └── domain/
│   │       ├── PortfolioCalculator.java
│   │       ├── HoldingValuation.java
│   │       ├── PortfolioValuation.java
│   │       ├── ValuationStatus.java
│   │       └── MoneyMath.java
│   └── pricing/
│       ├── application/
│       │   ├── PricingService.java
│       │   └── PriceMode.java
│       ├── domain/
│       │   ├── MarketQuote.java
│       │   └── PriceStatus.java
│       └── infrastructure/
│           ├── EastMoneyPricingAdapter.java
│           ├── DemoPricingAdapter.java
│           └── PricingAdapter.java
└── src/test/java/.../holdhive/
    ├── portfolio/domain/
    │   └── PortfolioCalculatorTest.java
    ├── portfolio/application/
    │   ├── HoldingCommandServiceTest.java       # Mockito mock Repository/PricingAdapter
    │   └── PortfolioSummaryServiceTest.java     # Mockito mock Repository/PricingAdapter
    ├── portfolio/api/
    │   ├── HoldingControllerTest.java           # MockMvc
    │   └── PortfolioSummaryControllerTest.java  # MockMvc
    ├── pricing/
    │   └── PricingServiceTest.java
    └── common/error/
        └── GlobalExceptionHandlerTest.java
```

成员 B 的交付边界：

- `GET /api/v1/portfolio/summary` 返回总成本、总市值、未实现盈亏、配置比例和价格状态。
- 业务计算使用 `BigDecimal`，不使用 `double`。
- Mockito 只用于隔离 Service 和 Adapter 依赖，不替代数据库、migration 或 Controller JSON 测试。
- 后端测试覆盖率达到 JaCoCo `>= 70%`。

#### 成员 C：前端页面与交互

成员 C 负责 React 前端应用、页面状态、图表和前后端联调。目标是让用户可以完成查看组合、添加持仓、删除持仓、查看摘要和图表的完整流程。

```text
frontend/
├── package.json
├── vite.config.ts
├── index.html
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── api/
    │   ├── httpClient.ts
    │   ├── portfolioApi.ts
    │   ├── marketApi.ts
    │   └── types.ts
    ├── features/
    │   └── portfolio/
    │       ├── PortfolioDashboard.tsx
    │       ├── components/
    │       │   ├── SummaryCards.tsx
    │       │   ├── AllocationDonut.tsx
    │       │   ├── HoldingsTable.tsx
    │       │   ├── AddHoldingForm.tsx
    │       │   ├── DeleteHoldingDialog.tsx
    │       │   ├── PriceStatusBadge.tsx
    │       │   └── EmptyPortfolioState.tsx
    │       ├── hooks/
    │       │   ├── usePortfolioData.ts
    │       │   └── useHoldingActions.ts
    │       └── fixtures/
    │           └── portfolioFixtures.ts
    ├── components/
    │   ├── AppShell.tsx
    │   ├── Navigation.tsx
    │   ├── ToastHost.tsx
    │   └── LoadingSkeleton.tsx
    ├── styles/
    │   ├── tokens.css
    │   ├── theme.css
    │   └── global.css
    └── test/
        ├── setup.ts
        ├── PortfolioDashboard.test.tsx
        ├── AddHoldingForm.test.tsx
        └── HoldingsTable.test.tsx
```

成员 C 的交付边界：

- 前端通过 `src/api` 访问后端，不在组件中硬编码第三方行情 URL。
- Dashboard 支持 loading、empty、partial valuation、error 和 success 状态。
- 添加和删除持仓后刷新 holdings 与 summary。
- 图表使用 Recharts，并提供文字图例。
- 前端测试覆盖表单校验、删除确认、价格状态和关键渲染。

#### 成员 D：QA、交付与自动化

成员 D 负责验收资产、协作流程、CI/CD 配置和演示材料。目标是让项目可从零启动、可验收、可回归，并在最终展示时有稳定交付版本。

```text
.
├── .github/
│   └── workflows/
│       ├── pr-check.yml
│       ├── qa-integration.yml
│       └── release.yml
├── docs/
│   ├── adr/
│   │   ├── ADR-001-technology-stack.md
│   │   ├── ADR-002-market-data-provider.md
│   │   └── ADR-003-branching-and-ci.md
│   ├── qa/
│   │   ├── acceptance-checklist.md
│   │   ├── defect-log.md
│   │   └── regression-record.md
│   ├── demo/
│   │   ├── demo-script.md
│   │   ├── demo-data.md
│   │   └── fallback-plan.md
│   └── architecture/
│       ├── c4-context.md
│       └── erd.md
├── scripts/
│   └── mysql/
├── .env.example
└── docs/
    ├── guideline/
    │   ├── README.md
    │   ├── project/
    │   ├── references/
    │   ├── output/
    │   └── tools/
    └── design/
        ├── lanhu/
        └── assets/
```

成员 D 的交付边界：

- GitHub Actions 能在 PR、`qa`、`main`、`prod` 场景下执行对应检查。
- 验收清单覆盖新增、查询、删除、组合摘要、价格失败和空状态。
- 缺陷记录包含环境、commit、复现步骤、预期、实际和修复验证。
- Demo 脚本、演示数据和失败备用方案可直接用于最终展示。

#### 共享目录规则

| 目录或文件 | 主要负责人 | 共同修改规则 |
| --- | --- | --- |
| `backend/pom.xml` | A、B | 新依赖必须说明用途；测试依赖优先使用 Spring Boot starter 已包含能力 |
| `backend/src/main/resources/db/migration/` | A | 已合并 migration 不修改；新增变更使用新版本文件 |
| `backend/src/main/java/.../portfolio/api/dto/` | A、B | 字段变更必须同步 API 文档和前端 `types.ts` |
| `frontend/src/api/types.ts` | C | 后端字段变更后由 C 更新，A/B review |
| `.github/workflows/` | D | 影响 CI 门禁的变更必须由对应模块负责人确认 |
| `scripts/mysql/`、`.env.example` | D、A | 数据库和 profile 配置变更必须可本地复现 |
| `docs/guideline/project/*.md` | D | 涉及技术栈、API 或数据库的改动需对应负责人 review |

#### 低耦合与依赖规则

本项目按“接口稳定、实现可替换”的原则组织代码。PR 中只要新增跨目录调用、第三方库或共享类型，都必须说明依赖原因；如果无法说明替换方式，默认不合并。

```text
Frontend UI
  -> frontend hooks
  -> frontend api client + DTO
  -> REST API / OpenAPI
  -> backend controller
  -> backend application service
  -> backend domain service / calculator
  -> repository interface
  -> JPA entity / MySQL

backend application service
  -> pricing service interface
  -> pricing adapter implementation
  -> external market data API
```

依赖约束：

- 前端组件只能依赖 `src/api` 暴露的客户端和 DTO，不直接拼接后端 URL，也不直接访问第三方行情接口。
- 后端 `domain` 不依赖 Spring、JPA、HTTP、数据库或外部行情 API；它只处理业务规则和可测试计算。
- `application` 可以依赖 domain、repository 接口和 pricing service 接口，但不直接解析外部 API 原始响应。
- `persistence` 负责 Entity、Repository 和数据库映射，不写组合估值、价格降级或 HTTP 状态码逻辑。
- `pricing.infrastructure` 可以依赖外部 API SDK 或 HTTP client，但必须通过 `PricingAdapter` 接口对内暴露。
- `common` 只能放错误响应、配置和通用工具，不放 portfolio 或 pricing 的业务规则。
- DTO、Entity、Domain Object 不混用：Controller 返回 DTO，Repository 管 Entity，计算使用 Domain Object。
- 若一个文件需要同时修改三个以上业务方向，优先拆小模块，而不是继续堆逻辑。

PR review 检查点：

- 是否出现 UI 组件直接调用 `fetch`、直接写 URL 或复制后端估值公式。
- 是否出现 Controller 直接访问 Repository 并跳过 application service。
- 是否出现 domain 层 import `org.springframework`、`jakarta.persistence` 或 HTTP client。
- 是否出现价格适配器修改持仓数据，或数据库层决定价格状态。
- 是否新增依赖但没有单元测试、mock/stub 或降级策略。

测试隔离规则：

- Domain 测试不启动 Spring，不连数据库，不访问网络。
- Service 测试用 Mockito mock Repository 和 PricingAdapter。
- Controller 测试用 MockMvc 验证 HTTP 契约，不依赖真实外部行情。
- Repository 测试只验证数据库映射和约束，不验证组合收益算法。
- 前端组件测试 mock `src/api`，不让组件测试依赖真实后端启动状态。

## 7. 四天计划

### Day 1：规划 - 需求与仓库基线

全员完成以下事项：

1. 与产品负责人和技术评审方确认 MVP 字段、价格数据来源和验收优先级。
2. 创建 Jira、Trello 或等价看板；所有工作必须从带优先级和验收标准的 User Story 开始。
3. 创建 `main`、`qa`、`feature/*` 分支。
4. 确认技术栈、目录结构、错误响应格式和 API 命名约定。
5. 创建并审查初版 ERD、C4/架构图、API 契约和演示数据集。
6. 使用 ADR 记录关键技术选择，例如价格服务的降级策略。

**完成标准**

- MVP 范围、字段和接口经团队确认。
- 看板中每条 P0 用户故事都包含负责人、优先级和验收标准。
- 四人均可启动项目骨架。
- 首个文档或配置 PR 已获队友人工审查。

### Day 2：规划 - 任务、测试和演示设计

**成员 A 与 B**

- 拆分 API 工作：持仓 CRUD、校验、组合计算、价格服务和错误处理。
- 设计测试：正常流程、空组合、无效数量、无效价格、不存在资源、价格服务失败。
- 确认后端覆盖率工具及 70% 门槛。

**成员 C 与 D**

- 完成低保真页面流程：组合总览、持仓表、添加表单、删除确认、空态、错误态。
- 建立逐条对应验收标准的人工测试清单。
- 确定演示故事线、每人讲解部分及录屏/截图备用方案。

**全员完成标准**

- 每个编码任务均有负责人、对应分支、验收标准和测试方案。
- API 契约与页面流程对齐。
- 只在 P0 完成后才开始 P1/P2。

### Day 3：编码 - MVP 主流程

| 成员 | 当日工作 |
| --- | --- |
| A | 实现数据库、数据访问层、持仓查询/新增/删除 API 和 API 文档。 |
| B | 实现参数校验、统一错误响应、组合市值/盈亏计算及业务单元测试。 |
| C | 实现持仓列表、添加表单、删除、加载态、空态和错误态；完成 API 联调。 |
| D | 执行 API 验收，记录缺陷，维护启动说明、测试记录和演示数据。 |

**完成标准**

- 用户可以通过 API 和 UI 新增、查询和删除持仓。
- `qa` 可集成当日功能。
- 每个已完成功能均通过 PR 和一名队友的真实人工审查。

### Day 4：编码 - 图表、稳定性与交付

| 成员 | 当日工作 |
| --- | --- |
| A、B | 实现当前价格或演示价格降级、边界处理，并将后端覆盖率补至 70% 以上。 |
| C | 完成一个高价值图表，优先资产分布或组合市值；完成页面回归。 |
| D | 执行全量验收，维护架构图、ADR、演示脚本、PPT 和故障备份材料。 |

**全员完成标准**

- 所有 P0 验收标准通过。
- 后端测试通过，行覆盖率不低于 70%。
- `main` 可从零启动并完成演示流程。
- 已完成至少一次完整演练，并准备录屏或截图备用。

## 8. Git 与 PR 规范

### 分支策略

| 分支 | 用途 |
| --- | --- |
| `main` | 可交付版本，始终可运行 |
| `qa` | 功能集成和回归测试 |
| `feature/<story-id>-<short-name>` | 单一用户故事或任务开发 |
| `bugfix/<issue-id>-<short-name>` | 未发布问题修复 |
| `hotfix/<issue-id>-<short-name>` | 已发布/演示版本紧急修复 |
| `prod` | 最终演示或部署版本 |

- 禁止直接提交到 `main` 或 `qa`。
- 每个功能或任务使用独立 `feature/*` 分支。
- 所有合并必须经过 PR 和至少一名队友的人工审查。
- 审查者必须阅读 diff、提出必要问题，并核对验收标准；不得只做形式化批准。
- 完整分支生命周期、`main/qa/feature/hotfix/prod` 创建时机、合并方向和 GitHub Actions 门禁见 [Git 分支与 GitHub 自动化流程](./git_branching_ci_zh.md)。

### Git Commit 规范

提交信息使用以下统一格式：

```text
<type>(<scope>): <简短英文祈使句描述>
```

`scope` 可选，建议使用模块名，例如 `holding`、`portfolio`、`api`、`ui`、`docs` 或 `test`。标题不超过 72 个字符，不使用句号，不写无意义信息。

| Type | 适用场景 |
| --- | --- |
| `feat` | 新增用户可见功能或 API 能力 |
| `fix` | 修复缺陷、异常处理或错误结果 |
| `test` | 新增或修改测试，不改变生产行为 |
| `docs` | README、ADR、架构图、API 文档等变更 |
| `refactor` | 不改变外部行为的代码重构 |
| `chore` | 构建、依赖、配置、CI 或仓库维护 |
| `style` | 不影响逻辑的格式化或样式调整 |

**合格示例：**

```text
feat(holding): add endpoint to create a holding
fix(portfolio): handle empty portfolio valuation
test(api): cover invalid holding quantity
docs(readme): document local startup steps
chore(ci): enforce backend coverage threshold
```

**禁止示例：**

```text
fix
update
wip
change stuff
final version
```

### 提交与 PR 前检查

提交前，作者必须确认：

- [ ] 提交只包含一个清晰目的，不混入无关格式化或他人文件。
- [ ] 相关测试已运行并通过；若无法运行，已在 PR 中说明原因。
- [ ] 未提交密钥、令牌、本地配置、构建产物或 IDE 临时文件。
- [ ] 提交内容与对应 User Story 的验收标准有关。
- [ ] 作者能够解释本次变更的关键实现、测试覆盖和边界处理。

一个功能可以有多个小提交。合并 PR 时可选择保留提交或 squash，但 `qa` 和 `main` 的历史必须清晰、可追溯。每个 PR 描述至少包含：关联 User Story、实现摘要、测试结果、验收标准核对和已知限制。

## 9. 工程质量要求

- 后端必须包含单元测试，行覆盖率至少 70%。
- 测试必须覆盖异常输入、边界条件和失败路径，不能只测试 happy path。
- 修复缺陷前先通过日志、断点、测试和请求响应记录定位原因。
- 任何提交都必须由作者理解并能够解释；无法解释的实现不得合并。
- 编译通过不等于完成，负责人必须依据验收标准手动验证。
- 架构、数据模型、代码结构、类职责和方法命名由团队决定，并同步更新文档。

## 10. 每日协作节奏

- 每日站会 10 分钟：Yesterday / Today / Blockers。
- 每天结束前更新看板、PR 状态、测试结果、风险和阻塞项。
- 遇到超过 30 分钟无法推进的阻塞，立即在团队频道提出并记录处理结果。
- Day 4 进行简短复盘：记录一个做得好的实践、一个问题和一个后续改进项。

## 11. 最终交付检查清单

- [ ] `main` 可启动，且有完整的启动、测试和 API 使用说明。
- [ ] UI 可演示持仓查询、新增、删除和组合汇总。
- [ ] 至少一个组合表现图表可用。
- [ ] 后端测试通过，行覆盖率 `>= 70%`。
- [ ] API 文档、ERD、架构图与实际实现一致。
- [ ] 每个已合并 PR 均有队友人工审查。
- [ ] `qa`、`main`、`prod` 的分支保护和 GitHub Actions required checks 已启用。
- [ ] Git 提交持续、清晰，且四人贡献相对均衡。
- [ ] 四位成员均参与展示，并能解释自己负责和审查的代码。
- [ ] 已准备现场 Demo 失败时的录屏或截图备用方案。
