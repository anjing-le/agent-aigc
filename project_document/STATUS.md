# Status

更新时间：2026-06-07

本文记录 `infra-dev-scaffolding` 当前作为 Anjing 开源工程母版的阶段状态和可验证证据。

## 阶段状态

| 阶段 | 状态 | 证据 |
|------|------|------|
| S0 构建与入口收口 | Ready | 前端 `pnpm build`、后端 `mvn -q -DskipTests package` 已通过，`./scripts/quality-gate.sh` 已纳入仓库 |
| S1 工程母版收口 | Ready | `project_document/PROJECT_CONSTRAINTS.md`、`project_document/NEW_MODULE_GUIDE.md`、`project_document/SCAFFOLD_ADOPTION_PROMPT.md`、`project_document/UI_DESIGN_GUIDE.md`、`project_document/DEMO_EVIDENCE.md`、`project_document/ci/quality-gate.yml`、`./scripts/check-template.sh`、`./scripts/smoke-copy.sh` 和 `./scripts/quality-gate.sh` 已纳入门禁 |
| S2 AI 协作资产收口 | Ready | Cursor Rules / Prompts、`project_document/SCAFFOLD_ADOPTION_PROMPT.md` 和 `project_document/NEW_MODULE_GUIDE.md` 已作为通用 AI 协作入口 |
| S3 后续项目复用验证 | In progress | `infra-skill-hub` 已从本母版接入骨架，完成 H0 / H1 / H2 验证，并推进 H3 HTTP/MCP/INTERNAL 调度层、全局与注册级协议配置、凭据托管与选择体验、权限身份头联动、策略批量管理、默认策略模板、调用治理、审计查询、治理指标与前端治理面 |
| S4 契约与全球化基线 | In progress | 已落地前端 `ApiPaths`、请求上下文头、统一时间工具、`utils/locale`、响应解析 helper、platform locale contract；后端已落地 `ApiConstants` 运行路径引用、`RequestContextFilter`、`LocaleUtils` 语言归一化、`TimeZoneUtils` 时区归一化、UTC 默认时间策略、响应 `requestId`、`PageResult`、API 契约指南、`contracts/service-boundaries.json` 和 `/v3/api-docs` OpenAPI JSON |
| S5 分布式可观测基线 | In progress | 已落地 MDC 日志字段、Controller 路径/耗时/错误码访问日志、异步上下文传播、`RemoteCallWrapper.serviceCallHeaders(callerId)`、支持 `ServiceEndpointResolver`、`ServiceEndpointRegistry`、`RemoteCallerResolver`、`ConfiguredRemoteCallPolicy`、`RemoteCallObserver` 和 `ParameterizedTypeReference` 的 `RemoteHttpClient`、错误码分段指南、`scripts/check-backend-context-contract.js`、`scripts/check-async-context-contract.js`、`scripts/check-error-codes.js`、`/api/test/features` 可选能力状态接口和 dev/test/prod profile 矩阵 |
| S7 契约生成与共享包 | In progress | 已落地 `project_document/SHARED_KERNEL_GUIDE.md`、`project_document/OPENAPI_CONTRACT_GUIDE.md`、`project_document/SERVICE_BOUNDARY_GUIDE.md`、`scripts/generate-openapi-frontend-types.js`、`scripts/check-openapi-contract.js`、`scripts/check-service-boundaries.js`、`scripts/check-shared-kernel.js` 和 `scripts/check-async-context-contract.js`，先守住未来可抽 `anjing-common`、服务边界、异步上下文传播、前端 schema/operation/path/query 类型生成和 typed OpenAPI 调用入口的契约边界 |

## 当前证据链

母版发布前至少运行：

```bash
./scripts/quality-gate.sh
./scripts/check-template.sh
./scripts/check-contracts.sh
node scripts/check-api-constants.js
node scripts/check-api-path-parity.js
node scripts/check-backend-controller-contracts.js
node scripts/check-scaffold-governance.js
node scripts/check-frontend-api-boundaries.js
node scripts/check-frontend-openapi-boundaries.js
node scripts/check-frontend-context-contract.js
node scripts/check-backend-context-contract.js
node scripts/check-async-context-contract.js
node scripts/check-frontend-time-contract.js
node scripts/generate-platform-contract-backend.js --check
node scripts/generate-platform-contract-frontend.js --check
node scripts/generate-service-boundaries-backend.js --check
node scripts/generate-service-boundaries-frontend.js --check
node scripts/check-platform-contract.js
node scripts/check-error-codes.js
node scripts/check-openapi-contract.js
node scripts/generate-openapi-frontend-types.js /path/to/openapi.json --check
node scripts/check-service-boundaries.js
node scripts/check-shared-kernel.js
node scripts/check-remote-http-contract.js
./scripts/smoke-copy.sh
./scripts/probe-backend-dev.sh
(cd backend && mvn -q -Dtest=RequestContextTaskDecoratorTest test)
(cd backend && mvn -q -Dtest=LocaleUtilsTest test)
(cd backend && mvn -q -Dtest=TimeZoneUtilsTest test)
(cd backend && mvn -q -Dtest=JsonUtilsTest test)
(cd backend && mvn -q -Dtest=RemoteCallWrapperContextHeadersTest test)
(cd backend && mvn -q -Dtest=RemoteHttpClientTest test)
(cd backend && mvn -q -DskipTests package)
(cd frontend && pnpm build)
(cd frontend && pnpm -s clean:dev)
```

AI 协作验证：

- `project_document/SCAFFOLD_ADOPTION_PROMPT.md` 提供可复制给 Codex / Cursor 的接入提示词，用于让旧项目或新项目按本母版文档、契约和质量门禁逐步重构。
- `project_document/NEW_MODULE_GUIDE.md` 提供新增模块的前后端交付顺序、OpenAPI 类型派生、DTO/VO、`APIResponse<T>`、`PageResult<T>` 和 AI Prompt 约束。
- 验证后回补的关键契约：
  - `project_document/PROJECT_CONSTRAINTS.md` 已记录母版长期约束、防破窗规则和脚本门禁清单；`scripts/check-template.sh` 已要求该文档存在并包含核心约束。
  - `project_document/NEW_MODULE_GUIDE.md` 已记录新增模块的前后端交付顺序、OpenAPI 类型派生、DTO/VO、`APIResponse<T>`、`PageResult<T>` 和 AI Prompt 约束。
  - `project_document/SCAFFOLD_ADOPTION_PROMPT.md` 已提供可复制给 Codex / Cursor 的接入提示词，用于让旧项目或新项目按本母版文档、契约和质量门禁逐步重构。
  - `project_document/UI_DESIGN_GUIDE.md` 已记录极简、虚线、轻玻璃、少文字和 hover 承载次级信息的 UI 基线；前端已提供 `glass-panel`、`glass-control`、`line-dashed` 等基础 utility。
  - `project_document/DEMO_EVIDENCE.md` 已记录发布和复制前的截图、后端 probe、质量门禁证据留存方式。
  - `project_document/ci/quality-gate.yml` 已提供 GitHub Actions 质量门禁模板，启用时复制到 `.github/workflows/quality-gate.yml` 即可在 push 和 PR 中执行 `./scripts/quality-gate.sh`。
  - `node scripts/check-backend-controller-contracts.js` 已校验非示例 Controller 必须返回 `APIResponse<T>`，且不能用 `Map` 承载真实请求或响应。
  - `node scripts/check-scaffold-governance.js` 已校验 CI 工作流、根入口文档、新模块指南、接入提示词、UI 设计基线、演示证据、全局玻璃 token、登录页玻璃/虚线入口和前端 legacy API 使用边界。
  - `node scripts/check-frontend-openapi-boundaries.js` 已校验页面和组件不能直接依赖 `@/contracts/openapi/**`，生成物只允许 API model 和 API runtime helper 消费。
  - 后端列表响应字段为 `records`、`current`、`size`、`total`。
  - 后端运行 Controller 路径引用 `ApiConstants`，前端路径收口到 `ApiPaths`，路径契约记录在 `project_document/API_PATH_GUIDE.md`。
  - `node scripts/check-api-constants.js` 已校验 `ApiConstants` 内部只保留 `API_PREFIX = "/api"` 一个 API 前缀字面量，模块路径使用 `BASE + 相对路径 + *_FULL`。
  - `contracts/service-boundaries.json` 已记录 auth、test、common 当前运行/预留边界，以及 user、admin、integration 未来服务边界；后端 `ServiceBoundaryConstants.java` 与前端 `frontend/src/contracts/service-boundaries.ts` 已由 manifest 生成，`node scripts/generate-service-boundaries-backend.js --check`、`node scripts/generate-service-boundaries-frontend.js --check` 与 `node scripts/check-service-boundaries.js` 已校验边界 basePath、route、`ApiConstants`、`ApiPaths`、Controller 和 OpenAPI `@Tag` 一致。
  - `node scripts/check-api-path-parity.js` 已改为读取 `contracts/service-boundaries.json`，校验 manifest 中声明的稳定运行路径和 `ApiConstants` / `ApiPaths` 一致；前端 `ApiPaths` 已引用生成的 `SERVICE_BOUNDARY_ROUTE_PATHS`，只保留调用语义和动态参数编码。
  - `ApiPaths` 已只保留 service boundary 声明的运行/预留运行路径；旧模板 auth/system 路径集中到 `ApiLegacyPaths`，并由 `node scripts/check-frontend-api-boundaries.js` 防止回流到运行路径。
  - 非 Axios 上传地址通过 `resolveApiPath(ApiPaths.common.uploadWangEditor)` 生成，避免组件手动拼 `VITE_API_URL + '/api/...'`。
  - 前端删除请求使用项目 HTTP 工具的 `request.del`。
  - 前端标准响应消息使用 `message`，`msg` 兼容集中在 `utils/http/response.ts`。
  - API envelope 契约记录在 `project_document/API_CONTRACT_GUIDE.md`。
  - OpenAPI 运行接口契约记录在 `project_document/OPENAPI_CONTRACT_GUIDE.md`；后端通过 `springdoc-openapi-starter-webmvc-api` 暴露 `/v3/api-docs`，dev/test 默认开启，prod 默认关闭并可通过 `OPENAPI_API_DOCS_ENABLED=true` 显式开启。
  - `OpenApiConfig` 已限制 OpenAPI 只扫描 `com.anjing.controller` 和 `/api/**`，并给每个 operation 补充 requestId、traceId、tenantId、userId、userName、userRoles、callerId、timeZone、language 等平台请求头。
  - `node scripts/generate-openapi-frontend-types.js` 已从真实 OpenAPI JSON 生成 `frontend/src/contracts/openapi/schemas.ts` 和 `frontend/src/contracts/openapi/operations.ts`，operation 类型已包含 `OpenApiOperationPathParams` 和 `OpenApiOperationQuery`；`frontend/src/api/model/authModel.ts` 已从生成的 `OpenApiOperationRequest<'login'>`、`OpenApiOperationData<'login'>`、`OpenApiOperationData<'getCurrentUser'>` 和 `OpenApiOperationRequest<'refreshToken'>` 派生类型，`frontend/src/api/openapiClient.ts` 已通过 `OPENAPI_OPERATIONS` 提供 `openApiRequest` typed 调用入口，`frontend/src/api/auth.ts` 已迁移到 `operationId` 调用。
  - `node scripts/check-openapi-runtime-contract.js` 已读取真实 OpenAPI JSON、`contracts/service-boundaries.json` 和 `contracts/platform-contract.json`，校验 openapi 运行 route/method 全量存在，并由 `./scripts/probe-backend-dev.sh` 自动执行；`probe-backend-dev.sh` 同时执行 `node scripts/generate-openapi-frontend-types.js <openapi.json> --check` 防止前端生成 schema / operation 类型过期。
  - `AuthController` 已将登录、当前用户和刷新 Token 的运行 payload 从 `Map` 收敛为 `LoginRequest`、`RefreshTokenRequest`、`AuthTokenResponse` 和 `CurrentUserResponse`，前端 `authModel.ts` 与全局 `Api.Auth` 类型已同步。
  - `contracts/platform-contract.json` 已记录 API 前缀、响应 envelope、分页字段、请求上下文头、前端/后端透传头、UTC 时间策略、默认语言、支持语言和错误码分段，并生成 `backend/src/main/java/com/anjing/model/constants/PlatformContractConstants.java` 与 `frontend/src/contracts/platform-contract.ts` 供前后端基础工具复用；`node scripts/generate-platform-contract-backend.js --check`、`node scripts/generate-platform-contract-frontend.js --check` 和 `node scripts/check-platform-contract.js` 已校验生成产物、Java/TypeScript/文档一致。
  - 前端 `requestId` 每次请求生成、`traceId` 在浏览器会话内复用；语言请求头已通过 `frontend/src/utils/locale` 从生成契约读取默认语言和支持语言，按用户语言或浏览器语言匹配后发送；`HttpError` 已从响应体、响应头或请求头提取 `requestId` / `traceId`，业务错误码、HTTP 状态错误、网络错误和 401 分支都保留链路上下文；`node scripts/check-frontend-context-contract.js` 已禁止前端源码直接手写平台上下文请求头并校验错误上下文。
  - 后端 `RequestContextFilter` 已统一生成/透传 requestId、traceId、租户、用户、语言和时区上下文，`Accept-Language` 已通过 `LocaleUtils.normalizeAcceptLanguage` 归一化到 platform contract 支持语言，`X-Time-Zone` 已通过 `TimeZoneUtils.normalizeTimeZone` 归一化到默认 UTC 或合法 zone id，`ControllerLogAspect` 访问日志已固定输出接口路径、耗时和错误码；`node scripts/check-backend-context-contract.js` 已校验后端入站上下文、MDC、响应头、访问日志字段和远程调用透传。
  - `GlobalRequestContextHolder` 已提供纯 Java 上下文快照和 `runWith/callWith` 辅助方法；`RequestContextTaskDecorator` 与统一 `applicationTaskExecutor` 已让 `@Async` 线程传播 requestId、traceId、租户、用户、语言、时区和 MDC；`node scripts/check-async-context-contract.js` 已纳入守护。
  - `PageResult` 已去除 Spring Data Page 依赖，作为纯分页 payload；Spring Page 需要在业务层展开为 records/total/current/size。
  - 共享内核边界记录在 `project_document/SHARED_KERNEL_GUIDE.md`，`LocaleUtils` 和 `TimeZoneUtils` 已作为纯 Java 全球化契约工具纳入共享候选，`JsonUtils` 已具备无 Spring 容器默认 mapper；`node scripts/check-shared-kernel.js` 已校验候选共享类不依赖 Spring Web、Servlet、JPA 或运行时层。
  - 业务当前时间统一通过 `DateUtils.nowIso()`、`DateUtils.now(pattern)`、`DateUtils.nowEpochMilli()` 等 UTC 出口获取，默认 zone 来自 `TimeZoneUtils.defaultZoneId()`，自检脚本已禁止业务源码直接调用 `Instant.now()` / `LocalDateTime.now()`。
  - 前端展示时间、展示语言、导出文件名时间戳、错误时间戳和日期 key 已收口到 `frontend/src/utils/time` 与 `frontend/src/utils/locale`；`node scripts/check-frontend-time-contract.js` 已禁止页面/组件直接散落 `toLocale*`、`Intl.DateTimeFormat`、浏览器语言读取和 `useDateFormat`。
  - HTTP 服务间调用使用 `RemoteHttpClient`，内部服务地址通过 `ServiceEndpointResolver` 统一解析，endpoint 来源通过 `ServiceEndpointRegistry` 查询，默认 `ConfiguredServiceEndpointRegistry` 读取 `app.remote-http.service-base-urls`，调用方身份通过 `RemoteCallerResolver` 统一解析，默认 `DefaultRemoteCallerResolver` 支持请求级覆盖、配置默认值和应用 id 回退，默认关闭的 `ConfiguredRemoteCallPolicy` 已提供 service 阻断、全局 caller 白名单和服务级 caller 白名单样板，调用审计通过 `RemoteCallObserver` 预留审计表、指标和 tracing 挂点，调用侧优先使用 `serviceId + path`；标准响应、分页响应或列表响应使用 `ParameterizedTypeReference<T>` 保留嵌套泛型；自定义 adapter 使用 `RemoteCallWrapper.serviceCallHeaders(callerId)` 按 `backendPropagatedHeaders` 透传 requestId、traceId、租户、用户、语言和时区。
  - `node scripts/check-remote-http-contract.js` 已校验后端远程 HTTP 调用具备服务解析扩展点、服务发现/地址注册表扩展点、配置型地址表、调用方身份解析扩展点、配置型调用治理策略、调用审计扩展点、泛型响应重载、示例不再拼接本地绝对 URL、文档使用 `serviceId + path`，且服务间透传头来自 platform contract。
  - 错误码按 `project_document/ERROR_CODE_GUIDE.md` 分段，`node scripts/check-error-codes.js` 已校验 Java 错误码枚举必须实现 `ErrorCode`、4 位数字、全局唯一并落在 `contracts/platform-contract.json` 分段内。
  - 远程调用默认只重试 `1800-1899`，`RemoteCallWrapper` 已读取 `PlatformContractConstants.ErrorCodes.RETRYABLE_RANGES`，避免在业务代码里硬编码重试范围。
  - `node scripts/check-openapi-contract.js` 已校验 springdoc 依赖、OpenAPI 配置、平台请求头、Auth 明确 DTO/VO、前端生成 schema / operation 类型、typed OpenAPI 调用入口和前端 auth 类型派生关系。
  - `./scripts/check-contracts.sh` 已将 API 路径、服务边界、响应 envelope、分页字段、请求上下文、远程调用、时间工具、错误码、OpenAPI 和前后端防破窗边界的关键约束纳入可执行检查。
  - 可选中间件状态按 `project_document/FEATURE_STATUS_GUIDE.md` 使用 `disabled/configured/ready/degraded`，并通过 `/api/test/features` 输出。
  - 后端 profile 矩阵按 `project_document/ENVIRONMENT_PROFILE_GUIDE.md` 管理，`dev/test` 使用 H2 轻启动，`prod` 通过环境变量显式开启 MySQL 和外部中间件。
  - 本地轻启动验证记录在 `project_document/LOCAL_STARTUP_GUIDE.md`，已验证 `dev` profile 下 `/api/test/health`、`/api/test/features` 和 `/v3/api-docs` 可返回。

下游复用验证：

- `infra-skill-hub` 已从本母版复制接入前后端骨架。
- `infra-skill-hub` 已完成项目名、后端 `artifactId`、`spring.application.name`、数据库名和前端包名切换。
- `infra-skill-hub` 已通过自检、复制烟测、后端打包、前端构建和 clean dry-run。
- `infra-skill-hub` 已落地 `SkillRegistry` 后端 CRUD 和前端管理页，验证母版 Prompt 契约可支撑真实 Infra 领域模块。
- `infra-skill-hub` 已落地 `SkillDiscoveryController`、统一调用入口、`SkillInvocationLog` 和基础统计，验证母版可承载第二阶段领域演进。
- `infra-skill-hub` 已开始 H3，落地 `SkillProtocolDispatcher`、`SkillProtocolAdapter`、`HttpSkillProtocolAdapter` 和 `McpSkillProtocolAdapter`，验证母版可承载协议适配层演进。
- `infra-skill-hub` 已落地 `SkillGovernancePolicy` 和 `SkillGovernanceService`，支持启停、调用方白名单、日配额和失败熔断，验证母版可承载调用治理演进。
- `infra-skill-hub` 已落地 `SkillInvocationLogPageVO` 和 `/api/skill-governance/invocations`，支持调用审计与失败追踪查询，验证母版可承载运维查询能力演进。
- `infra-skill-hub` 已落地前端 `SkillGovernance` 页面、`SkillPolicyDialog` 和 `SkillInvocationDetailDialog`，验证母版可承载治理管理面的真实迭代。
- `infra-skill-hub` 已落地 `InternalSkillProtocolAdapter`、`InternalSkillHandler` 和 `SkillHubEchoInternalSkillHandler`，内置 `skill-hub.echo` 诊断 handler，验证母版可承载内部能力适配层演进。
- `infra-skill-hub` 已落地 `SkillGovernanceMetricsVO`、`/api/skill-governance/metrics` 和 `SkillGovernanceMetricsPanel`，验证母版可承载治理指标看板演进。
- `infra-skill-hub` 已落地 `SkillProtocolProperties`、`SkillProtocolMetadataResolver` 和 `SkillProtocolOptions`，支持 HTTP/MCP timeout、retry、authorization 和 headers 配置，验证母版可承载生产协议配置演进。
- `infra-skill-hub` 已落地 `SkillCredentialProperties`、`SkillCredentialResolver`、`SkillCredentialSummaryVO` 和 `SkillCredentialDrawer`，支持 `credentialRef` 引用后端托管凭据并提供脱敏查询，验证母版可承载凭据托管演进。
- `infra-skill-hub` 已在 `SkillRegistry` 注册模型中落地 `protocolConfig`，支持注册级协议默认配置并允许调用级 metadata 覆盖，验证母版可承载领域配置模型演进。
- `infra-skill-hub` 已在 Skill 注册弹窗中落地凭据引用选择体验，支持从脱敏凭据列表选择 `credentialRef` 并写入 `protocolConfig`，验证母版可承载配置选择型管理面演进。
- `infra-skill-hub` 已落地 `SkillInvokeCallerContext` 和请求身份头治理匹配，支持 `X-Skill-Caller-Id`、`X-User-Id`、`X-User-Name`、`X-User-Roles` 与 `role:R_ADMIN` 等白名单 token，验证母版可承载权限联动演进。
- `infra-skill-hub` 已落地 `SkillGovernancePolicyBatchRequest`、策略列表接口、批量启停接口和前端 `SkillPolicyDrawer`，验证母版可承载批量管理型治理页面演进。
- `infra-skill-hub` 已落地 `SkillGovernanceProperties`、`SkillGovernancePolicyApplyDefaultRequest`、`SkillGovernancePolicyTemplateVO` 和默认策略模板批量应用，验证母版可承载配置驱动的治理模板演进。

## 当前发布判断

当前状态适合作为 Anjing 内部后续开源项目的母版起点。

对外发布前按 `project_document/DEMO_EVIDENCE.md` 留存本次实际截图或数据库 profile 演示证据即可；母版侧已经提供证据清单和门禁入口。

## 下一步

1. 继续在 `infra-skill-hub` 中推进认证中心 / API Gateway 职责边界和凭据生命周期管理。
2. 记录复用时还需要回流到底座的类型、路由、环境变量和 Prompt 契约问题。
3. 将可复用能力留在母版，将具体领域能力放进独立 Infra 项目。
