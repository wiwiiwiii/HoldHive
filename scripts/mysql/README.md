# HoldHive Local MySQL

本项目默认使用本机 MySQL，不要求安装 Docker。

## 一次性初始化

```bash
mysql -u root -p < scripts/mysql/init-local-mysql.sql
```

脚本会创建：

- 数据库：`holdhive`
- 用户：`holdhive`
- 密码：`holdhive`

这些值与仓库根目录 `.env.example` 和 `backend/src/main/resources/application-local.yml` 保持一致。

## 启动顺序

```bash
cp .env.example .env
mysql -u root -p -e "SELECT 1"
(cd backend && ./mvnw spring-boot:run)
(cd frontend && npm install && npm run dev)
```

如果本机 MySQL 已经有不同用户名、密码或端口，只修改本地 `.env`，不要提交真实凭据。
