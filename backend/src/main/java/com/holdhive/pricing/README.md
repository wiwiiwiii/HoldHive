# Pricing Module

主要负责人：成员 B。

目录边界：

- `application/`：价格查询用例和价格模式选择。
- `domain/`：报价值对象、价格状态和业务含义。
- `infrastructure/`：东方财富、演示价格、缓存或其他外部数据适配器。

规则：

- 外部行情响应只在 `infrastructure/` 解析。
- 对 portfolio 暴露稳定的 pricing service 或 adapter 接口。
- 外部价格失败时返回明确状态，不伪造实时价格。
- 测试中用 Mockito 或 stub 隔离网络调用。
