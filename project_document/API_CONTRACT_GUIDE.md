# API Contract Guide

本文档定义前后端共同遵守的 API 契约。运行接口的 OpenAPI JSON 入口和类型生成方向见 `project_document/OPENAPI_CONTRACT_GUIDE.md`；本文关注所有新增接口必须遵守的稳定字段和生成规则。

机器可读版本位于 `contracts/platform-contract.json`，维护方式见 `project_document/PLATFORM_CONTRACT_GUIDE.md`。

## Response Envelope

所有后端业务接口统一返回 `APIResponse<T>`：

```json
{
  "code": "0",
  "message": "操作成功",
  "data": {},
  "timestamp": 1700000000000,
  "requestId": "0b4b3b0c-7b7b-4ed1-8c53-2ac0e73d2f6a"
}
```

字段规则：

- `code`: 字符串；成功固定为 `"0"`，错误码参考 `ERROR_CODE_GUIDE.md`。
- `message`: 标准用户可读消息。
- `data`: 业务数据；无数据时可以为 `null` 或省略。
- `timestamp`: 服务端响应时间戳。
- `requestId`: 请求链路 ID，用于前端报错和后端日志关联。

## Backend Rules

- Controller 返回 `APIResponse<T>`。
- 真实业务接口使用明确 Request / Response DTO，不要用 `Map<String, Object>` 作为业务 payload；这样 `/v3/api-docs` 才能生成稳定 schema。
- 有数据：`APIResponse.success(data)`。
- String 数据：`APIResponse.successData("pong")`，避免和成功消息重载混淆。
- 无数据自定义消息：`APIResponse.successMessage("删除成功")`。
- 错误响应：优先抛出 `BizException(ErrorCode)` / `SystemException(ErrorCode)`，或使用 `APIResponse.error(ErrorCode)`。

分页 payload 使用 `PageResult<T>`：

```json
{
  "records": [],
  "current": 1,
  "size": 20,
  "total": 100
}
```

`PageResult` 是共享契约对象，不依赖 Spring Data、JPA 或 Web 框架。使用 Spring Data `Page<T>` 时，在业务层展开为：

```java
PageResult.of(page.getContent(), page.getTotalElements(), page.getNumber() + 1, page.getSize())
```

旧的 `BaseResponse`、`MultiResponse`、`PageResponse` 只用于兼容外部或历史远程响应，新业务不要使用。

## Frontend Rules

- API 函数使用 `request.get<T>()` / `request.post<T>()`，返回 `data` 内部类型。
- 页面不解析 response envelope。
- 标准消息字段是 `message`。
- `msg` 只允许在 `utils/http/response.ts` 内部兼容旧 mock 或第三方接口。
- 列表类型使用 `PaginatedResponse<T>`，字段为 `records`、`current`、`size`、`total`。
- 用户反馈或排查日志应保留 `requestId` 和 `traceId`。

## Request Context Contract

- 前端请求上下文由 `frontend/src/utils/http/context.ts` 统一生成。
- `requestId` 每次请求重新生成，用于精确定位一次 HTTP 调用。
- `traceId` 在当前浏览器会话内复用，用于把页面内连续请求和后端服务间调用串起来。
- `X-Time-Zone` 来自统一时间工具，`Accept-Language` 来自用户语言或浏览器语言。
- 平台时区从 `contracts/platform-contract.json` 的 `time.defaultTimeZone` 派生；后端通过 `TimeZoneUtils.normalizeTimeZone` 归一化，缺失或非法时回落到默认 UTC。
- 平台语言从 `contracts/platform-contract.json` 的 `locale.defaultLocale` 和 `locale.supportedLocales` 派生；前端通过 `frontend/src/utils/locale` 归一化后只发送支持语言，后端通过 `LocaleUtils.normalizeAcceptLanguage` 归一化，无法匹配时回落到默认语言。
- 页面、组件和 API 模块不要手写平台上下文请求头；Axios 拦截器和非 Axios 适配层需要复用 `buildRequestContextHeaders`。
- 前端 `HttpError` 必须从响应体、响应头或请求头提取 `requestId` / `traceId`，业务错误码、HTTP 状态错误、网络错误和 401 分支都不能丢失链路上下文。

## Time Contract

- 服务端接口时间默认使用 UTC，不依赖部署机器本地时区。
- 业务代码需要当前时间时使用 `DateUtils`：接口时间字符串使用 `DateUtils.nowIso()`，自定义 UTC 格式使用 `DateUtils.now(pattern)`，毫秒时间戳使用 `DateUtils.nowEpochMilli()`。
- 请求上下文中的 `timeZone` 必须通过 `TimeZoneUtils` 归一化，远程调用透传的是合法 IANA/offset zone id 或默认 UTC。
- Controller、Service、工具扩展代码不要直接调用 `Instant.now()`、`LocalDateTime.now()`、`OffsetDateTime.now()` 或 `ZonedDateTime.now()`；统一入口方便未来接入租户时区、审计时间源或测试固定时钟。
- 前端展示使用 `frontend/src/utils/time`，请求头透传 `X-Time-Zone` 和 `Accept-Language`。
- 页面、组件和 API 模块不要直接使用浏览器本地化时间格式化 API、浏览器语言读取 API 或 VueUse 日期格式化 helper；展示时间、文件名时间戳和日期 key 统一走 `formatDateTime`、`formatDate`、`formatTime`、`formatDateKey`、`formatFilenameTimestamp`，展示语言统一走 `frontend/src/utils/locale`。

## Migration Notes

当前母版仍保留少量旧接口兼容：

- 前端 `isSuccessCode` 暂时兼容数字 `200`。
- 前端 `extractResponseMessage` 暂时兼容 `msg`。
- 后端 `RemoteCallWrapper` 暂时兼容旧 `BaseResponse`。

这些兼容点必须集中在基础设施层，业务页面、API 模块和新后端接口不要继续扩散旧字段。

## Verification

发布母版或复制项目前运行：

```bash
./scripts/check-contracts.sh
```

```bash
node scripts/check-platform-contract.js
```

```bash
node scripts/check-openapi-contract.js
```

```bash
node scripts/check-frontend-time-contract.js
```

```bash
node scripts/check-frontend-context-contract.js
```

```bash
(cd backend && mvn -q -Dtest=LocaleUtilsTest test)
```

```bash
(cd backend && mvn -q -Dtest=TimeZoneUtilsTest test)
```

该脚本会检查 API path、response envelope、分页字段、请求上下文、远程调用、时间工具和 OpenAPI 的关键契约是否仍然集中管理。
