# Common Module

共同维护目录。

目录边界：

- `api/`：跨模块可用的基础 API，例如 health check。
- `config/`：CORS、OpenAPI、profile 等 Spring 配置。
- `error/`：统一错误响应、错误码、异常处理。

规则：

- `common` 不放 portfolio 或 pricing 的业务规则。
- 新增通用工具前先确认至少两个模块会使用。
- 错误响应字段变更必须同步 API 文档和前端类型。
