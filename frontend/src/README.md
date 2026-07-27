# Frontend Source

主要负责人：成员 C。

依赖方向：

```text
App.tsx
  -> features/portfolio
  -> api
  -> backend REST API

components -> styles
```

规则：

- 组件不直接拼接 URL，不直接访问第三方行情接口。
- API DTO 统一放在 `api/types.ts`。
- 页面状态和数据加载优先放在 feature hook。
- 图表只展示后端 summary/holding 数据，不在前端复制权威估值公式。
