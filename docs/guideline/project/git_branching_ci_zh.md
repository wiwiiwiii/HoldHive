# HoldHive Git 分支与 GitHub 自动化流程

## 1. 决策摘要

HoldHive 采用“轻量 GitFlow + QA 集成门禁”。它比单纯 GitHub Flow 多一个 `qa` 集成分支和一个 `prod` 发布分支，但不会引入复杂 release train。目标是服务四天项目节奏：两天规划时把分支、保护规则和 CI 建起来；两天 coding 时所有功能通过 PR 进入 `qa`，验收后再进入 `main` 和 `prod`。

分支流向固定为：

```text
feature/*  ──PR──>  qa  ──release PR──>  main  ──deploy PR/tag──>  prod
                         ↑                               │
hotfix/*  <──────────────┴──────── back-merge/cherry-pick ┘
```

核心原则：

- `main`、`qa`、`prod` 都是受保护分支，不允许直接 push。
- `feature/*` 只从 `qa` 创建，只通过 PR 合并回 `qa`。
- `main` 代表稳定可交付版本；`prod` 代表正式演示/部署版本。
- `hotfix/*` 只用于 `prod` 已经发布后出现必须立即修复的问题。
- `prod` 合并后必须打 tag，例如 `v0.1.0`、`v0.1.1`。
- 所有 PR 必须通过 GitHub Actions；CI 失败不得合并。

## 2. 分支何时创建

| 分支 | 创建时间 | 创建来源 | 创建人 | 是否长期存在 | 用途 |
| --- | --- | --- | --- | --- | --- |
| `main` | 仓库初始化时已有 | 初始仓库 | 仓库 owner | 是 | 稳定可交付版本，最终从这里做 release candidate |
| `qa` | Day 1 规划结束前 | `main` | 成员 D 或组长 | 是 | 团队集成、联调、QA 验收 |
| `feature/<story-id>-<short-name>` | 每个 User Story 开始编码前 | `qa` | 对应开发者 | 否 | 单一功能开发，例如 `feature/HH-02-add-holding` |
| `bugfix/<issue-id>-<short-name>` | `qa` 或 `main` 验收发现普通 bug 时 | 目标 bug 所在分支 | 修复人 | 否 | 未发布问题修复，不直接碰 `prod` |
| `hotfix/<issue-id>-<short-name>` | `prod` 已发布后出现阻断演示/部署的问题 | `prod` | 最熟悉问题的人 | 否 | 生产/演示紧急修复 |
| `docs/<topic>` | 文档或设计稿单独修改时 | `qa` | 文档负责人 | 否 | 文档、API、ERD、设计稿等非代码变更 |
| `prod` | Day 4 验收通过或需要部署时 | `main` | 成员 D 或组长 | 是 | 已发布/演示版本，不承接日常开发 |

如果没有真实部署环境，`prod` 仍可作为“最终演示快照”分支使用；如果项目评审方明确不需要 `prod`，则用 `main` + tag 替代，但本指南仍保留 `prod` 流程。

## 3. 初始化命令

Day 1 完成仓库骨架后创建 `qa`：

```bash
git switch main
git pull origin main
git switch -c qa
git push -u origin qa
```

Day 4 验收通过后创建 `prod`：

```bash
git switch main
git pull origin main
git switch -c prod
git push -u origin prod
git tag -a v0.1.0 -m "HoldHive MVP demo release"
git push origin v0.1.0
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
- `backend-ci`、`frontend-ci`、`contract-check` 通过。
- PR 描述包含 Story ID、实现摘要、测试结果、验收标准和已知限制。
- 合并方式使用 squash merge，保持 `qa` 历史简洁。

### 4.2 QA 集成到 main

当 `qa` 完成一批 P0 功能并通过人工验收时，创建 release PR：

```text
base: main
compare: qa
```

合并要求：

- 成员 D 完成验收清单。
- 后端覆盖率 `>= 70%`。
- 前端 build 通过。
- API 文档、OpenAPI、数据库 migration 与实现一致。
- 合并方式使用 merge commit，保留一次清晰的 release 节点。

`qa -> main` 不是每天随手合并。建议 Day 3 晚上做一次候选合并，Day 4 最终演示前做一次正式合并。

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

推荐 tag：

```bash
git switch prod
git pull origin prod
git tag -a v0.1.0 -m "HoldHive MVP demo release"
git push origin v0.1.0
```

### 4.4 Hotfix 流程

只有 `prod` 已发布后才使用 `hotfix/*`：

```bash
git switch prod
git pull origin prod
git switch -c hotfix/HH-99-fix-demo-startup

# fix + tests
git commit -m "fix(startup): restore demo database migration"
git push -u origin hotfix/HH-99-fix-demo-startup
```

合并顺序：

1. PR `hotfix/* -> prod`，只包含紧急修复。
2. 合并后打补丁 tag，例如 `v0.1.1`。
3. 将同一修复 back-merge 或 cherry-pick 到 `main`。
4. 将同一修复 back-merge 或 cherry-pick 到 `qa`，避免后续功能覆盖 hotfix。

如果问题尚未发布到 `prod`，不要使用 `hotfix/*`；从 `qa` 或 `main` 创建 `bugfix/*` 即可。

## 5. 分支保护规则

GitHub 仓库设置中为 `qa`、`main` 和 `prod` 分别配置 branch protection 或 ruleset。官方文档说明，受保护分支可以要求 status checks、限制删除和 force push，并要求 PR review 后才能合并。

| 分支 | 必须开启 | 推荐规则 |
| --- | --- | --- |
| `qa` | Pull request、status checks | 1 个 approval；禁止 force push；要求 `backend-ci`、`frontend-ci`、`contract-check` |
| `main` | Pull request、status checks、线性或清晰历史 | 1 个 approval；禁止直接 push；要求 `full-ci`、`coverage-check`、`frontend-build` |
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

建议在正式建项时新增以下文件：

```text
.github/workflows/
├── pr-check.yml        # PR 门禁：后端、前端、契约检查
├── qa-integration.yml  # push 到 qa 后的集成验证
└── release.yml         # main/prod/tag 的发布候选验证与 artifact
```

当前文档只提供方案和 YAML 模板，不直接创建 `.github/workflows`，避免在文档交付阶段影响主仓库。

### 7.2 PR 门禁

触发条件：

```yaml
on:
  pull_request:
    branches: [qa, main, prod]
```

检查内容：

| Job | 技术栈 | 命令 |
| --- | --- | --- |
| `backend-ci` | Java 21、Spring Boot、Maven、MySQL 8.4、Flyway、JaCoCo | `./mvnw -B clean verify` |
| `frontend-ci` | React、TypeScript、Vite、Vitest | `npm ci`、`npm run test -- --run`、`npm run build` |
| `contract-check` | OpenAPI、DTO、API 文档 | 生成 OpenAPI 并与文档/示例字段核对 |

### 7.3 QA 集成

触发条件：

```yaml
on:
  push:
    branches: [qa]
  workflow_dispatch:
```

检查内容：

- 后端连接 MySQL service container，执行 Flyway migration 和测试。
- 前端使用 mock 和 live API 配置各跑一次 build 或 smoke。
- 上传测试报告、覆盖率报告和前端 build artifact。
- 如果有时间，跑 Playwright 冒烟：空组合 -> 添加 AAPL -> 看到摘要和图表 -> 删除 AAPL。

### 7.4 Release 验证

触发条件：

```yaml
on:
  push:
    branches: [main, prod]
    tags: ["v*"]
```

检查内容：

- 完整后端 verify。
- 前端生产 build。
- 本地 MySQL 环境模板、Flyway migration 和 profile 配置检查。
- 上传后端 jar、前端 dist、测试报告和覆盖率报告。
- `prod` 或 tag 构建时设置 `environment: production-demo`，由成员 D 手动确认。

## 8. 可执行 GitHub Actions 模板

以下模板按本项目技术栈编写。当前建议使用 `actions/checkout@v7`、`actions/setup-java@v5`、`actions/setup-node@v7` 和 `actions/upload-artifact@v7`。如果学校使用较旧 GitHub Enterprise Server，`upload-artifact@v4+` 可能不可用，需要按平台版本回退。

### 8.1 `pr-check.yml`

```yaml
name: pr-check

on:
  pull_request:
    branches: [qa, main, prod]

permissions:
  contents: read

concurrency:
  group: pr-check-${{ github.event.pull_request.number }}
  cancel-in-progress: true

jobs:
  backend-ci:
    name: backend-ci
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.4
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: holdhive_test
          MYSQL_USER: holdhive
          MYSQL_PASSWORD: holdhive
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping -h 127.0.0.1 -uroot -proot"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=10
    defaults:
      run:
        working-directory: backend
    env:
      SPRING_PROFILES_ACTIVE: test
      DB_URL: jdbc:mysql://127.0.0.1:3306/holdhive_test
      DB_USERNAME: holdhive
      DB_PASSWORD: holdhive
      MARKET_PROVIDER: DEMO
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "21"
          cache: maven
          cache-dependency-path: backend/pom.xml
      - name: Backend verify
        run: ./mvnw -B clean verify

  frontend-ci:
    name: frontend-ci
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-node@v7
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run test -- --run
      - run: npm run build

  contract-check:
    name: contract-check
    runs-on: ubuntu-latest
    needs: [backend-ci]
    steps:
      - uses: actions/checkout@v7
      - name: Check API docs placeholders
        run: |
          test -f docs/guideline/project/api_documentation_zh.md
          test -f docs/guideline/project/database_design_zh.md
          test -f docs/guideline/project/market_data_api_zh.md
          ! grep -R "待补充\\|未定" docs/guideline/project/api_documentation_zh.md
```

### 8.2 `qa-integration.yml`

```yaml
name: qa-integration

on:
  push:
    branches: [qa]
  workflow_dispatch:

permissions:
  contents: read

concurrency:
  group: qa-integration-${{ github.ref }}
  cancel-in-progress: true

jobs:
  full-ci:
    name: full-ci
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.4
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: holdhive_test
          MYSQL_USER: holdhive
          MYSQL_PASSWORD: holdhive
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping -h 127.0.0.1 -uroot -proot"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=10
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "21"
          cache: maven
          cache-dependency-path: backend/pom.xml
      - name: Backend verify
        working-directory: backend
        run: ./mvnw -B clean verify
        env:
          SPRING_PROFILES_ACTIVE: test
          DB_URL: jdbc:mysql://127.0.0.1:3306/holdhive_test
          DB_USERNAME: holdhive
          DB_PASSWORD: holdhive
          MARKET_PROVIDER: DEMO
      - uses: actions/setup-node@v7
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Frontend verify
        working-directory: frontend
        run: |
          npm ci
          npm run test -- --run
          npm run build
      - uses: actions/upload-artifact@v7
        with:
          name: qa-build-artifacts
          path: |
            backend/target/*.jar
            frontend/dist
            backend/target/site/jacoco
          if-no-files-found: warn
          retention-days: 7
```

### 8.3 `release.yml`

```yaml
name: release

on:
  push:
    branches: [main, prod]
    tags: ["v*"]
  workflow_dispatch:

permissions:
  contents: read

jobs:
  release-candidate:
    name: release-candidate
    runs-on: ubuntu-latest
    environment: ${{ github.ref_name == 'prod' && 'production-demo' || 'release-check' }}
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "21"
          cache: maven
          cache-dependency-path: backend/pom.xml
      - name: Package backend
        working-directory: backend
        run: ./mvnw -B clean verify package
        env:
          SPRING_PROFILES_ACTIVE: test
          MARKET_PROVIDER: DEMO
      - uses: actions/setup-node@v7
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Build frontend
        working-directory: frontend
        run: |
          npm ci
          npm run build
      - uses: actions/upload-artifact@v7
        with:
          name: holdhive-release-${{ github.sha }}
          path: |
            backend/target/*.jar
            frontend/dist
          if-no-files-found: error
          retention-days: 14
```

## 9. 自动化门禁矩阵

| 合并目标 | 必须通过 | 人工动作 |
| --- | --- | --- |
| `feature/* -> qa` | `backend-ci`、`frontend-ci`、`contract-check` | 1 名队友 review |
| `docs/* -> qa` | `contract-check`，若影响技术栈则跑 full CI | 1 名队友 review |
| `qa -> main` | `full-ci`、覆盖率、前端 build、API 文档检查 | 成员 D 完成验收清单 |
| `main -> prod` | `release-candidate` | 成员 D 或组长批准 production-demo environment |
| `hotfix/* -> prod` | 受影响模块测试 + release-candidate | 最少 1 名后端/前端相关负责人 review |

## 10. 四天落地计划

| 时间 | Git/CI 动作 | 负责人 |
| --- | --- | --- |
| Day 1 Planning | 创建 `qa`；配置 branch protection；提交 workflow 草案 | 成员 D + 成员 A |
| Day 2 Planning | 冻结 API 契约；让 `pr-check` 至少能跑通空项目或骨架项目 | 成员 A + 成员 C |
| Day 3 Coding | 所有 Story 用 `feature/* -> qa`；中午和收工前各做一次 qa 集成 | 全员 |
| Day 4 Coding | `qa -> main`；验收通过后 `main -> prod` 并打 `v0.1.0` | 成员 D + 全员 |

## 11. 常见冲突处理

- 后端 migration 冲突：不改已合并 migration，新建更高版本 `V<n>__*.sql`。
- OpenAPI 字段冲突：先更新 API 文档和 mock，再合并实现。
- 前后端都改 DTO：以 `api_documentation_zh.md` 和 OpenAPI 为准。
- `qa` 红灯：暂停合并新 feature，优先建 `bugfix/*` 修 CI。
- `prod` 出问题：只建 `hotfix/*`，不要从 `qa` 带入未验收功能。

## 12. 参考链接

| 主题 | 链接 |
| --- | --- |
| GitHub protected branches | https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches |
| GitHub Actions workflow syntax | https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax |
| GitHub Actions concurrency | https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency |
| actions/setup-java | https://github.com/actions/setup-java |
| actions/setup-node | https://github.com/actions/setup-node |
| actions/upload-artifact | https://github.com/actions/upload-artifact |
