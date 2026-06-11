# Roadmap

## 定位

`infra-dev-scaffolding` 是 Anjing 开源项目的工程母版。

它提供一个可启动、可演示、可复制、可被 AI 协作工具理解的全栈基础工程。后续 `agent-aigc`、`agent-customer-service`、`agent-knowledge` 以及 `infra-auth`、`infra-api-gateway`、`infra-llm-gateway`、`infra-skill-hub` 都应从这套工程结构继承或裁剪。

## 目标

1. 开箱能跑：前端游客模式无后端可体验，后端示例 API 可启动和验证。
2. 工程规范稳定：统一目录、类型、路由、权限、请求、响应、异常、日志和环境配置。
3. 可复制：新项目能从本仓库复制并快速改名、换端口、换业务模块。
4. 可被 AI 协作：Cursor Rules、Prompts 和接入提示词让 AI 生成或迁移代码时遵循本项目结构。

## 非目标

- 不做完整业务系统。
- 不在底座中沉淀具体 Agent 业务逻辑。
- 不把认证中心、API 网关、LLM 网关、Skill Hub 的领域能力提前塞进本仓库。
- 不追求功能越多越好，优先保持清晰、稳定、易复制。

## 阶段规划

### S0: 构建与入口收口

目标：项目可以被可信地启动、构建和演示。

验收：
- `frontend` 通过 `pnpm build`。
- `backend` 通过 `mvn -q -DskipTests package`。
- `./scripts/quality-gate.sh` 串联母版质量门禁，后续可接入任意 CI。
- 前端游客模式进入 `/dashboard/console`。
- README 中的环境要求、端口、启动命令和验证命令准确。
- 敏感配置只出现在模板或环境变量说明中。

### S1: 工程母版收口

目标：复制本仓库可以生成一个新项目骨架。

验收：
- 明确前端、后端、文档、参考资料目录职责。
- 提供“复制为新项目”的改名清单。
- 提供复制改名 smoke 验证，证明自检脚本适配新项目名。
- 前端环境变量模板、后端 `.env.example`、数据库名、端口说明一致。
- 示例代码和模板代码边界清楚，示例可以删除或替换。
- 项目级约束文档清楚记录母版长期边界，并有脚本守护核心规则。
- 新增模块、UI 设计、演示证据和 GitHub CI 都有清晰入口，并由脚本守护。

### S2: AI 协作资产收口

目标：Cursor Rules / Prompts 成为本项目的核心差异点。

验收：
- Rules 能覆盖 Vue、TypeScript、路由、API、Spring Boot、分层结构和错误处理。
- Prompts 覆盖前端 API、列表页、业务组件、弹窗，以及后端 CRUD 和单端点生成。
- `project_document/AI_ASSETS.md`、`.cursor/**/README` 和 `scripts/check-template.sh` 中的资产数量一致。
- `project_document/SCAFFOLD_ADOPTION_PROMPT.md` 能引导 Codex / Cursor 按母版文档、契约和质量门禁迁移旧项目或初始化新项目。

### S3: 后续项目复用验证

目标：用本脚手架支撑后续开源项目迭代。

验收：
- `agent-aigc`、`agent-customer-service`、`agent-knowledge` 的共性前端类型问题能回流到底座。
- 新建 Infra 项目时能复用本仓库的 README 模板、环境模板和发布清单。
- 记录哪些能力应留在底座，哪些能力应抽到独立 infra 项目。

### S4: 契约与全球化基线

目标：单体仍可运行，但 API、时间、上下文和 URL 已经具备未来拆分基础。

验收：
- 统一前后端 API path 管理，减少散落 URL 字符串。
- 使用机器可读 service boundary manifest 记录运行模块、预留模块和未来服务归属。
- 统一响应 envelope 和分页 payload，新增接口只使用 `code/message/data/timestamp/requestId` 与 `records/current/size/total`。
- 请求链路具备 `requestId`、`traceId`、语言和时区上下文。
- 后端默认 UTC，可通过环境变量覆盖应用时区。
- 前端具备统一时间工具，新增页面不直接手写日期格式化逻辑。
- 后端提供 OpenAPI JSON 作为前端类型生成、网关和服务调用方的接口契约入口。
- AI Rules / Prompts 纳入 URL、响应、时间和上下文契约。

### S5: 分布式可观测基线

目标：未来拆服务后仍能追踪一次请求从前端到后端再到远程调用的链路。

验收：
- 日志统一输出 requestId、traceId、用户、租户、路径、耗时和错误码。
- 远程调用 wrapper 和 HTTP client adapter 支持上下文透传、调用方身份解析扩展点、超时、重试、审计字段、服务解析扩展点、服务发现/地址注册表扩展点、调用治理策略扩展点、调用审计扩展点和标准/分页/列表响应的泛型反序列化。
- 错误码按分段指南分配，并由脚本校验唯一性、格式、范围和可重试错误。
- 中间件状态能区分 disabled、configured、ready、degraded，并提供可被前端/运维页消费的状态接口。

### S6: 服务边界与可选适配层

目标：母版能自然裁剪出 `infra-auth`、`infra-api-gateway`、`infra-skill-hub` 等独立项目。

验收：
- 明确 auth、gateway、common、admin、business 的 API prefix 和包边界。
- 服务边界 manifest 能校验 `ApiConstants`、`ApiPaths`、Controller 和 OpenAPI 标记一致。
- 可选中间件有 dev/test/prod profile 示例、状态接口和验证命令。
- 下游项目验证出的通用工具回流母版，领域能力留在下游。

### S7: 契约生成与共享包

目标：减少手写重复契约，让 AI 和人都沿着同一套约定生成代码。

验收：
- OpenAPI schema、operation、path/query 参数生成前端 types，并提供轻量 typed `openApiRequest` 调用入口；后续评估继续生成完整 API client。
- 评估抽出共享包，只放稳定工具和类型。
- AI Prompts 生成模块时自动引用统一 URL、统一响应和统一时间工具。

当前进展：
- `contracts/platform-contract.json` 已记录平台级响应、分页、请求头、前端/后端透传头、时间、语言和错误码分段契约。
- `backend/src/main/java/com/anjing/model/constants/PlatformContractConstants.java` 已由 manifest 生成并被后端 API 前缀、请求头、透传头列表和响应成功码复用。
- `frontend/src/contracts/platform-contract.ts` 已由 manifest 生成并被前端 HTTP、响应、时间和 `utils/locale` 语言上下文工具复用。
- `scripts/generate-platform-contract-backend.js --check`、`scripts/generate-platform-contract-frontend.js --check` 和 `scripts/check-platform-contract.js` 已校验生成产物、前后端代码及文档一致。
- `scripts/check-error-codes.js` 已校验 Java 错误码枚举和 platform contract 分段一致，`RemoteCallWrapper` 已读取生成的可重试范围。
- `springdoc-openapi-starter-webmvc-api` 已提供 `/v3/api-docs`，`scripts/generate-openapi-frontend-types.js` 已生成 `frontend/src/contracts/openapi/schemas.ts` 与 `frontend/src/contracts/openapi/operations.ts`，其中 operation 类型包含 path/query 参数；`frontend/src/api/openapiClient.ts` 已提供按 `operationId` 调用的 typed helper，`scripts/check-openapi-contract.js`、`scripts/check-openapi-runtime-contract.js` 与 `./scripts/probe-backend-dev.sh` 已校验 OpenAPI 配置、service boundary 运行路径、平台请求头、前端生成 schema / operation 类型和 auth DTO/VO 契约。
- `contracts/service-boundaries.json` 已记录当前运行、示例、预留和未来服务边界，后端 `ServiceBoundaryConstants.java` 与前端 `service-boundaries.ts` 已由 manifest 生成，`scripts/check-service-boundaries.js` 已校验 route、basePath 和前后端路径一致。
- `ApiPaths` 已和旧模板路径分离，`ApiLegacyPaths` 承载历史兼容路径，`scripts/check-frontend-api-boundaries.js` 已防止旧路径回流到运行路径。
- `frontend/src/utils/http/context.ts` 已统一生成前端 `requestId`、会话级 `traceId`、语言和时区请求头，并通过 `frontend/src/utils/locale` 从生成契约读取默认语言和支持语言；`HttpError` 已统一保留 `requestId` / `traceId`，`scripts/check-frontend-context-contract.js` 已防止平台上下文头和错误上下文逻辑散落到页面或 API 模块。
- 后端 `RequestContextFilter`、MDC、`ControllerLogAspect` 访问日志和远程调用上下文透传已由 `scripts/check-backend-context-contract.js` 守住，`Accept-Language` 通过 `LocaleUtils` 归一化到支持语言，`X-Time-Zone` 通过 `TimeZoneUtils` 归一化到默认 UTC 或合法 zone id，日志统一输出 requestId、traceId、用户、租户、路径、耗时和错误码。
- 后端 `GlobalRequestContextHolder` 已提供上下文快照能力，`RequestContextTaskDecorator` 和统一 `applicationTaskExecutor` 已支持 `@Async` 场景传播请求上下文与 MDC，`scripts/check-async-context-contract.js` 已纳入守卫。
- `frontend/src/utils/time`、`frontend/src/utils/locale` 和后端 `TimeZoneUtils` 已承载前端展示时间、展示语言、日期 key、文件名时间戳、错误时间戳和后端请求时区归一化，`scripts/check-frontend-time-contract.js` 已防止时间格式化和浏览器语言读取逻辑散落到页面组件。
- `RemoteHttpClient` 已支持通过 `serviceId + path` 调用内部服务，服务地址由 `ServiceEndpointResolver` 解析，endpoint 来源由 `ServiceEndpointRegistry` 查询，默认配置型 registry 读取 `app.remote-http.service-base-urls`；调用方身份由 `RemoteCallerResolver` 解析，默认实现支持请求级覆盖、配置默认值和应用 id 回退；并通过 `RemoteCallPolicy` 提供熔断、限流和治理策略挂点，通过 `RemoteCallObserver` 提供调用审计、指标和 tracing 挂点，通过 `ParameterizedTypeReference<T>` 保留嵌套泛型响应；`RemoteCallWrapper` 已按 `backendPropagatedHeaders` 透传服务间上下文，`scripts/check-remote-http-contract.js` 已纳入守卫。
- `project_document/SHARED_KERNEL_GUIDE.md` 已定义共享内核边界。
- `scripts/check-shared-kernel.js` 已守护可抽取契约/工具类不能依赖 Spring Web、Servlet、JPA 或运行时层。
- `project_document/PROJECT_CONSTRAINTS.md`、`project_document/NEW_MODULE_GUIDE.md`、`project_document/UI_DESIGN_GUIDE.md`、`project_document/DEMO_EVIDENCE.md`、`project_document/ci/quality-gate.yml`、`scripts/check-backend-controller-contracts.js`、`scripts/check-scaffold-governance.js` 和 `scripts/check-frontend-openapi-boundaries.js` 已把防破窗约束沉淀为文档、CI 模板和可执行门禁。

## 当前优先级

当前阶段状态见 `project_document/STATUS.md`。

1. 保持 S0 / S1 / S2 的验证链路稳定：自检、复制烟测、前端构建、后端打包。
2. 推进 S4 契约与全球化基线：统一 URL、请求上下文、时间策略、OpenAPI 类型生成和 AI 生成约束。
3. 用下游 Infra 项目验证 S5 / S6 的分布式、治理和服务边界设计。
4. 将复用过程中发现的共性类型、路由、环境变量和 Prompt 契约问题回流到底座。

## 成功标准

当一个新项目要从本仓库出发时，开发者应该只需要完成四件事：

1. 复制仓库。
2. 修改项目名、端口、数据库名和品牌文案。
3. 按 README 启动前后端。
4. 用 Prompts 生成第一组业务模块。
