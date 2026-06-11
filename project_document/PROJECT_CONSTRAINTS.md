# Project Constraints

本文档定义 `infra-dev-scaffolding` 的长期工程约束。它不是代码风格指南，而是防止母版在迭代中变重、变散、变脏的底线。

原则：清晰优先，简单优先，可验证优先。能用契约和脚本守住的规则，不只停留在口头约定。

## 总体边界

- 母版只沉淀通用工程能力、契约、扩展点和最小默认实现。
- 具体业务领域能力留在下游项目；示例能力必须可删除、可替换、可识别。
- 默认启动必须轻量，本地 dev/test 不依赖 MySQL、Redis、Kafka、MinIO、注册中心或网关。
- 新增能力先问三个问题：是否可被多个 Anjing 项目复用，是否能被复制项目裁剪，是否能被脚本验证。
- 重平台能力只保留接口和配置样例；真实实现由 `infra-auth`、`infra-api-gateway`、`infra-skill-hub` 等下游项目验证后再回流。
- 新增业务模块必须先按 `project_document/NEW_MODULE_GUIDE.md` 确认边界、契约、类型生成和验证命令。

## 后端约束

- 运行 Controller 路径必须引用 `ApiConstants`，不要直接写 `"/api/..."`。
- 非示例 Controller 返回值必须使用 `APIResponse<T>`，分页 payload 使用 `PageResult<T>`。
- 非示例 Controller 不要用 `Map<String, Object>` 承载真实请求或响应；需要明确 DTO / VO。
- 示例 Controller 必须标记 `@ScaffoldSample`，复制项目时删除或替换。
- 业务当前时间统一通过 `DateUtils`，不要直接调用 `Instant.now()` / `LocalDateTime.now()`。
- 语言和时区必须通过 `LocaleUtils` / `TimeZoneUtils` 归一化。
- 服务间 HTTP 调用优先使用 `RemoteHttpClient` 的 `serviceId + path`，不要在业务代码里拼内部服务绝对 URL。
- 调用方身份、服务地址解析、调用治理和调用审计分别通过 `RemoteCallerResolver`、`ServiceEndpointResolver` / `ServiceEndpointRegistry`、`RemoteCallPolicy`、`RemoteCallObserver` 扩展。

## 前端约束

- 页面和组件不要直接拼接口 URL，也不要直接写 `url: '/api/...'`。
- 运行接口优先使用 `openApiRequest(operationId)`；尚未接入 OpenAPI 的兼容接口集中在 `ApiPaths` / `ApiLegacyPaths`。
- 页面和组件不要直接依赖 `@/contracts/openapi/**`；生成物只允许 API model 和 API runtime helper 消费。
- 页面和组件不要直接读浏览器语言、直接写 `Intl.DateTimeFormat` 或散落 `toLocale*`；统一走 `utils/locale` 和 `utils/time`。
- HTTP 错误展示、`requestId`、`traceId`、语言和时区请求头都由 `utils/http` 统一处理。
- 新增 API 类型放在 `src/api/model/**`，优先从 OpenAPI operation 类型派生，再做前端兼容字段适配。
- 旧模板兼容接口只能留在明确的兼容 API 模块，不能被新页面或新运行接口继续扩散。

## UI 约束

- UI 默认遵守 `project_document/UI_DESIGN_GUIDE.md`。
- 新页面保持极简、清晰、少文字，次级说明优先 hover、tooltip、popover 或详情承载。
- 表面优先复用全局玻璃 token，边界优先 dashed，不在局部组件散落新的玻璃变量。
- 页面区域不套娃卡片，不用 decorative orb、bokeh blob 或大面积单色渐变制造装饰感。

## 契约与生成

- `contracts/platform-contract.json` 是平台级事实来源，覆盖 API 前缀、响应 envelope、分页字段、请求头、时间、语言和错误码分段。
- `contracts/service-boundaries.json` 是服务边界事实来源，覆盖 basePath、owner、currentHost、runtime route 和未来拆分方向。
- OpenAPI JSON 是运行接口事实来源；前端生成 `schemas.ts`、`operations.ts`，并通过 `openApiRequest` 消费 operation 类型。
- 生成文件不要手改，修改 manifest 或后端接口后运行对应 generator，并执行 `--check`。

## 文档与门禁

- 任何新增母版能力必须更新相关文档和 `project_document/STATUS.md`。
- 任何新增约束优先进入脚本门禁，不要只写在文档里。
- 发布或作为新项目母版前必须运行 `./scripts/quality-gate.sh`。
- 复制项目验证必须保持可用，`./scripts/smoke-copy.sh` 不通过时不能认为母版健康。

## 当前脚本守护

- `./scripts/check-contracts.sh`: 聚合路径、响应、上下文、时间、远程调用、OpenAPI、共享内核等关键契约。
- `./scripts/check-template.sh`: 守住母版文档、示例边界、复制资产和项目身份。
- `node scripts/check-backend-controller-contracts.js`: 守住后端非示例 Controller 的响应和 DTO/VO 边界。
- `node scripts/check-scaffold-governance.js`: 守住 CI、根入口文档、新模块指南、接入提示词、UI 设计基线、演示证据和前端 legacy API 使用边界。
- `node scripts/check-frontend-openapi-boundaries.js`: 守住前端 OpenAPI 生成物的导入边界。
- `./scripts/quality-gate.sh`: 作为母版提交前的总门禁。
