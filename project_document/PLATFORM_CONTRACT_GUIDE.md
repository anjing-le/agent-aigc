# Platform Contract Guide

本文档说明 `contracts/platform-contract.json` 的定位和维护方式。它是母版里最稳定的跨端、跨服务平台契约 manifest，为后续 OpenAPI、类型生成、共享包拆分和 AI 生成代码提供机器可读的事实来源。

## Scope

当前 manifest 收录稳定基础契约：

- API 前缀：`/api`
- 响应 envelope：`code`、`message`、`data`、`timestamp`、`requestId`
- 成功码：`0`
- 分页 payload：`records`、`current`、`size`、`total`
- 请求上下文头：requestId、traceId、tenant、user、caller、timezone、language
- 前端可透传头：`frontendPropagatedHeaders`，仅包含浏览器可生成的 requestId、traceId、timezone、language
- 后端服务间可透传头：`backendPropagatedHeaders`，包含 request、trace、tenant、user、caller、timezone、language 上下文
- 时间策略：默认 UTC，前端透传 `X-Time-Zone`，后端通过 `TimeZoneUtils` 归一化
- 语言策略：默认 `zh-CN`，支持语言由 `locale.supportedLocales` 统一声明
- 错误码分段和默认可重试远程错误范围

它不描述具体业务接口。具体接口仍由 Controller、DTO/VO、`ApiConstants`、`ApiPaths` 和未来 OpenAPI 生成链路承载。

## Generated Artifacts

基础常量由 manifest 生成：

- `backend/src/main/java/com/anjing/model/constants/PlatformContractConstants.java`
- `frontend/src/contracts/platform-contract.ts`

后端当前通过生成文件提供：

- `PlatformContractConstants.API_PREFIX`
- `PlatformContractConstants.FRONTEND_PROPAGATED_HEADER_KEYS`
- `PlatformContractConstants.BACKEND_PROPAGATED_HEADER_KEYS`
- `PlatformContractConstants.Response.SUCCESS_CODE`
- `PlatformContractConstants.Headers.*`
- `PlatformContractConstants.Time.DEFAULT_TIME_ZONE`
- `PlatformContractConstants.Locale.DEFAULT_LOCALE`
- `PlatformContractConstants.Locale.SUPPORTED_LOCALES`
- `PlatformContractConstants.ErrorCodes.RANGES`
- `PlatformContractConstants.ErrorCodes.RETRYABLE_RANGES`

前端当前通过生成文件导出：

- `PLATFORM_CONTRACT`
- `API_SUCCESS_CODE`
- `REQUEST_HEADERS`
- `FRONTEND_PROPAGATED_HEADER_KEYS`
- `BACKEND_PROPAGATED_HEADER_KEYS`
- `DEFAULT_TIME_ZONE`
- `DEFAULT_LOCALE`
- `SUPPORTED_LOCALES`
- `PlatformSupportedLocale`

`ApiConstants`、`RequestHeaderConstants`、`APIResponse`、`RemoteCallWrapper`、`TimeZoneUtils`、`LocaleUtils`、`frontend/src/utils/http/context.ts`、`frontend/src/utils/http/response.ts`、`frontend/src/utils/time/index.ts`、`frontend/src/utils/locale/index.ts` 应引用生成文件，不要重复手写 API 前缀、请求头、前端/后端透传头列表、成功码、默认时区或支持语言。

## Update Rules

- 修改响应字段、分页字段、请求头、透传头列表、语言策略或错误码分段时，先更新 `contracts/platform-contract.json`。
- 运行 `node scripts/generate-platform-contract-backend.js` 更新后端生成常量。
- 运行 `node scripts/generate-platform-contract-frontend.js` 更新前端生成常量。
- 同步更新 Java/TypeScript 代码、文档和 Cursor Rules / Prompts。
- 新增字段必须能被 `scripts/check-platform-contract.js` 校验到。
- 新增错误码枚举或调整错误码范围时，必须通过 `scripts/check-error-codes.js`。
- 业务模块自己的字段不要写进 platform contract；它们属于业务 OpenAPI 或模块文档。

## Verification

发布母版或复制项目前运行：

```bash
node scripts/generate-platform-contract-backend.js --check
```

```bash
node scripts/check-platform-contract.js
```

```bash
node scripts/check-frontend-context-contract.js
```

```bash
node scripts/check-error-codes.js
```

```bash
node scripts/generate-platform-contract-frontend.js --check
```

```bash
(cd backend && mvn -q -Dtest=LocaleUtilsTest test)
```

```bash
(cd backend && mvn -q -Dtest=TimeZoneUtilsTest test)
```

完整检查链路会通过 `./scripts/check-contracts.sh` 间接运行该脚本。

## Future Direction

后续可以把该 manifest 作为生成入口之一：

- 生成前端基础响应和分页类型。
- 生成后端共享包中的契约常量。
- 作为 OpenAPI 生成前的全局约束输入。
- 在 AI Prompts 中作为标准契约引用，减少重复手写规则。
