# Remote Call Guide

本文档定义脚手架里的远程调用默认路径。当前母版仍保持单体可运行，但所有新增远程调用都应按未来服务拆分后的方式设计。

## 默认选择

- HTTP 服务间调用：优先使用 `RemoteHttpClient` / `RemoteHttpRequest`。
- 内部服务调用：优先传 `serviceId + path`，服务地址由 `ServiceEndpointResolver` 解析；默认实现从 `app.remote-http.service-base-urls` 读取。
- 第三方外部 API：可以继续传绝对 `url`，但不要在业务代码里拼接内部服务地址。
- Feign / Dubbo / WebClient 自定义适配器：使用 `RemoteCallWrapper.serviceCallHeaders(callerId)` 生成透传头。
- 只需要包装已有 Java 方法调用：使用 `RemoteCallWrapper.call(...)` 或 `callWithRetry(...)`。

## HTTP 调用示例

```java
RemoteHttpRequest request = RemoteHttpRequest.builder()
        .method(HttpMethod.GET)
        .serviceId(ServiceBoundaryConstants.Auth.OWNER)
        .path(ApiConstants.Auth.ME_FULL)
        .callerId(ServiceBoundaryConstants.APPLICATION_ID)
        .retryCount(1)
        .checkResponse(true)
        .build();

APIResponse<CurrentUserResponse> response = remoteHttpClient.exchange(
        request,
        new ParameterizedTypeReference<APIResponse<CurrentUserResponse>>() {}
);
```

简单响应可以继续使用 `Class<T>` 重载；标准响应、分页响应或列表响应优先使用 `ParameterizedTypeReference<T>`，避免 `APIResponse<PageResult<Xxx>>` 这类泛型在服务间调用中退化为裸类型。

## 统一透传头

`RemoteHttpClient` 会按照 `contracts/platform-contract.json` 的 `backendPropagatedHeaders` 自动透传：

- `X-Request-Id`
- `X-Trace-Id`
- `X-Tenant-Id`
- `X-User-Id`
- `X-User-Name`
- `X-User-Roles`
- `X-Caller-Id`
- `X-Time-Zone`
- `Accept-Language`

没有入站请求上下文时，会生成新的 `X-Request-Id` 和 `X-Trace-Id`，适用于定时任务、异步任务和本地工具入口。

浏览器端只允许透传 `frontendPropagatedHeaders` 中的 requestId、traceId、timezone、language；租户、用户和 caller 这类身份上下文由网关、认证层或后端服务间调用链路生成和透传。

## 配置

默认配置在 `app.remote-http`：

```yaml
app:
  remote-http:
    default-caller-id: infra-dev-scaffolding
    connect-timeout-ms: 3000
    read-timeout-ms: 5000
    default-retry-count: 0
    default-retry-interval-ms: 1000
    policy:
      enabled: ${REMOTE_HTTP_POLICY_ENABLED:false}
      blocked-service-ids: ${REMOTE_HTTP_POLICY_BLOCKED_SERVICE_IDS:}
      allowed-caller-ids: ${REMOTE_HTTP_POLICY_ALLOWED_CALLER_IDS:}
      allowed-caller-ids-by-service:
        infra-auth:
          - infra-api-gateway
    service-base-urls:
      infra-dev-scaffolding: http://localhost:18080
      infra-auth: ${INFRA_AUTH_BASE_URL:}
```

`ConfiguredServiceEndpointResolver` 是默认的服务解析实现，负责校验 serviceId 并拼接 service-relative path。它通过 `ServiceEndpointRegistry` 查询服务 base URL；默认 `ConfiguredServiceEndpointRegistry` 读取 `service-base-urls`。

母版不默认引入注册中心；下游项目可以先用环境变量切换服务地址，再在真实微服务项目中替换 `ServiceEndpointRegistry`，接入 API Gateway、Service Discovery、区域路由或灰度路由。`ServiceEndpoint` 的 `source` 字段用于标记 endpoint 来源，例如 configuration、gateway、discovery、region 或 gray。

## 调用方身份扩展点

`RemoteCallerResolver` 是服务间调用的调用方身份解析扩展点。默认 `DefaultRemoteCallerResolver` 先使用 `RemoteHttpRequest.callerId`，再使用 `app.remote-http.default-caller-id`，最后回退到 `ServiceBoundaryConstants.APPLICATION_ID`。

下游如果接入认证中心、API Gateway、服务网格或服务注册策略，可以定义自己的 `RemoteCallerResolver` bean，统一决定 `X-Caller-Id`。业务代码不应在每个调用点手写调用方身份规则。

## 调用策略扩展点

`RemoteCallPolicy` 是远程调用治理扩展点。母版默认注册 `ConfiguredRemoteCallPolicy`，但 `app.remote-http.policy.enabled=false` 时行为等同 Noop，不会限流或熔断；下游项目可以通过配置先启用轻量治理，也可以定义自己的 `RemoteCallPolicy` bean，替换默认实现：

- `beforeCall(context)`: 调用前检查，可用于熔断、限流、白名单、灰度策略。
- `afterSuccess(context)`: 传输成功后记录指标。
- `afterFailure(context, exception)`: 传输失败后记录指标或推进熔断状态。

配置型策略支持：

- `blocked-service-ids`: 调用前阻断指定 serviceId / targetService。
- `allowed-caller-ids`: 全局调用方白名单，空值表示不限制。
- `allowed-caller-ids-by-service`: 服务级调用方白名单，适合先表达“只有网关能调 auth”这类边界。

配置型策略拒绝调用时抛出 `SystemException(RemoteErrorCode.REMOTE_CALL_PERMISSION_DENIED)`。自定义熔断器或限流器拒绝调用时，可以抛出 `SystemException(RemoteErrorCode.REMOTE_CALL_CIRCUIT_BREAKER_OPEN)` 或下游项目自己的治理错误码。`RemoteCallPolicyContext` 只包含 method、targetService、serviceId、path、脱敏 URL 和 callerId，不包含请求体或请求头。

## 调用审计扩展点

`RemoteCallObserver` 是远程调用完成后的审计与指标扩展点。母版默认提供 `NoopRemoteCallObserver`，不会写库或引入 metrics 依赖；下游项目可以定义自己的 `RemoteCallObserver` bean，替换默认实现。

`RemoteCallObservation` 会在调用成功、失败或被策略拒绝后生成，包含 method、targetService、serviceId、path、脱敏 URL、callerId、requestId、traceId、tenantId、userId、timeZone、locale、success、durationMs、errorCode、errorMessage 和 exceptionType。它不包含请求体、响应体和 headers，可安全转发到审计表、指标系统或 tracing adapter。

## 日志与安全

`RemoteHttpClient` 交给 `RemoteCallWrapper` 的日志对象只包含 method、targetService、url 和 callerId，不输出请求体和 headers，避免 Authorization、Cookie、Token 或业务敏感参数进入日志。

## 重试策略

默认重试仍由 `RemoteCallWrapper` 判断：

- `1800-1899` 远程网络、连接、超时类错误可重试。
- `1600-1799` 远程契约或响应校验错误默认不重试。
- `2xxx` / `3xxx` / `4xxx` 业务、参数、权限错误默认不重试。

HTTP 状态映射：

- `408` / `504` -> `REMOTE_CALL_TIMEOUT`
- `429` / `502` / `503` -> `REMOTE_CALL_NETWORK_ERROR`
- 其他非 2xx 状态 -> `REMOTE_RESPONSE_STATUS_FAILED`
