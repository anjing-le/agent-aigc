# OpenAPI Contract Guide

OpenAPI 是后端运行接口给前端、AI Prompts、网关和未来服务调用方看的机器可读契约。母版提供轻量 JSON 文档入口，不默认引入 Swagger UI；并已基于该入口生成前端 schema 类型、operation 类型目录、path/query 参数类型和轻量 typed 调用入口，后续可以继续扩展为完整 API client 生成。

服务/模块归属由 `contracts/service-boundaries.json` 记录。OpenAPI 负责描述运行接口细节，service boundary 负责描述这些接口当前由谁承载、未来可能迁到哪个服务。

## Runtime Endpoint

开发和测试环境默认启用：

```text
/v3/api-docs
```

生产环境默认关闭，可通过环境变量显式开启：

```bash
OPENAPI_API_DOCS_ENABLED=true
```

当前依赖使用：

```xml
<artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
```

`backend/src/main/java/com/anjing/config/openapi/OpenApiConfig.java` 负责：

- 只匹配 `PlatformContractConstants.API_PREFIX + "/**"`。
- 只扫描 `com.anjing.controller`。
- 给每个 operation 补充 requestId、traceId、tenantId、callerId、timeZone、language 等平台请求头。

## DTO / VO Rules

- 运行接口优先返回明确 DTO / VO，不要用 `Map<String, Object>` 承载真实业务 payload。
- 请求体使用明确 Request 类，并配合 Jakarta Validation。
- 返回体统一包在 `APIResponse<T>` 中。
- 分页响应使用 `PageResult<T>` 或字段完全一致的 VO。
- 示例接口可以保留 Map 演示，但真实业务 Controller 不应复制这种写法。
- 需要让 OpenAPI 更清晰时，可在 Request / Response 模型上补充 `@Schema`。

## Frontend Types

运行 OpenAPI JSON 可生成到：

```bash
node scripts/generate-openapi-frontend-types.js /path/to/openapi.json
```

生成产物：

```text
frontend/src/contracts/openapi/schemas.ts
frontend/src/contracts/openapi/operations.ts
```

`schemas.ts` 保存 DTO / VO schema 类型；`operations.ts` 保存运行接口的 operationId、method、path、pathParams、query、request 类型、response envelope 类型和 data 类型。`src/api/model/**` 优先从 operation 类型派生请求/响应类型。页面不要直接依赖生成目录，避免生成格式变化影响业务页面；API model 层负责做必要的前端兼容字段和命名适配。

当前 `frontend/src/api/model/authModel.ts` 已从 `OpenApiOperationRequest<'login'>`、`OpenApiOperationData<'login'>`、`OpenApiOperationData<'getCurrentUser'>` 和 `OpenApiOperationRequest<'refreshToken'>` 派生登录相关类型。

## Frontend Runtime Helper

`frontend/src/api/openapiClient.ts` 提供一个轻量 typed 调用入口：

```typescript
openApiRequest('login', {
  body: params
})

openApiRequest('getItem', {
  pathParams: { id: 1 }
})
```

它从 `OPENAPI_OPERATIONS` 读取 method/path，通过生成的 `OpenApiOperationPathParams<T>` 和 `OpenApiOperationQuery<T>` 约束 `pathParams/query`，再用 `bindOpenApiPathParams` 绑定 `{id}` 这类路径参数，并复用现有 `utils/http` 的 token、上下文请求头、错误处理和消息提示能力。需要非 Axios 完整地址时使用 `resolveOpenApiPath(operationId, pathParams)`。

当前 `frontend/src/api/auth.ts` 已改为通过 `openApiRequest('login')`、`openApiRequest('getCurrentUser')` 和 `openApiRequest('refreshToken')` 调用运行接口。批量生成每个模块的完整 API client 仍作为后续 S7 任务推进。

## Verification

静态契约检查：

```bash
node scripts/check-openapi-contract.js
```

生成类型静态检查：

```bash
node scripts/generate-openapi-frontend-types.js /path/to/openapi.json --check
```

开发环境运行检查会通过 `./scripts/probe-backend-dev.sh` 拉取 `/v3/api-docs`，确认 OpenAPI JSON、运行路径、平台请求头和前端生成 schema 类型可用。

动态契约检查：

```bash
node scripts/check-openapi-runtime-contract.js /path/to/openapi.json
```

该脚本读取 `contracts/service-boundaries.json` 和 `contracts/platform-contract.json`，校验所有 `openapi=true` 的 route/method 都出现在 OpenAPI 中，并校验每个 operation 都包含平台请求头。`./scripts/probe-backend-dev.sh` 会自动调用它，并同步调用 `generate-openapi-frontend-types.js --check` 防止前端生成 schema 过期。

完整母版检查：

```bash
./scripts/check-contracts.sh
```
