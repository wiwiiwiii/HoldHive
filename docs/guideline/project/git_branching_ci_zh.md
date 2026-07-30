# HoldHive Git 分支与 GitHub 自动化流程

## 1. 决策摘要

HoldHive 采用“轻量 GitFlow + QA 集成门禁”。核心是 `qa` 作为团队集成分支，`main` 作为稳定交付快照。若后续有真实部署环境，可再启用 `prod`；当前 `1.0.0` 交付采用 `qa -> main -> tag`，不强制维护 `prod`。

分支流向固定为：

```text
feature/* / bugfix/* / docs/*  ──PR──>  qa  ──release PR──>  main  ──tag──>  1.0.0
                                      ↑
hotfix/*  <───────────────────────────┘  （仅已发布版本紧急修复时使用）
```

核心原则：

- `main`、`qa` 是受保护分支，不允许直接 push；如果启用 `prod`，也必须受保护。
- `feature/*` 只从 `qa` 创建，只通过 PR 合并回 `qa`。
- `docs/*` 也从 `qa` 创建，只通过 PR 合并回 `qa`，避免文档直接改动 `main` 的发布快照。
- `main` 代表稳定可交付版本；`1.0.0` 这类 tag 代表不可移动的发布快照。
- `hotfix/*` 只用于 `main` 或 `prod` 已发布后出现必须立即修复的问题。
- 发布后必须打 tag；本项目当前使用无 `v` 前缀的语义化版本，例如 `1.0.0`。
- 所有 PR 必须通过 GitHub Actions；CI 失败不得合并。

## 2. 分支何时创建

| 分支 | 创建时间 | 创建来源 | 创建人 | 是否长期存在 | 用途 |
| --- | --- | --- | --- | --- | --- |
| `main` | 仓库初始化时已有 | 初始仓库 | 仓库 owner | 是 | 稳定可交付版本，最终从这里做 release candidate |
| `qa` | Day 1 规划结束前 | `main` | 成员 D 或组长 | 是 | 团队集成、联调、QA 验收 |
| `feature/<story-id>-<short-name>` | 每个 User Story 开始编码前 | `qa` | 对应开发者 | 否 | 单一功能开发，例如 `feature/HH-02-add-holding` |
| `bugfix/<issue-id>-<short-name>` | `qa` 或 `main` 验收发现普通 bug 时 | 目标 bug 所在分支 | 修复人 | 否 | 未发布问题修复，不直接碰 `prod` |
| `hotfix/<issue-id>-<short-name>` | `main` 已发布后出现阻断演示/部署的问题 | `main` | 最熟悉问题的人 | 否 | 已发布版本紧急修复 |
| `docs/<topic>` | 文档或设计稿单独修改时 | `qa` | 文档负责人 | 否 | 文档、API、ERD、设计稿等非代码变更 |
| `prod` | 只有存在真实部署/演示环境隔离需求时 | `main` | 成员 D 或组长 | 可选 | 已部署环境快照，不承接日常开发 |

当前项目没有单独部署环境，因此使用 `main` + tag 替代 `prod`：`main` 保持稳定，`1.0.0` 固定指向正式交付 commit。后续普通文档更新不要移动 tag，应通过新 PR 进入 `qa`。

## 3. 初始化命令

Day 1 完成仓库骨架后创建 `qa`：

```bash
git switch main
git pull origin main
git switch -c qa
git push -u origin qa
```

如果团队决定启用 `prod`，Day 4 验收通过后创建 `prod`：

```bash
git switch main
git pull origin main
git switch -c prod
git push -u origin prod
git tag -a 1.0.0 -m "HoldHive MVP demo release"
git push origin 1.0.0
```

不要提前创建大量空分支。`feature/*`、`bugfix/*`、`docs/*` 和 `hotfix/*` 都应在具体任务开始时创建，任务合并后删除。

## 4. 日常使用流程

### 4.1 功能开发

```bash
git switch qa
git pull origin qa
git switch -c feature/HH-02-add-holding

# coding + local tests
git add backend frontend docs
git commit -m "feat(holding): add holding creation flow"
git push -u origin feature/HH-02-add-holding
```

然后在 GitHub 上创建 PR：

```text
base: qa
compare: feature/HH-02-add-holding
```

合并要求：

- 至少 1 名队友 review。
- `backend`、`backend mysql smoke`、`frontend`、`docs` 通过。
- PR 描述包含 Story ID、实现摘要、测试结果、验收标准和已知限制。
- 合并方式使用 squash merge，保持 `qa` 历史简洁。

### 4.2 QA 集成到 main

当 `qa` 完成一批功能并通过人工验收时，创建 release PR：

```text
base: main
compare: qa
```

合并要求：

- 成员 D 完成验收清单。
- GitHub Actions 中 `backend`、`backend mysql smoke`、`frontend`、`docs` 全部通过。
- API 文档、OpenAPI、数据库 migration 与实现一致。
- 合并方式使用 merge commit，保留一次清晰的 release 节点。

`qa -> main` 不是每天随手合并。建议 Day 3 晚上做一次候选合并，Day 4 最终演示前做一次正式合并。`1.0.0` 已按该流程由 PR #36 合入 `main` 后打 tag。

发布 tag：

```bash
git switch main
git pull --ff-only origin main
git tag -a 1.0.0 -m "Release 1.0.0"
git push origin 1.0.0
```

tag 是发布证据，不随普通文档或小修订移动。如果需要发布补丁，使用新 tag，例如 `1.0.1`。

### 4.3 main 发布到 prod

`main` 稳定后创建 PR：

```text
base: prod
compare: main
```

合并要求：

- 所有 CI 通过。
- QA/交付负责人确认演示脚本通过。
- 不允许包含未验收的 P1/P2。
- 合并后打 tag。

如果启用 `prod`，推荐 tag：

```bash
git switch prod
git pull origin prod
git tag -a 1.0.0 -m "HoldHive MVP demo release"
git push origin 1.0.0
```

### 4.4 Hotfix 流程

只有 `main` 或可选的 `prod` 已发布后才使用 `hotfix/*`。当前项目未启用 `prod` 时，从 `main` 创建 hotfix：

```bash
git switch main
git pull origin main
git switch -c hotfix/HH-99-fix-demo-startup

# fix + tests
git commit -m "fix(startup): restore demo database migration"
git push -u origin hotfix/HH-99-fix-demo-startup
```

合并顺序：

1. PR `hotfix/* -> main`，只包含紧急修复。
2. 合并后打补丁 tag，例如 `1.0.1`。
3. 将同一修复 back-merge 或 cherry-pick 到 `qa`，避免后续功能覆盖 hotfix。
4. 如果启用了 `prod`，再同步到 `prod`。

如果问题尚未发布到 `main` 或 `prod`，不要使用 `hotfix/*`；从 `qa` 创建 `bugfix/*` 即可。

## 5. 分支保护规则

GitHub 仓库设置中为 `qa`、`main` 和 `prod` 分别配置 branch protection 或 ruleset。官方文档说明，受保护分支可以要求 status checks、限制删除和 force push，并要求 PR review 后才能合并。

| 分支 | 必须开启 | 推荐规则 |
| --- | --- | --- |
| `qa` | Pull request、status checks | 1 个 approval；禁止 force push；要求 `backend`、`backend mysql smoke`、`frontend`、`docs` |
| `main` | Pull request、status checks、清晰历史 | 1 个 approval；禁止直接 push；要求 `backend`、`backend mysql smoke`、`frontend`、`docs` |
| `prod` | Pull request、status checks、tag/release gate | 1 个 approval；只允许从 `main` 或 `hotfix/*` 合并；禁止删除；发布后打 tag |

注意：

- 每个 required check 的 job name 必须唯一，避免 GitHub 无法判断哪个 check 应作为门禁。
- 不允许管理员绕过保护规则，除非项目负责人明确要求紧急演示。
- 不使用 force push 修复共享分支；错误提交通过 revert 或新 PR 修复。

## 6. Merge 策略

| 合并方向 | 合并方式 | 原因 |
| --- | --- | --- |
| `feature/* -> qa` | Squash merge | 一个 Story 对应一个清晰集成提交，便于回滚 |
| `docs/* -> qa` | Squash merge | 文档变更保持清晰 |
| `bugfix/* -> qa/main` | Squash merge | 普通修复独立可追溯 |
| `qa -> main` | Merge commit | 保留一次 release candidate 边界 |
| `main -> prod` | Merge commit 或 fast-forward | 代表正式发布节点 |
| `hotfix/* -> prod` | Squash 或 merge commit | 视修复复杂度；必须 back-merge/cherry-pick 到 `main` 和 `qa` |

禁止：

- `feature/*` 直接合并到 `main` 或 `prod`。
- 从 `prod` 反向开发新功能。
- 把多个无关 Story 合并在同一个 feature PR。
- 为了“赶时间”跳过 PR 和 CI。

## 7. GitHub Actions 自动化设计

### 7.1 工作流文件

当前仓库已有主工作流：

```text
.github/workflows/
└── pr-check.yml        # PR/push 门禁：后端、MySQL smoke、前端、文档检查
```

后续如有部署环境，再新增 `release.yml` 或环境审批 workflow；当前 `1.0.0` 以 `pr-check.yml` 作为发布质量门禁。

### 7.2 PR 门禁

触发条件：

```yaml
on:
  pull_request:
    branches: [qa, main]
```

检查内容：

| Job | 技术栈 | 命令 |
| --- | --- | --- |
| `backend` | Java 21、Spring Boot、Maven、Flyway、JaCoCo | `./mvnw -B verify` |
| `backend mysql smoke` | MySQL 8.4 service container + Spring Boot local profile | package 后启动后端，访问 health、market search、create cash holding、summary |
| `frontend` | React、TypeScript、Vite、Vitest | `npm ci`、`npm run test -- --run`、`npm run build` |
| `docs` | 文档存在性检查 | 检查核心 guideline/design 文档存在 |

### 7.3 QA 集成

当前 `pr-check.yml` 已覆盖 push 到 `qa`：

```yaml
on:
  push:
    branches: [qa]
```

检查内容：

- 后端执行 `./mvnw -B verify`。
- `backend mysql smoke` 连接 MySQL service container，执行 Flyway migration 和基础 API smoke。
- 前端执行 test + build。
- 文档 job 检查核心文档仍存在。
- 如果后续有时间，可追加 Playwright 冒烟：Gateway -> Add Holding -> Dashboard -> Analysis。

### 7.4 Release 验证

触发条件：

```yaml
on:
  push:
    branches: [main]
    tags: ["*.*.*"]
```

检查内容：

- 完整后端 verify。
- 前端生产 build。
- 本地 MySQL 环境模板、Flyway migration 和 profile 配置检查。
- 上传后端 jar、前端 dist、测试报告和覆盖率报告。
- 如果后续新增 `prod` 或专门 release workflow，再设置 `environment: production-demo`，由成员 D 手动确认。

## 8. 当前 GitHub Actions

当前仓库使用 `.github/workflows/pr-check.yml`，覆盖 PR 和 push：

```yaml
on:
  pull_request:
    branches: [qa, main]
  push:
    branches: [qa, main]
```

| Job | 目的 | 关键命令/动作 |
| --- | --- | --- |
| `backend` | 后端单元、集成、Flyway 和 JaCoCo 检查 | `cd backend && ./mvnw -B verify` |
| `backend mysql smoke` | 用 MySQL 8.4 service container 验证真实数据库启动、Flyway migration 和基础 API | package 后启动后端；curl health、market search、创建现金持仓、summary |
| `frontend` | 前端依赖安装、Vitest 和生产构建 | `npm ci`、`npm run test -- --run`、`npm run build` |
| `docs` | 防止核心文档缺失 | 检查 API、数据库、团队指南和蓝湖说明存在 |

### 8.1 可选后续增强

如果项目继续演进，可新增：

- `release.yml`：仅在 tag 或 `main` push 后打包后端 jar 和前端 dist。
- Playwright smoke：启动前后端后跑浏览器级路径，覆盖 Gateway -> Add Holding -> Dashboard -> Analysis。
- OpenAPI diff：防止 DTO 字段变更没有同步 API 文档和前端类型。

## 9. 自动化门禁矩阵

| 合并目标 | 必须通过 | 人工动作 |
| --- | --- | --- |
| `feature/* -> qa` | `backend`、`backend mysql smoke`、`frontend`、`docs` | 1 名队友 review |
| `docs/* -> qa` | `docs`；若改动 API/运行流程，也确认 `backend`、`frontend` | 1 名队友 review |
| `bugfix/* -> qa` | 受影响模块 + 全量 `pr-check` | 1 名队友 review |
| `qa -> main` | `backend`、`backend mysql smoke`、`frontend`、`docs` | 成员 D 完成验收清单 |
| `hotfix/* -> main` | 受影响模块 + 全量 `pr-check` | 最少 1 名相关负责人 review |

## 10. 四天落地计划

| 时间 | Git/CI 动作 | 负责人 |
| --- | --- | --- |
| Day 1 Planning | 创建 `qa`；配置 branch protection；提交 workflow 草案 | 成员 D + 成员 A |
| Day 2 Planning | 冻结 API 契约；让 `pr-check` 至少能跑通空项目或骨架项目 | 成员 A + 成员 C |
| Day 3 Coding | 所有 Story 用 `feature/* -> qa`；中午和收工前各做一次 qa 集成 | 全员 |
| Day 4 Coding | `qa -> main`；验收通过后在 `main` 打 `1.0.0` | 成员 D + 全员 |

## 11. 常见冲突处理

- 后端 migration 冲突：不改已合并 migration，新建更高版本 `V<n>__*.sql`。
- OpenAPI 字段冲突：先更新 API 文档和 mock，再合并实现。
- 前后端都改 DTO：以 `api_documentation_zh.md` 和 OpenAPI 为准。
- `qa` 红灯：暂停合并新 feature，优先建 `bugfix/*` 修 CI。
- 已发布版本出问题：从 `main` 建 `hotfix/*`，合回 `main` 后再同步回 `qa`；不要从 `qa` 带入未验收功能。

## 12. 参考链接

| 主题 | 链接 |
| --- | --- |
| GitHub protected branches | https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches |
| GitHub Actions workflow syntax | https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax |
| GitHub Actions concurrency | https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency |
| actions/setup-java | https://github.com/actions/setup-java |
| actions/setup-node | https://github.com/actions/setup-node |
| actions/upload-artifact | https://github.com/actions/upload-artifact |
