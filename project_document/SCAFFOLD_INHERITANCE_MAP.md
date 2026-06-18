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
| 请求上下文 | requestId、traceId、tenantId、userId、locale、timeZone 透传和日志字段 | AIGC 任务、素材、资产和审计从上下文继承归属与追踪字段 |
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
- `frontend/src/views/aigc/gallery`：已发布作品、筛选、运营规则说明/配置、动态合集、人工专题、创作者榜单和 Prompt 复用到工作台。
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

AIGC 私有资源归属也从脚手架上下文生长：`AigcOwnershipService` 统一读取 `GlobalRequestContextHolder` 的 userId 和 tenantId，新上传素材、新生成任务、新生成资产写入 owner/tenant；任务、素材、资产和存储审计查询统一走 `findVisible...` 或规格查询过滤。历史空归属数据继续可见，用于兼容本地 demo 和早期迁移；需要治理时通过模型配置页的数据治理区块调用 `/api/aigc/ownership/backfill` 进入 `AigcOwnershipBackfillService`，先 dry-run 统计资产、素材、任务候选行，只有管理角色且显式确认后才按当前请求上下文回填 owner/user/tenant，并写入管理审计。这个接口同样从 `ApiConstants.Aigc`、`service-boundaries.json`、OpenAPI 类型和 `openApiRequest` 派生，教学时可以作为“历史数据迁移也继承脚手架权限与契约习惯”的例子。

AIGC 文件访问也必须先经过业务边界：资产和素材下载分别进入 `/api/aigc/assets/{assetId}/download` 与 `/api/aigc/materials/{materialId}/download`，预览分别进入 `/api/aigc/assets/{assetId}/preview` 与 `/api/aigc/materials/{materialId}/preview`，由 `AigcDownloadService` 先做 owner/tenant 可见性查询，再调用 `AigcStorageService.resolveDownload` 解析本地文件或 OSS 授权 URL。前端下载工具不再直接下载资产裸 URL，资产库、素材库、创作结果和历史记录预览也不再直接渲染裸 `/files/**` 路径，而是通过 `ApiPaths.aigc.*Preview` 进入授权预览接口；本地 `/files/**` 只作为开发和历史演示兼容映射，由 `AIGC_LOCAL_STATIC_SERVING_ENABLED` 单独控制，dev/demo 可开启，生产部署建议关闭，并通过存储状态接口展示。这样教学时可以清楚看到“文件能力从 storage adapter 生长，但权限习惯来自脚手架 request context 和 service-boundary”。

灵感广场属于公开发布边界，不复用个人资产授权入口，也不返回原始存储 URL。已发布作品通过 `/api/aigc/gallery/{assetId}/preview` 进入 `AigcDownloadService.previewPublishedAsset`，后端只查询 `isPublished=true` 的资产，再交给 storage adapter 返回本地文件或 OSS 授权 URL；Gallery DTO 返回 `previewUrl` 和 `publicAccessMode=published-preview`，前端卡片通过 `resolveAigcGalleryPreviewUrl` 解析公开预览或静态后备外链。这样可以在教学里区分“我的资产是上下文授权资源，灵感广场是发布后的公开资源”。

广场运营能力也沿同一条路径生长：作品榜单、动态合集、人工专题、创作者榜单、运营规则说明和运营规则配置分别进入 `/api/aigc/gallery/ranking`、`/api/aigc/gallery/collections`、`/api/aigc/gallery/topics`、`/api/aigc/gallery/creators/ranking`、`/api/aigc/gallery/curation/rules`、`/api/aigc/gallery/curation/rules/config`，都先声明在 `ApiConstants.Aigc` 和 `contracts/service-boundaries.json`，再由 OpenAPI 类型、`frontend/src/api/aigc.ts` 和广场页面消费。规则配置写操作继续复用脚手架请求上下文、`AigcProviderManagementPermissionService` 的管理角色边界和管理审计，这样教学时可以直接对比“同一个脚手架路径，长出不同 AIGC 业务视角”。

Provider 管理还必须遵守密钥和权限边界：配置页只能展示配置状态、缺失说明、默认参数、来源和存储模式，不能返回或渲染 `apiKey`、`accessKey`、`secretKey`、`accessKeySecret` 等明文字段；日志也只能记录密钥存在性和长度，不能打印前缀或后缀。Provider 凭证写入通过 `/api/aigc/models/provider-credential` 进入 AIGC service-boundary，前端只能调用 `frontend/src/api/aigc.ts`，后端由 `AigcProviderCredentialConfigService` 统一处理“页面保存优先、环境配置兜底”的来源规则，由 `AigcProviderCredentialCodec` 统一处理 AES-GCM 静态加密和旧明文兼容；后续 KMS 替换也只能发生在这个 codec 边界。Provider 路由、凭证和默认参数模板写操作必须先经过 `AigcProviderManagementPermissionService`，从 `GlobalRequestContextHolder` 读取 `userRoles`，默认要求 `R_SUPER` 或 `R_ADMIN`；前端只能在统一 HTTP client 中通过平台 `REQUEST_HEADERS` 透传 userId、userName 和 userRoles，页面不允许手写请求头。Provider 默认参数写入同样通过 service-boundary 和 OpenAPI 类型进入页面，不允许在前端页面手写 URL 或绕过 API 模块。Provider 管理审计继承脚手架 `GlobalRequestContextHolder`，审计 requestId、traceId、tenantId、userId、callerId 和客户端 IP，但凭证审计只记录来源变化、存储模式和配置状态，不记录明文；权限拒绝写入 `permission-denied` 审计。

## 教学视角

讲解这个项目时，可以按下面顺序展开：

1. 先讲脚手架底座：目录、统一响应、OpenAPI、服务边界、请求上下文和质量门禁。
2. 再讲业务增量：AIGC 为什么需要任务、Provider、Prompt、素材和资产。
3. 然后讲生长过程：先接入 service boundary，再做 Controller/Service/DTO，最后接页面。
4. 最后讲验证闭环：每次小步改动都通过同一套脚本、测试、构建和 push 证明。

这样学习者只需要关注业务设计本身，底层技术栈、命名习惯、质量守卫和最佳实践都从脚手架继承。
