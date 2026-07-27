# HoldHive Contributing

本项目按四人分工推进。开发前先从 `qa` 拉分支，不直接推送 `main`、`qa` 或 `prod`。

## 本地启动

本机开发不需要 Docker。

```bash
cp .env.example .env
mysql -u root -p < scripts/mysql/init-local-mysql.sql
(cd backend && ./mvnw spring-boot:run)
(cd frontend && npm install && npm run dev)
```

健康检查：

```bash
curl http://localhost:8080/api/v1/health
```

## 分工目录

| 成员 | 分支建议 | 主要目录 |
| --- | --- | --- |
| 成员 A | `feature/backend-crud-db` | `backend/src/main/java/com/holdhive/portfolio/api/`、`portfolio/persistence/`、`backend/src/main/resources/db/migration/`、`scripts/mysql/` |
| 成员 B | `feature/backend-summary-pricing` | `portfolio/application/`、`portfolio/domain/`、`pricing/`、`common/error/`、`backend/src/test/java/com/holdhive/` |
| 成员 C | `feature/frontend-dashboard` | `frontend/src/api/`、`frontend/src/features/portfolio/`、`frontend/src/components/`、`frontend/src/styles/` |
| 成员 D | `feature/qa-ci-docs` | `.github/`、`docs/qa/`、`docs/demo/`、`docs/adr/`、`docs/guideline/`、`docs/design/` |

详细目录边界见 `docs/guideline/project/member_directory_map_zh.md`。

## 日常分支

```bash
git switch qa
git pull origin qa
git switch -c feature/HH-xx-short-name
```

完成后提交、推送并开 PR 到 `qa`：

```bash
git add <changed-files>
git commit -m "feat(scope): describe the change"
git push -u origin feature/HH-xx-short-name
```

## 协作边界

- 前端组件只通过 `frontend/src/api/` 调后端。
- 后端 `domain/` 不依赖 Spring、JPA、HTTP client 或外部行情 API。
- 数据库变更只新增 Flyway migration，不修改已经共享的旧 migration。
- API 字段变更必须同步后端 DTO、前端 `types.ts`、API 文档和测试。
