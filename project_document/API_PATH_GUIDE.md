# API Path Guide

本文档定义前后端 API 路径的集中管理方式。路径是未来 OpenAPI、网关转发、微服务拆分和 AI 生成代码的共同契约。服务/模块归属记录在 `contracts/service-boundaries.json`，维护方式见 `project_document/SERVICE_BOUNDARY_GUIDE.md`。

## Backend

后端路径统一写入 `ApiConstants`：

```java
@RequestMapping(ApiConstants.Auth.BASE)
public class AuthController {
    @PostMapping(ApiConstants.Auth.LOGIN)
    public APIResponse<LoginVO> login(...) {
        ...
    }
}
```

约定：

- 每个模块提供 `BASE`。
- 子路径使用相对路径，例如 `/login`、`/{id}`。
- 需要给远程调用或文档使用完整路径时，提供 `*_FULL`。
- `ApiConstants` 内部只允许 `API_PREFIX = "/api"` 一个 API 前缀字面量；模块路径使用 `BASE = API_PREFIX + "/module"`。
- Controller 注解不要直接写 `"/api/..."`。
- 新增接口时同步补齐 `ApiConstants`。

## Frontend

前端路径统一写入 `src/api/paths.ts`：

```typescript
import { SERVICE_BOUNDARY_ROUTE_PATHS } from '@/contracts/service-boundaries'

export const ApiPaths = {
  auth: {
    login: SERVICE_BOUNDARY_ROUTE_PATHS.auth.login,
    me: SERVICE_BOUNDARY_ROUTE_PATHS.auth.me
  }
} as const
```

约定：

- API 模块优先使用 `openApiRequest(operationId)` 调用 OpenAPI 运行接口；它会从生成的 operation 类型约束 `pathParams/query/body`。尚未接入 OpenAPI 的运行接口引用 `ApiPaths`，不要直接写 `url: '/api/...'`。
- `ApiPaths` 只放 `contracts/service-boundaries.json` 中声明的运行或预留运行路径，并优先引用生成的 `SERVICE_BOUNDARY_ROUTE_PATHS`。
- 旧模板、mock 或尚未由后端运行面承载的路径放入 `ApiLegacyPaths`，并在后续真实实现时迁回 `ApiPaths`。
- 路径参数使用函数，并通过 `encodeURIComponent` 处理。
- 页面组件不直接拼接口路径。
- 非 Axios 场景需要完整上传地址时，使用 `resolveApiPath(ApiPaths.xxx.yyy)`；OpenAPI 运行接口可使用 `resolveOpenApiPath(operationId, pathParams)`。不要手动拼 `VITE_API_URL + '/api/...'`。
- 新增后端接口时同步确认 `ApiConstants` 和 `ApiPaths` 命名一致。

## Current Runtime Paths

| 模块 | 后端常量 | 前端路径 |
|------|----------|----------|
| 登录 | `ApiConstants.Auth.LOGIN_FULL` | `ApiPaths.auth.login` |
| 当前用户 | `ApiConstants.Auth.ME_FULL` | `ApiPaths.auth.me` |
| 登出 | `ApiConstants.Auth.LOGOUT_FULL` | `ApiPaths.auth.logout` |
| 刷新 Token | `ApiConstants.Auth.REFRESH_FULL` | `ApiPaths.auth.refresh` |
| 健康检查 | `ApiConstants.Test.HEALTH_FULL` | `ApiPaths.test.health` |
| 可选能力状态 | `ApiConstants.Test.FEATURES_FULL` | `ApiPaths.test.features` |
| Ping | `ApiConstants.Test.PING_FULL` | `ApiPaths.test.ping` |
| 示例 items | `ApiConstants.Test.ITEMS_FULL` | `ApiPaths.test.items` |
| 通用上传 | `ApiConstants.Common.UPLOAD_FILE_FULL` | `ApiPaths.common.upload` |
| 富文本上传 | `ApiConstants.Common.UPLOAD_WANG_EDITOR_FULL` | `ApiPaths.common.uploadWangEditor` |

## Future Direction

OpenAPI / 类型生成应以 `ApiConstants`、Controller 注解和 `contracts/service-boundaries.json` 作为后端事实来源，生成或校验前端 API client。当前母版先用服务边界 manifest 生成 `SERVICE_BOUNDARY_ROUTE_PATHS`，由 `ApiPaths` 承载未接入 OpenAPI helper 的路径语义和动态参数编码；已接入 OpenAPI 的模块通过 `frontend/src/api/openapiClient.ts` 按 `operationId` 调用。

## Verification

发布母版或复制项目前运行：

```bash
./scripts/check-contracts.sh
```

该脚本会阻止运行 Controller 直接写 `"/api/..."`，也会阻止前端 API 模块绕过 `ApiPaths` 或 `openApiRequest` 直接写接口 URL；非 Axios 上传等场景不能手动拼 `VITE_API_URL + '/api/...'`。

如果只想验证前后端运行路径是否一致，可以运行：

```bash
node scripts/check-api-constants.js
```

```bash
node scripts/check-api-path-parity.js
```

```bash
node scripts/check-frontend-api-boundaries.js
```

```bash
node scripts/check-service-boundaries.js
```

```bash
node scripts/generate-service-boundaries-frontend.js --check
```

`check-api-constants.js` 会阻止 `ApiConstants` 内部重新散落 `"/api/..."` 字面量。`check-api-path-parity.js` 会读取 `contracts/service-boundaries.json`，比对 manifest 中声明的稳定运行接口和 `ApiConstants` / `ApiPaths`。`check-frontend-api-boundaries.js` 会阻止旧模板路径混入 `ApiPaths`。`generate-service-boundaries-frontend.js --check` 会确保前端生成路由常量与 manifest 一致。后续新增模块进入运行面时，应同步扩展 `service-boundaries.json` 的 route 表。
