# Frontend API Layer

主要负责人：成员 C；API 字段由成员 A/B 一起 review。

职责：

- 集中维护后端 REST API 调用。
- 集中维护 TypeScript DTO。
- 将 HTTP 错误转换为 UI 可理解的错误对象。

规则：

- 组件只能通过本目录调用后端。
- API 路径必须与 `docs/guideline/project/api_documentation_zh.md` 保持一致。
- 变更 DTO 时同步测试 fixture 和前端组件测试。
