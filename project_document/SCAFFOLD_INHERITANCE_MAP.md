# Scaffold Inheritance Map

本文档用于教学：说明 `agent-aigc` 如何从 `infra-dev-scaffolding` 生长出来。学习者看这个项目时，应先识别哪些是脚手架底座，哪些是 AIGC 业务增量。

## 一句话结论

`agent-aigc` 不重新发明工程体系。它继承脚手架的 Spring Boot、Vue 3、TypeScript、OpenAPI、统一响应、服务边界、请求上下文、质量门禁和后台页面习惯；AIGC 只在业务模块里扩展创作链路、Provider、资产、素材和页面体验。

## 继承关系

| 层级 | 来自脚手架 | agent-aigc 的业务增量 |
| --- | --- | --- |
| 后端技术栈 | Spring Boot 3、JPA、H2/MySQL、OpenAPI、统一异常和响应 | `com.anjing.aigc` 下的创作任务、素材、资产、Agent 和 Provider |
| 前端技术栈 | Vue 3、TypeScript、Vite、Element Plus、路由、HTTP helper、OpenAPI 类型 | `views/aigc` 下的工作台、素材库、资产库、灵感广场、模型配置 |
| API 契约 | `ApiConstants`、`ApiPaths`、`APIResponse`、`PageResult`、错误码分段 | `/api/aigc/**` 的任务、模型、素材、资产和广场接口 |
| 服务边界 | `contracts/service-boundaries.json` 作为机器可读边界 | 新增 `aigc` service boundary，并由前后端常量生成和脚本校验 |
| 请求上下文 | requestId、traceId、locale、timeZone 透传和日志字段 | 异步 AIGC 任务继续保留上下文与错误码，方便追踪生成链路 |
| 质量门禁 | `scripts/check-contracts.sh`、`scripts/quality-gate.sh`、OpenAPI runtime probe | 每次业务迭代后继续运行同一套门禁，证明没有破坏底座 |

## 后端边界

脚手架层保留在 `com.anjing` 的通用包中：

- `config`：环境、OpenAPI、HTTP、JPA、Redis、锁、异步线程池等通用配置。
- `context`：请求上下文和 MDC 传播。
- `model`：统一响应、分页、错误码、常量和平台契约。
- `client`：远程调用 wrapper、服务地址解析和调用观测扩展点。
- `util`：时间、语言、JSON、请求头等共享工具。

AIGC 业务只放在 `com.anjing.aigc`：

- `agent`：意图识别、Prompt 清洗/增强、模型路由决策。
- `provider`：Mock、Google、后续 OneRouter 等模型供应商适配。
- `service`：创作任务、素材、资产、重试、发布、本地存储、Provider 成本估算和运行前诊断检查项。
- `controller`：只暴露 `/api/aigc/**`，继续使用脚手架统一响应。
- `model` / `repository`：AIGC 任务、资产、素材的实体、DTO、VO 和查询。

## 前端边界

脚手架层继续提供应用壳和通用能力：

- `router`、`store`、`utils/http`、`utils/time`、`utils/locale`。
- `contracts/openapi` 生成类型和 `api/openapiClient.ts` typed 调用入口。
- `components/core`、全局样式 token、后台布局和基础交互习惯。

AIGC 页面只聚焦创作体验：

- `frontend/src/views/aigc/studio`：一句话创作、任务状态、Agent 决策、结果预览。
- `frontend/src/views/aigc/materials`：参考素材上传、筛选、引用任务反查。
- `frontend/src/views/aigc/assets`：个人资产、下载、删除、发布、Prompt 复用和存储状态诊断。
- `frontend/src/views/aigc/gallery`：已发布作品、筛选、Prompt 复用到工作台。
- `frontend/src/views/aigc/models`：Provider 配置、模型能力、运行前探测、诊断检查项和显式 smoke test。

## API 生长方式

新增 AIGC 接口时遵守同一条路径：

1. 在后端 `ApiConstants.Aigc` 声明路径。
2. 在 `contracts/service-boundaries.json` 确认 `aigc` 边界。
3. Controller 返回 `APIResponse<T>` 或 `APIResponse<PageResult<T>>`。
4. 运行 OpenAPI 类型生成或检查。
5. 前端通过 `ApiPaths` / `openApiRequest` / `api/model` 调用，不在页面里手写 URL。
6. 运行 `./scripts/check-contracts.sh`，必要时再跑 `./scripts/quality-gate.sh`。

其中 `./scripts/check-aigc-scaffold-boundaries.js` 是 AIGC 专属守卫：它检查后端 AIGC Controller 是否继续使用 `ApiConstants.Aigc`、`APIResponse` 和 `PageResult`，前端 AIGC API 是否继续通过 `openApiRequest` 与 OpenAPI 派生类型调用，页面是否没有直接拼接 `/api/aigc` 或绕过 API 模块。比如 Provider 持久化切换接口 `/api/aigc/models/active-provider`、默认参数模板接口 `/api/aigc/models/provider-params`、管理审计接口 `/api/aigc/models/provider-audits`、存储状态接口 `/api/aigc/storage/status` 和存储审计接口 `/api/aigc/storage/audits` 都必须先进入 service-boundary，再生成前后端常量和 OpenAPI 类型，并分别通过 `AigcProviderRouteConfigService`、`AigcProviderParamConfigService`、`AigcProviderAuditLogService`、`AigcStorageService`、`AigcStorageAuditLogService` 统一处理配置来源、运行时覆盖、审计查询、存储诊断和 storage adapter 切换；素材、资产和 Provider 代码不允许绕过 `AigcStorageService` 直接依赖本地存储实现。对象存储实现通过 `OssAigcStorageService` 的 S3-compatible adapter 接入，默认关闭，不破坏脚手架轻启动；上传/删除重试和清理审计也收口在 storage adapter 边界，由 `AigcStorageAuditLogService` 继承脚手架请求上下文。

Provider 管理还必须遵守密钥和权限边界：配置页只能展示配置状态、缺失说明、默认参数、来源和存储模式，不能返回或渲染 `apiKey`、`accessKey`、`secretKey`、`accessKeySecret` 等明文字段；日志也只能记录密钥存在性和长度，不能打印前缀或后缀。Provider 凭证写入通过 `/api/aigc/models/provider-credential` 进入 AIGC service-boundary，前端只能调用 `frontend/src/api/aigc.ts`，后端由 `AigcProviderCredentialConfigService` 统一处理“页面保存优先、环境配置兜底”的来源规则，由 `AigcProviderCredentialCodec` 统一处理 AES-GCM 静态加密和旧明文兼容；后续 KMS 替换也只能发生在这个 codec 边界。Provider 路由、凭证和默认参数模板写操作必须先经过 `AigcProviderManagementPermissionService`，从 `GlobalRequestContextHolder` 读取 `userRoles`，默认要求 `R_SUPER` 或 `R_ADMIN`；前端只能在统一 HTTP client 中通过平台 `REQUEST_HEADERS` 透传 userId、userName 和 userRoles，页面不允许手写请求头。Provider 默认参数写入同样通过 service-boundary 和 OpenAPI 类型进入页面，不允许在前端页面手写 URL 或绕过 API 模块。Provider 管理审计继承脚手架 `GlobalRequestContextHolder`，审计 requestId、traceId、tenantId、userId、callerId 和客户端 IP，但凭证审计只记录来源变化、存储模式和配置状态，不记录明文；权限拒绝写入 `permission-denied` 审计。

## 教学视角

讲解这个项目时，可以按下面顺序展开：

1. 先讲脚手架底座：目录、统一响应、OpenAPI、服务边界、请求上下文和质量门禁。
2. 再讲业务增量：AIGC 为什么需要任务、Provider、Prompt、素材和资产。
3. 然后讲生长过程：先接入 service boundary，再做 Controller/Service/DTO，最后接页面。
4. 最后讲验证闭环：每次小步改动都通过同一套脚本、测试、构建和 push 证明。

这样学习者只需要关注业务设计本身，底层技术栈、命名习惯、质量守卫和最佳实践都从脚手架继承。
