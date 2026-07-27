# Backend Tests

主要负责人：成员 B，成员 A 负责数据库和 Repository 相关测试。

建议分层：

- `portfolio/domain/`：纯 Java 单元测试，不启动 Spring。
- `portfolio/application/`：Service 测试，使用 Mockito mock Repository 和 PricingAdapter。
- `portfolio/api/`：MockMvc 测试 HTTP 契约、校验和错误响应。
- `portfolio/persistence/`：Repository/Flyway 测试，验证数据库映射和约束。
- `pricing/`：价格服务、fallback 和 adapter 转换测试。
- `common/error/`：统一异常响应测试。

运行：

```bash
cd backend
./mvnw verify
```
