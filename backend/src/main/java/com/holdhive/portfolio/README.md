# Portfolio Module

主要负责人：

- 成员 A：`api/`、`persistence/`、`persistence/entity/`、`persistence/repository/`
- 成员 B：`application/`、`domain/`

依赖方向：

```text
api -> application -> domain
application -> persistence repository
persistence -> MySQL/JPA
```

规则：

- Controller 只处理 HTTP、校验入口和响应 DTO。
- Service 负责用例、事务和编排。
- Domain 只放估值、盈亏、配置占比等纯业务规则，不依赖 Spring/JPA。
- Persistence 只放 Entity、Repository 和数据库映射，不写业务计算。
