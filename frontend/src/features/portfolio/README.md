# Portfolio Feature

主要负责人：成员 C。

建议结构：

- `PortfolioDashboard.tsx`：组合页面入口。
- `components/`：SummaryCards、AllocationDonut、HoldingsTable、AddHoldingForm、DeleteHoldingDialog。
- `hooks/`：`usePortfolioData`、`useHoldingActions`。
- `fixtures/`：mock 数据和组件测试数据。

规则：

- feature 组件可以理解 portfolio 业务字段，但不直接调用 `fetch`。
- 添加/删除成功后重新请求后端 holdings 和 summary。
- loading、empty、partial valuation、error、success 状态都要有可见反馈。
