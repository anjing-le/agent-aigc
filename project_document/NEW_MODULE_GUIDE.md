# New Module Guide

本文档定义从母版新增一个业务模块时的最小步骤。它服务于人和 AI 协作：先定契约，再写实现；先能验证，再扩展体验。

## 适用范围

- 适用于复制项目后的真实业务模块。
- 母版仓库只保留可复用模板、示例和规则，不长期沉淀具体业务领域能力。
- 纯演示模块应放在临时复制项目或独立示例仓库中，进入母版时必须标记为可删除示例。

## 后端清单

1. 在 service boundary 中确认 API 归属。如果是运行接口，先更新 `contracts/service-boundaries.json`，再生成前后端边界常量。
2. Controller 路径引用 `ApiConstants` 或生成的 service boundary 常量，不直接写 `"/api/..."`。
3. 非示例 Controller 返回 `APIResponse<T>`；分页返回 `APIResponse<PageResult<T>>`。
4. 请求使用明确 DTO，响应使用明确 VO；不要用 `Map<String, Object>` 承载真实业务请求或响应。
5. 输入 DTO 使用 Bean Validation；OpenAPI 使用 `@Tag`、`@Operation`、`@Schema` 描述运行契约。
6. 业务当前时间通过 `DateUtils`，语言/时区通过 `LocaleUtils`、`TimeZoneUtils` 归一化。
7. 服务间 HTTP 调用走 `RemoteHttpClient` 的 `serviceId + path`，调用方身份、治理和审计使用已有扩展点。
8. 错误码落在 `contracts/platform-contract.json` 声明的分段内，并实现 `ErrorCode`。

## 前端清单

1. 运行接口优先从 OpenAPI 生成 `schemas.ts` 和 `operations.ts`。
2. API model 放在 `frontend/src/api/model/**`，优先从 `OpenApiOperationRequest<T>` 和 `OpenApiOperationData<T>` 派生。
3. API 文件优先使用 `openApiRequest(operationId)`；页面和组件不直接依赖 `@/contracts/openapi/**`。
4. 尚未接入后端运行契约的旧模板接口只能留在 `ApiLegacyPaths`，并且只允许兼容 API 文件调用。
5. 页面组件不直接拼 `/api/...`，不直接读浏览器语言，不散落 `Intl.DateTimeFormat` 或 `toLocale*`。
6. 页面文案保持克制，次级说明优先通过 tooltip、hover、空状态或详情弹层承载。
7. 复杂页面先抽 API、model、composable，再落视图；不要让业务判断散落在 template。

## 最小交付顺序

1. 更新 service boundary 或确认模块仍属于旧模板兼容层。
2. 写后端 DTO / VO / Controller / Service。
3. 启动后端获取 `/v3/api-docs`，运行 OpenAPI 前端类型生成。
4. 写 `src/api/model/**` 和 `src/api/**`，运行接口使用 `openApiRequest`。
5. 写页面、路由和权限声明。
6. 补必要测试或 smoke 验证。
7. 更新文档和 `project_document/STATUS.md`。
8. 执行 `./scripts/quality-gate.sh`。

## AI Prompt 约束

给 AI 生成新模块时，Prompt 必须说明：

- 后端不要用 `Map` 作为真实 DTO / VO。
- Controller 返回 `APIResponse<T>`，分页 payload 使用 `PageResult<T>`。
- 前端 API 类型优先从 OpenAPI operation 派生。
- 页面不直接导入 OpenAPI 生成目录。
- 新增约束要进入脚本门禁，而不是只写文档。

## 示例边界

- `@ScaffoldSample` 标记的 Controller 或 package 是母版示例。
- 复制项目后，示例可以删除、替换或作为 Prompt 参考。
- 示例不能反向污染运行模块规则；真实模块必须通过同一套契约脚本。
