# Shared Kernel Guide

本文档定义母版中可以沉淀为未来 `anjing-common` / `anjing-web-common` 的共享内核边界。目标不是现在拆多模块，而是先保证可共享的类保持干净，未来拆服务时不用从业务代码里硬挖。

## Shared Kernel

共享内核只放跨项目稳定复用的契约和工具：

- API 路径与请求头：`ApiConstants`、`RequestHeaderConstants`
- 机器生成平台契约：`PlatformContractConstants`
- 错误码与异常：`ErrorCode`、各类 `XxxErrorCode`、`BizException`、`SystemException`
- 请求/响应契约：`BaseRequest`、`GlobalRequestContext`、`PageRequest`、`APIResponse`、`PageResult`
- 上下文持有：`GlobalRequestContextHolder`，包含纯 Java 的 `capture/restore` 辅助方法，供异步和定时任务 adapter 复用
- 纯工具：`DateUtils`、`IdUtils`、`LocaleUtils`、`StringUtils`、`TimeZoneUtils`、`ValidationUtils`

这些类未来可以优先抽到共享 jar。为了保持可抽取，它们不能反向依赖 Controller、Config、Aspect、Client、Example 等运行时层，也不能依赖 Spring Web、Servlet、JPA。

## Runtime Adapters

以下能力目前属于运行时适配层，不进入共享内核：

- `RequestContextFilter`：Web 请求入口适配。
- `RequestContextTaskDecorator`、`AsyncConfig`：异步线程池上下文和 MDC 传播适配。
- `RemoteHttpClient`、`ServiceEndpointResolver`、`ServiceEndpointRegistry`、`RemoteCallerResolver`、`RemoteCallPolicy`、`ConfiguredRemoteCallPolicy`、`RemoteCallObserver`、`RemoteCallWrapper`：远程调用、服务地址解析、服务发现/地址注册表、调用方身份解析、轻量配置型调用治理策略、调用审计观察与重试适配。
- `JsonUtils`：已提供无 Spring 容器默认 `ObjectMapper`，运行时仍可由 Spring 注入配置后的 mapper 覆盖；未来可进一步拆成纯 JSON helper 与 Spring adapter 两层。
- `ExceptionUtils`、`SqlLogUtils`：依赖 Servlet 或 Web 上下文。
- `PageResponse`：旧分页兼容类，保留给历史响应格式，不作为新共享契约。

## Rules

- 共享内核类禁止出现 `org.springframework.*`、`jakarta.servlet.*`、`jakarta.persistence.*`。
- 共享内核类禁止使用 `@Component`、`@Service`、`@Configuration`、`@Autowired` 等运行时注解。
- 标准分页响应 `PageResult` 不依赖 Spring Data `Page`；使用 Spring Page 时在业务层展开字段。
- 时间、ID、语言、字符串、校验这类工具优先保持纯 Java；需要框架能力时拆 adapter。
- 异步上下文传播时，共享内核只保留 `GlobalRequestContextHolder.capture()` / `setOrClear()` 等纯 Java 方法；MDC、线程池和 Spring `TaskDecorator` 留在运行时适配层。
- `LocaleUtils` 只依赖 platform contract 和 JDK `Locale`，用于把 `Accept-Language` 归一化到母版声明的支持语言。
- `TimeZoneUtils` 只依赖 platform contract 和 JDK `ZoneId`，用于把 `X-Time-Zone` 归一化到合法时区或默认 UTC。
- 新增共享候选类时，同步加入 `scripts/check-shared-kernel.js` 的文件清单。

## Verification

发布母版或复制项目前运行：

```bash
node scripts/check-shared-kernel.js
```

该脚本会检查共享内核候选类是否引入运行时框架依赖。完整母版自检会通过 `./scripts/check-contracts.sh` 间接运行该脚本。
