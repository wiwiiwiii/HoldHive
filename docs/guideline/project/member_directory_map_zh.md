# HoldHive 成员目录分工速查

本文件用于成员拉取仓库后的第一天启动。详细职责、时间计划和 PR 规则见 `team_project_guideline_zh.md`；这里保留最短路径，避免成员改错目录。

## 成员 A：后端 API + 数据库

主要目录：

- `backend/src/main/java/com/holdhive/portfolio/api/`
- `backend/src/main/java/com/holdhive/portfolio/persistence/`
- `backend/src/main/resources/db/migration/`
- `scripts/mysql/`

优先任务：

1. 用 Flyway 建表，保证空 MySQL 可自动迁移。
2. 实现 holdings 查询、新增、删除 API。
3. 保持 DTO 与 `docs/guideline/project/api_documentation_zh.md` 一致。

不要直接改：

- `portfolio/domain/` 中的估值公式。
- `pricing/infrastructure/` 中的外部行情适配器。

## 成员 B：后端业务计算 + 质量

主要目录：

- `backend/src/main/java/com/holdhive/portfolio/application/`
- `backend/src/main/java/com/holdhive/portfolio/domain/`
- `backend/src/main/java/com/holdhive/pricing/`
- `backend/src/main/java/com/holdhive/common/error/`
- `backend/src/test/java/com/holdhive/`

优先任务：

1. 实现组合市值、成本、盈亏和配置占比计算。
2. 实现 demo/market pricing adapter 的可替换边界。
3. 用 JUnit、Mockito、MockMvc 把核心路径覆盖到 JaCoCo 70% 以上。

不要直接改：

- 已合并的 Flyway migration。
- 前端页面组件。

## 成员 C：前端页面 + 联调

主要目录：

- `frontend/src/api/`
- `frontend/src/features/portfolio/`
- `frontend/src/components/`
- `frontend/src/styles/`
- `frontend/src/test/`

优先任务：

1. 通过 `src/api` 统一访问后端，不在组件里拼接 URL。
2. 实现 Dashboard、持仓表、添加表单、删除确认、状态提示和图表。
3. 用 fixture/mock 先开发，再切换到真实 API 联调。

不要直接改：

- 后端数据库 migration。
- 后端业务计算公式。

## 成员 D：QA + CI + 交付

主要目录：

- `.github/workflows/`
- `.github/pull_request_template.md`
- `docs/qa/`
- `docs/demo/`
- `docs/adr/`
- `docs/guideline/`
- `docs/design/`
- `.env.example`

优先任务：

1. 维护 PR 模板、GitHub Actions、验收清单和缺陷记录。
2. 验证每位成员可从本机 MySQL、后端、前端完整启动。
3. 维护 PDF、蓝湖设计图和演示脚本。

不要直接改：

- API 字段、数据库字段或价格计算逻辑；需要修改时先开契约变更 PR。

## 共享规则

- 跨负责人目录改动必须在 PR 描述中说明原因。
- API 字段变更必须同步后端 DTO、前端 `types.ts`、API 文档和测试。
- 数据库变更只新增 migration，不修改已被他人 pull 的旧 migration。
- 本地启动不依赖 Docker；每人使用自己的本机 MySQL。
- `main`、`qa`、`prod` 不直接 push，全部通过 PR。
