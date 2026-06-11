# Service Boundary Guide

`contracts/service-boundaries.json` 定义母版当前和未来服务边界。它不是服务注册中心，也不要求母版拆成微服务；它是给 API Gateway、OpenAPI、前端 `ApiPaths`、AI Prompts 和后续拆分计划看的机器可读边界表。

## Why

脚手架可以先保持单体运行，但路径和职责要提前有边界：

- 网关能知道 `/api/auth/**`、`/api/test/**`、`/api/common/**` 的归属。
- 前端能知道哪些 `ApiPaths` 是当前运行接口，哪些只是旧模板兼容或未来预留。
- AI 生成新模块时能先选择边界，再补 `ApiConstants`、`ApiPaths`、DTO/VO 和 OpenAPI。
- 复制成新项目时能判断哪些示例接口要删除，哪些平台契约要保留。

## Generated Artifacts

生成服务边界常量：

- `backend/src/main/java/com/anjing/model/constants/ServiceBoundaryConstants.java`
- `frontend/src/contracts/service-boundaries.ts`

它们提供 `APPLICATION_ID`、`API_PREFIX`、边界 id、owner、currentHost、basePath 和 route path。后端远程调用、默认 caller id、示例代码等需要应用 id 或服务边界元数据时，优先引用 `ServiceBoundaryConstants`；前端 `ApiPaths` 优先引用 `SERVICE_BOUNDARY_ROUTE_PATHS`，不要重复手写 manifest 中的运行路径字符串。

## Boundary Kinds

| kind | 含义 | 当前示例 |
|------|------|----------|
| `runtime` | 当前母版真实运行接口 | `auth` |
| `sample` | 示例或自检接口，复制后可删除或替换 | `test` |
| `reserved-runtime` | 前后端已预留路径，具体实现可由复制项目决定 | `common` |
| `reserved` | 未来服务或模块预留边界 | `user`、`admin`、`integration` |

## Current Boundaries

| boundary | basePath | owner | 当前宿主 | 说明 |
|----------|----------|-------|----------|------|
| `auth` | `/api/auth` | `infra-auth` | `infra-dev-scaffolding` | Mock 认证接口，未来可迁到认证中心 |
| `test` | `/api/test` | `infra-dev-scaffolding` | `infra-dev-scaffolding` | 示例、自检、OpenAPI smoke |
| `common` | `/api/common` | `infra-common` | `infra-dev-scaffolding` | 上传、下载等平台通用路径预留 |
| `user` | `/api/users` | `infra-auth` | `future-service` | 用户管理预留 |
| `admin` | `/api/admin` | `infra-admin` | `future-service` | 运维管理预留 |
| `integration` | `/api/integration` | `infra-integration` | `future-service` | 外部系统集成预留 |

## Update Rules

- 新增运行模块时，先在 `contracts/service-boundaries.json` 增加 boundary。
- 运行 `node scripts/generate-service-boundaries-backend.js` 更新后端生成常量。
- 运行 `node scripts/generate-service-boundaries-frontend.js` 更新前端生成常量。
- 后端同步新增 `ApiConstants.Xxx.BASE` 和 `*_FULL` 路径。
- 前端同步新增 `ApiPaths.xxx`，只把真实运行路径加入 manifest route。
- 旧模板或 mock 路径留在 `ApiLegacyPaths`，不要加入 `ApiPaths` 或 manifest route。
- Controller 使用 `@RequestMapping(ApiConstants.Xxx.BASE)`。
- 当前运行接口需要出现在 `/v3/api-docs` 时，Controller 补 `@Tag`，DTO/VO 补必要 `@Schema`。
- 复制项目删除示例接口时，可以删除 `test` boundary 或改成自己的 health/sample 边界。

## Verification

```bash
node scripts/check-service-boundaries.js
```

```bash
node scripts/generate-service-boundaries-backend.js --check
```

```bash
node scripts/generate-service-boundaries-frontend.js --check
```

完整检查链路：

```bash
./scripts/check-contracts.sh
```

`check-service-boundaries.js` 会校验：

- boundary id、basePath 和 route 不重复。
- basePath 使用 `contracts/platform-contract.json` 的 API 前缀。
- manifest 中的 basePath 与 `ApiConstants` 一致。
- manifest 中的 route 与 `ApiConstants.*_FULL`、`ApiPaths` 一致。
- `ApiPaths` 不混入旧模板路径；旧路径集中在 `ApiLegacyPaths`。
- 当前 OpenAPI 运行 Controller 使用 `ApiConstants.Xxx.BASE` 并带有 `@Tag`。
- 后端 `ServiceBoundaryConstants` 和前端 `SERVICE_BOUNDARY_ROUTE_PATHS` 与 manifest 保持一致。
