# Architecture Evolution

本文档记录 `infra-dev-scaffolding` 作为工程脚手架继续演进时的架构判断。

结论：脚手架需要提前考虑微服务、分布式、全球化、时区、统一 URL 和共享工具，但不能把所有未来能力一次性塞进母版。母版应该沉淀稳定契约、基础抽象、目录边界和最小默认实现；重型平台能力通过可选适配层或下游 Infra 项目验证后再回流。

## 核心原则

1. 先定契约，再定实现。

   API 响应、错误码、请求上下文、时间格式、URL 命名、权限头、日志字段这些一旦分散，后面迁移成本很高。母版应优先统一这些契约。

2. 保留扩展点，不预装复杂平台。

   API Gateway、服务注册发现、链路追踪、消息队列、多租户计费、权限中心都应该被考虑，但不应默认成为母版运行依赖。母版提供接口、配置样例和接入边界即可。

3. 单体优先可跑，分布式优先可演进。

   当前项目仍应保持一个前端加一个 Spring Boot 后端即可启动。面向未来的设计体现在包结构、请求头、配置、日志和工具层，而不是提前拆多个服务。

4. 工具类可以共享，业务逻辑不能共享。

   时间、ID、JSON、校验、URL、错误码、请求上下文、HTTP client wrapper 可以沉淀为共享能力。具体 Agent、Skill、订单、支付、客服等领域能力应留在下游项目。

## 当前状态判断

已有基础：

- 后端已有 `ApiConstants`、`GlobalRequestContext`、`DateUtils`、`RemoteCallWrapper`、统一响应、异常、分布式锁、Redis/中间件开关。
- 前端已有 `utils/http`、`api/`、`types/`、`locales/`、`config/`、`utils/constants/links`、路由核心和权限守卫。
- 文档中已有 Roadmap、Status、Template Boundaries、Copy Guide、Release Checklist。
- `project_document/PROJECT_CONSTRAINTS.md` 已记录项目级防破窗约束，作为每轮迭代前的母版底线。
- `project_document/NEW_MODULE_GUIDE.md`、`project_document/UI_DESIGN_GUIDE.md` 和 `project_document/DEMO_EVIDENCE.md` 已把新增模块、极简玻璃 UI 和演示证据纳入母版入口。

主要缺口：

- 前端 API URL 已开始收口到 `ApiPaths`，后端运行 Controller 已开始引用 `ApiConstants`；`ApiConstants` 内部也已收口为 `API_PREFIX`、模块 `BASE`、相对子路径和 `*_FULL`，并由 `scripts/check-api-constants.js` 守护。路径规则已记录到 `project_document/API_PATH_GUIDE.md`；`contracts/service-boundaries.json` 已进一步记录 auth、test、common 当前边界和 user、admin、integration 未来服务边界，生成后端 `ServiceBoundaryConstants.java` 与前端 `service-boundaries.ts`，并由 `scripts/check-service-boundaries.js` 校验。旧模板 auth/system 路径已从 `ApiPaths` 拆到 `ApiLegacyPaths`，由 `scripts/check-frontend-api-boundaries.js` 防止回流。
- 响应契约已开始收敛到 `APIResponse` + `message` + `PageResult(records/current/size/total)`，并已记录到 `project_document/API_CONTRACT_GUIDE.md`；`msg`、`BaseResponse`、`PageResponse` 仅作为旧接口或远程响应兼容入口。
- 后端非示例 Controller 的响应和 DTO/VO 边界已由 `scripts/check-backend-controller-contracts.js` 守护，避免真实接口退回裸 `Map` 或绕过 `APIResponse<T>`。
- OpenAPI 运行接口契约已开始落地，`/v3/api-docs` 由 `springdoc-openapi-starter-webmvc-api` 暴露，dev/test 默认开启、prod 默认关闭；`scripts/generate-openapi-frontend-types.js` 已把运行 schema、operation 和 path/query 参数类型生成到 `frontend/src/contracts/openapi/schemas.ts` 与 `frontend/src/contracts/openapi/operations.ts`，`frontend/src/api/openapiClient.ts` 已提供按 `operationId` 调用的 typed helper，`project_document/OPENAPI_CONTRACT_GUIDE.md`、`scripts/check-openapi-contract.js` 和 `scripts/check-openapi-runtime-contract.js` 已守护前端类型生成和调用入口。
- 前端 OpenAPI 生成物导入边界已由 `scripts/check-frontend-openapi-boundaries.js` 守护，页面和组件不能直接依赖生成目录。
- `scripts/quality-gate.sh` 已把契约脚本、复制烟测、关键后端单测、后端打包、前端构建和 dev runtime probe 串成可接入 CI 的母版质量门禁。
- `project_document/ci/quality-gate.yml` 已提供 GitHub Actions 模板，用于在启用后于 push 和 PR 上执行同一套 `./scripts/quality-gate.sh`，避免本地门禁和 GitHub 状态分叉。
- 平台级契约已沉淀为 `contracts/platform-contract.json`，覆盖 API 前缀、响应 envelope、分页字段、请求头、前端/后端透传头、时间策略、语言策略、错误码分段和可重试范围；后端生成 `PlatformContractConstants.java`，前端生成 `frontend/src/contracts/platform-contract.ts`，供路径、请求头、响应解析、时间工具、语言上下文和远程重试判断复用，并由 `scripts/generate-platform-contract-backend.js --check`、`scripts/generate-platform-contract-frontend.js --check`、`scripts/check-platform-contract.js` 校验生成产物、前后端与文档一致性。
- 时间策略已开始转向 UTC 默认和客户端时区展示；后端 `TimeZoneUtils` 已把 `X-Time-Zone` 归一化到合法 zone id 或 platform contract 默认 UTC，前端展示时间、导出文件名时间戳、错误时间戳和日期 key 已收口到 `frontend/src/utils/time`，展示语言和请求语言已收口到 `frontend/src/utils/locale`，并由 `scripts/check-frontend-time-contract.js` 阻止新的散落格式化。
- 请求上下文已开始具备 `requestId`、会话级 `traceId`、语言和时区透传，并已接入日志格式、Controller 访问日志字段、异步线程池传播、远程调用请求头生成、前端 `HttpError` 链路上下文和统一 HTTP client adapter；前端可透传头和后端服务间可透传头已由 platform contract 分开声明，语言上下文已通过前端 `utils/locale` 与后端 `LocaleUtils` 归一化到 platform contract 支持列表，时区上下文已通过 `TimeZoneUtils` 归一化到默认 UTC 或合法 zone id。前端上下文头已由 `scripts/check-frontend-context-contract.js` 守护，后端入站上下文和访问日志已由 `scripts/check-backend-context-contract.js` 守护，异步上下文传播已由 `scripts/check-async-context-contract.js` 守护，后续需要继续接入权限上下文和真实 RPC client adapter。
- 错误码已补充分段指南和 `scripts/check-error-codes.js`，后续新模块应按 `project_document/ERROR_CODE_GUIDE.md` 分配 code，并通过唯一性、4 位数字和 manifest 分段校验。
- 共享内核边界已开始收口，`project_document/SHARED_KERNEL_GUIDE.md` 和 `scripts/check-shared-kernel.js` 已约束未来可抽 `anjing-common` 的契约/工具类不依赖 Spring Web、Servlet、JPA 或运行时层。
- 前端已有统一时间工具层，后续需要让日期控件、文件名、通知时间等存量逻辑逐步迁移。
- 中间件状态已能区分 `disabled/configured/ready/degraded`，并通过 `/api/test/features` 输出；dev/test/prod profile 矩阵已落地，后续仍需补真实探测开关。
- 微服务远程调用已有包装工具、`ServiceEndpointResolver` 服务解析扩展点、`ServiceEndpointRegistry` 服务发现/地址注册表扩展点、配置型默认 registry、`RemoteCallerResolver` 调用方身份解析扩展点、`ConfiguredRemoteCallPolicy` 轻量调用治理样板、`RemoteCallObserver` 调用审计/指标扩展点和 `ParameterizedTypeReference<T>` 泛型响应入口，可支撑标准响应、分页响应和列表响应；真实熔断、限流、灰度和服务网格仍留给下游项目替换实现。

## 母版应该内置

这些是脚手架层面的长期资产，应该逐步落地：

- 统一 API 契约：`code`、`message`、`data`、`timestamp`、`requestId`、分页字段、错误码命名。
- 统一接口文档契约：后端运行接口暴露 OpenAPI JSON，前端类型生成和 AI Prompts 都从同一入口理解 DTO/VO。
- 统一 URL 管理：后端 `ApiConstants` 与前端 `ApiPaths` / `apiUrl()`，禁止业务页面手写 URL 字符串。
- 统一服务边界：机器可读 manifest 记录 basePath、owner、currentHost、copyAction 和 runtime route，为未来网关转发和微服务拆分提供事实来源。
- 统一请求上下文：请求进入后生成或透传 `X-Request-Id`，保留 `X-Trace-Id`、`X-Tenant-Id`、`X-User-Id`、`X-Time-Zone`、`Accept-Language`，并把时区和语言归一化到 platform contract 支持范围。
- 统一时间策略：服务端存储 UTC，接口输出 ISO-8601，前端按用户时区格式化展示。
- 统一环境配置：前端 env、后端 yml、README、复制指南中的端口、API base、应用名、数据库名保持一致。
- 统一工具层：ID、JSON、校验、时间、URL、存储、错误处理、日志脱敏。
- 共享内核边界：可抽公共包的契约和工具保持纯净，运行时 adapter 单独留在后端应用层。
- 统一日志字段：应用名、环境、requestId、traceId、userId、tenantId、耗时、错误码。
- 统一 AI 生成契约：Rules / Prompts 必须告诉 AI 新模块应使用统一响应、统一 URL、统一时间工具和统一 API client。

## 母版只保留扩展点

这些能力要考虑，但不应成为默认运行依赖：

- API Gateway：母版定义服务前缀、认证头和转发约定，下游 `infra-api-gateway` 负责具体网关。
- Auth Center：母版保留 mock auth 和 token/header 契约，下游 `infra-auth` 负责真实账号、租户、权限。
- Service Discovery：母版保留服务名、调用方身份、远程调用 wrapper、`ServiceEndpointResolver` 和 `ServiceEndpointRegistry` 扩展点，不默认引入注册中心。
- Distributed Tracing：母版保留 trace/request id 字段，不默认绑定某个 tracing 平台。
- MQ / Event Bus：母版保留事件命名和幂等建议，不默认要求 Kafka/RabbitMQ 启动。
- Object Storage：母版保留上传接口契约和配置样例，不默认绑定 MinIO/OSS 业务实现。
- Multi-region / Global Deploy：母版保留时区、语言、区域配置契约，不实现真实多活部署。

## 不应进入母版

- 具体业务领域模型和流程。
- 具体 Agent / Skill / LLM 编排业务。
- 具体支付、订单、客服、知识库、RAG 逻辑。
- 只能在某个下游项目使用的页面和接口。
- 必须依赖外部平台才能启动的默认能力。

## 演进阶段

### S4: 契约与全球化基线

目标：单体仍可运行，但 API、时间、上下文和 URL 已经具备未来拆分基础。

验收：

- 后端新增接口只使用 `APIResponse` 标准结构和 `PageResult` 分页 payload；前端将 `msg` 兼容集中在 HTTP helper 内部。
- 前后端新增统一 API path 管理，不再新增散落 URL 字符串。
- 后端新增请求上下文过滤器或拦截器，生成并返回 `X-Request-Id`。
- 后端时间配置支持 `APP_TIME_ZONE`，默认建议 UTC；展示时区由前端或用户配置决定。
- 前端新增 `utils/time`，所有新增页面通过统一工具格式化时间。
- Cursor Rules / Prompts 增加 API path、时间和请求上下文约束。
- OpenAPI 文档入口能输出 `/api/**` 运行接口，并为后续前端类型生成提供稳定入口。

### S5: 分布式可观测基线

目标：即使未来拆服务，也能追踪一次请求从前端到后端再到远程调用的链路。

验收：

- 日志统一输出 `requestId`、`traceId`、`userId`、`tenantId`、接口路径、耗时、错误码。
- `RemoteCallWrapper` 支持调用方、上下文请求头、重试和 requestId/traceId 透传。
- `RemoteHttpClient` 支持统一 HTTP 出站调用、超时配置、上下文透传、目标服务审计描述、可重试错误映射、服务解析扩展点、调用方身份解析扩展点、调用策略扩展点、调用审计扩展点和泛型响应反序列化；后续补充真实熔断实现和 RPC adapter。
- 错误码按模块分段，有文档和脚本说明并校验哪些错误能重试、哪些必须提示用户。
- 健康检查和中间件状态能区分 disabled、configured、ready、degraded。

当前已完成：

- `logback-spring.xml` 已输出 MDC 中的 `requestId`、`traceId`、`tenantId`、`userId`。
- `ControllerLogAspect` 已复用 `RequestContextFilter` 生成的 requestId，并固定输出接口路径、耗时和错误码。
- `GlobalRequestContextHolder.capture()` / `setOrClear()` / `runWith()` / `callWith()` 已提供纯 Java 上下文快照能力，`RequestContextTaskDecorator` 和统一 `applicationTaskExecutor` 已支持 `@Async` 线程传播请求上下文与 MDC。
- `RemoteCallWrapper.serviceCallHeaders(callerId)` 已按 platform contract 的 `backendPropagatedHeaders` 生成服务间调用上下文请求头。
- `RemoteHttpClient` / `RemoteHttpRequest` 已提供 HTTP 服务间调用适配层。
- `RemoteHttpRequest` 已支持 `serviceId + path`，`RemoteHttpClient` 通过 `ServiceEndpointResolver` 解析内部服务地址，默认 `ConfiguredServiceEndpointResolver` 负责校验和拼接 path，`ServiceEndpointRegistry` 负责查询 endpoint，默认 `ConfiguredServiceEndpointRegistry` 读取 `app.remote-http.service-base-urls`；`RemoteCallerResolver` 已把 `X-Caller-Id` 调用方身份解析从 client 私有逻辑中抽出，默认 `DefaultRemoteCallerResolver` 支持请求级覆盖、配置默认值和应用 id 回退；`ConfiguredRemoteCallPolicy` 已提供默认关闭的 service 阻断、全局 caller 白名单和服务级 caller 白名单样板；`RemoteCallObserver` 已提供调用完成后的审计、指标和 tracing 挂点，默认 `NoopRemoteCallObserver` 不引入存储或 metrics 依赖；`RemoteHttpClient` 已通过 `Class<T>` 与 `ParameterizedTypeReference<T>` 两类重载支持简单和嵌套泛型响应，`scripts/check-remote-http-contract.js` 已防止示例重新回到手写本地绝对 URL、裸响应类型或直接在 client 内部硬查配置。
- `project_document/ERROR_CODE_GUIDE.md` 已记录错误码分段和远程调用重试策略。
- `scripts/check-error-codes.js` 已把 Java 错误码枚举实现、格式、全局唯一性、manifest 分段和远程可重试范围纳入自动校验。
- `MiddlewareManager.statusReport()` 和 `/api/test/features` 已提供可选能力状态基线，状态词典记录在 `project_document/FEATURE_STATUS_GUIDE.md`。
- `application-dev.yml`、`application-test.yml`、`application-prod.yml` 已提供环境矩阵，dev/test 使用 H2 轻启动，生产 MySQL 和外部能力由环境变量显式开启。
- `project_document/LOCAL_STARTUP_GUIDE.md` 已记录无 MySQL/Redis 的后端本地启动验证方式和当前证据。
- `scripts/check-contracts.sh` 已把路径、服务边界、响应、分页、上下文、远程调用和时间工具约束变成可执行守护检查。
- `scripts/check-scaffold-governance.js` 已把 CI、根入口文档、新模块指南、UI 设计基线、演示证据、全局玻璃 token、登录页玻璃/虚线入口和前端 legacy API 使用边界纳入自动校验。
- `scripts/check-api-constants.js` 已把 `ApiConstants` 内部 API 前缀约束纳入自动校验，避免共享路径常量重新散落 `"/api/..."` 字面量。
- `scripts/check-api-path-parity.js` 已改为读取 `contracts/service-boundaries.json`，把 manifest 声明的后端 `ApiConstants` 与前端 `ApiPaths` 运行路径一致性纳入自动校验。
- `scripts/check-frontend-api-boundaries.js` 已把前端运行路径和旧模板兼容路径分开校验，避免历史 mock/system API 混入服务边界运行面。
- `scripts/check-frontend-context-contract.js` 已把前端 `requestId`、`traceId`、语言和时区请求头的生成入口，以及 `HttpError` 链路上下文保留逻辑纳入自动校验，避免页面或 API 模块直接手写平台上下文头。
- `scripts/check-backend-context-contract.js` 已把后端入站上下文、MDC、响应头、Controller 访问日志字段和远程调用透传纳入自动校验。
- `scripts/check-async-context-contract.js` 已把 `@Async`、统一线程池、TaskDecorator、MDC 复制和共享内核上下文快照方法纳入自动校验。
- `scripts/check-service-boundaries.js` 已把服务边界 manifest、`ApiConstants`、`ApiPaths`、Controller base path 和 OpenAPI `@Tag` 一致性纳入自动校验。
- `scripts/generate-service-boundaries-backend.js --check` 已确保后端服务边界常量与 manifest 一致。
- `scripts/generate-service-boundaries-frontend.js --check` 已确保前端服务边界常量与 manifest 一致，`ApiPaths` 已引用生成的 `SERVICE_BOUNDARY_ROUTE_PATHS`。
- `scripts/generate-platform-contract-backend.js --check` 已确保后端平台契约生成文件与 manifest 一致。
- `scripts/generate-platform-contract-frontend.js --check` 已确保前端平台契约生成文件与 manifest 一致。
- `scripts/check-platform-contract.js` 已把 `contracts/platform-contract.json` 与 Java/TypeScript/文档的一致性纳入自动校验。
- `scripts/check-openapi-contract.js` 已把 springdoc 依赖、OpenAPI 配置、平台请求头、Auth DTO/VO、前端生成 schema、typed OpenAPI 调用入口和 auth 类型派生关系纳入自动校验。
- `scripts/check-openapi-runtime-contract.js` 已把真实 `/v3/api-docs`、service boundary 运行路径和 platform request headers 的一致性纳入 `./scripts/probe-backend-dev.sh`；`probe-backend-dev.sh` 同时通过 `scripts/generate-openapi-frontend-types.js --check` 校验前端生成 schema 未过期。
- `scripts/check-frontend-time-contract.js` 已把前端时间展示、日期 key、导出文件名时间戳和错误时间戳的统一工具入口纳入自动校验。
- `RemoteCallWrapper` 已通过 `PlatformContractConstants.ErrorCodes.RETRYABLE_RANGES` 判断可重试错误码，避免重试范围散落在业务代码。

### S6: 服务边界与可选适配层

目标：母版能自然裁剪出 `infra-auth`、`infra-api-gateway`、`infra-skill-hub` 这类独立项目。

验收：

- 明确 auth、gateway、common、admin、business 的 API prefix 和包边界。
- `contracts/service-boundaries.json` 记录运行边界、示例边界、预留边界和未来服务归属。
- 新项目复制后可以只保留需要的模块，不破坏构建。
- 可选中间件有 profile 示例、验证命令和状态接口。
- 下游项目验证出的通用工具回流母版，领域能力留在下游。

### S7: 契约生成与共享包

目标：减少手写重复契约，让 AI 和人都沿着同一套约定生成代码。

验收：

- OpenAPI schema、operation 和 path/query 参数已生成前端 types，并提供轻量 typed `openApiRequest` 调用入口；后续评估进一步生成完整 API client。
- 评估抽出 `anjing-common` / `anjing-web-common`，只放稳定工具和类型。
- AI Prompts 生成模块时可以自动引用统一 URL、统一响应和统一时间工具。

当前已完成：

- `project_document/SHARED_KERNEL_GUIDE.md` 已定义共享内核、运行时适配层和领域代码边界。
- `scripts/check-shared-kernel.js` 已检查共享内核候选类不能引入 Spring Web、Servlet、JPA 或 Controller/Config/Aspect 等运行时层。
- `contracts/platform-contract.json` 已成为后续生成基础响应类型、分页类型、请求头常量和 OpenAPI 全局约束的机器可读入口；前后端常量生成链路已落地。
- `scripts/generate-openapi-frontend-types.js` 已从运行 OpenAPI JSON 生成 `frontend/src/contracts/openapi/schemas.ts` 与 `frontend/src/contracts/openapi/operations.ts`，operation 类型已包含 path/query 参数，`frontend/src/api/openapiClient.ts` 已通过 `OPENAPI_OPERATIONS` 提供 `openApiRequest` typed 调用入口，auth API model 和 auth API 调用已从生成的 operation request/data 类型派生。
- `PageResult` 已去除 Spring Data Page 依赖，保持标准分页 payload 可抽取。

## 近期推荐任务

1. 收敛 API 响应结构。

   统一为 `code`、`message`、`data`、`timestamp`、`requestId`。前端已将 `msg` 兼容集中到 HTTP helper，后续删除旧 mock / 第三方兼容后移除 `msg`。

2. 建立前端 `ApiPaths`。

   新建集中 URL 管理文件，先覆盖 auth、system、user 这些已有 API。页面和服务不再直接手写 `/api/...`。

3. 建立时间策略。

   后端默认 UTC，`DateUtils` 已提供 `nowInstant()`、`nowUtc()`、`nowIso()`、`now(pattern)`、`nowEpochMilli()`；前端已新增 `utils/time`，并通过请求头透传客户端时区。

4. 建立请求上下文。

   后端通过 filter/interceptor 统一读取和生成 requestId、traceId、locale、timeZone，其中 locale 由 `LocaleUtils` 按 platform contract 归一化，timeZone 由 `TimeZoneUtils` 回落到默认 UTC 或合法 zone id。前端通过 `utils/locale` 和 `utils/time` 统一生成语言和时区。

5. 更新 AI Rules / Prompts。

   让后续 AI 生成的 CRUD、列表页、API 模块默认遵守统一 URL、响应、时间和上下文契约。

## 决策答案

要考虑那么多，但不要一次性实现那么多。

脚手架真正要做的是把未来分布式和全球化一定会用到的“共同语言”先定下来：URL、响应、错误码、时间、请求上下文、日志、配置、工具层。至于微服务网关、认证中心、注册中心、链路追踪平台、消息队列这些重能力，母版只保留入口和契约，让下游 Infra 项目按真实需求实现，再把成熟的共性部分回流。
