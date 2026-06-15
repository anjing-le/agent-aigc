# Status

更新时间：2026-06-15

`agent-aigc` 当前定位为 AIGC 创作平台。工程骨架已对齐 `infra-dev-scaffolding`，业务主线围绕“用户一句话输入 -> Agent 意图分析 -> Prompt 优化 -> Provider 路由 -> 异步生成任务 -> 创作资产沉淀”展开。

## 当前阶段

| 维度 | 状态 | 说明 |
| --- | --- | --- |
| 工程规范 | Ready | 已继承脚手架的前后端目录、统一 API 响应、OpenAPI、服务边界、请求上下文和质量门禁 |
| AIGC 主链路 | V0.9 in progress | 已具备创作任务、意图识别、Prompt 优化、Provider 路由、任务轮询、失败重试、资产保存，并开始支持显式类型、参数提示和参考素材上传 |
| 本地演示 | Ready | 默认使用 mock provider，无 Google/OneRouter Key 也能端到端创建作品 |
| 真实模型接入 | In progress | Google GenAI 和 OneRouter 路径保留，支持环境变量配置、页面运行时切换 active provider、加密托管凭证、默认参数模板、角色边界和管理审计 |
| 作品管理 | V0.9 | 已完成资产管理页、详情溯源、发布到广场、下载、删除、Prompt 复用、卡片/表格视图和存储状态展示 |
| 素材库 | V0.9 | 已提供上传 API、素材记录、列表/删除 API、前端素材库、任务引用 ID 记录、结果区引用展示、基础引用统计、引用任务预览和文件清理；后续补权限隔离 |
| 前端体验 | V0.9 | 工作台、素材库、我的资产、灵感广场、模型配置已统一到脚手架后台页面范式，并形成 Prompt 复用闭环 |

## 已完成

- README 已精简为 agent-aigc 的启动与质量门禁入口。
- 后端已拆分 AIGC 领域包：agent、provider、service、controller、entity、repository、DTO。
- `IntentAnalyzer` 支持 OneRouter 智能解析；无配置时使用规则降级，保证本地可用。
- `PromptEnhancer` 清理临时代码，保留图片/视频/音频的基础增强规则。
- `AigcTaskExecutor` 独立承载异步生成，避免同类自调用导致 `@Async` 失效。
- `MockImageProvider`、`MockVideoProvider`、`MockAudioProvider` 支持本地无 Key 演示。
- 任务完成时回写 assetId、resultUrl、thumbnailUrl、durationMs 等结果字段。
- 画廊查询支持 contentType、model、keyword 过滤。
- 前端创作工坊支持任务提交、进度轮询、Agent 决策展示、生成结果预览、历史记录和作品广场入口。
- 失败任务支持从工作台一键重试；重试接口继续复用创作主链路和 service-boundary 契约。
- 资产下载逻辑已收口为前端统一工具，创作结果、历史记录和我的资产页复用同一套下载行为；我的资产页支持发布到灵感广场。
- 资产详情接口和弹窗已联动来源任务，可回看任务状态、Prompt 和参考素材。
- 生成任务会持久化 Agent 分析快照，资产详情可回看清洗 Prompt、优化 Prompt、置信度和参数摘要。
- 任务状态新增 Provider 调用观测摘要，创作台和资产详情可查看实际 Provider、模型、耗时、成本统计状态和估算金额。
- Provider 成本估算 V1 已接入：任务完成/失败后写入 `costStatus`、估算金额、币种、单位和说明；mock provider 固定 `USD 0`，Google 单任务估算成本通过 `aigc.cost.google.*` 配置，不在代码中硬编码官方价格。
- 创作输入支持自动识别、图片、视频、音频模式切换，并可传入宽高比、尺寸、时长、音色等轻量参数。
- Agent 参数覆盖层已加入生成参数白名单与范围校验，非法类型、比例、尺寸、时长、BPM 会返回 AIGC 业务错误码；任务失败状态会保留 errorCode 并在前端失败态展示。
- Agent 决策展示已补充清洗 Prompt、参数摘要和置信度；历史记录支持复用 Prompt，结果区支持重新生成回填。
- 模型列表已从后端 ProviderRouter 派生，创作页展示可用模型能力，并新增只读模型配置页。
- 模型配置页已升级为 Provider 配置视图，可展示 active provider、可用性、配置模型、缺失配置和默认参数模板。
- Provider 配置页已接入运行前探测接口，按 service-boundary、ApiConstants、明确 DTO/VO、OpenAPI 生成类型和 `openApiRequest` 贯通脚手架契约；探测结果已拆成注册、路由、凭证、可用性、模型、参数、成本和生成就绪检查项，便于真实图片 Provider 上线前逐项验证。
- Provider 显式 smoke test V1 已接入：模型配置页可手动触发图片 Provider 最小生成测试，Google Provider 必须二次确认 `confirmExternalCall=true` 才会触发外部调用；成功后保存测试资产和来源任务，并写入 `smoke-test` 管理审计。
- Provider 配置页已支持持久化切换 active provider：页面调用 `/api/aigc/models/active-provider`，后端写入 `aigc_provider_route_config`，列表返回 `routeConfigSource` 区分环境配置和页面保存。
- Provider 密钥边界已收口：模型配置接口只返回配置状态、缺失说明和来源，不返回 API Key；启动日志只输出密钥存在性和长度。
- Provider 凭证管理 V1 已接入：页面通过 `/api/aigc/models/provider-credential` 只写式保存 Google 凭证，后端写入 `aigc_provider_credential_config`，Google 图片/视频/音频 Provider 运行时优先读取数据库凭证，再回退环境配置；响应只返回 `credentialSource`、`credentialStorageMode`、状态和更新时间。
- Provider 凭证加密 V1 已接入：`AigcProviderCredentialCodec` 使用 `aigc.security.credential-master-key` 做 AES-GCM 静态加密，数据库新写入值使用 `enc:v1` 格式，旧明文记录可继续读取并标识为 `legacy-database`，后续可在 codec 边界替换为 KMS。
- Provider 参数模板 V1 已接入：页面通过 `/api/aigc/models/provider-params` 保存 Google 图片/视频/音频默认参数，后端写入 `aigc_provider_param_config`，模型列表返回 `paramConfigSource` 和更新时间；Google 图片宽高比/尺寸、视频比例/清晰度/时长、音频默认音色运行时优先读取页面配置，再回退环境配置。
- Provider 管理审计 V1 已接入：页面通过 `/api/aigc/models/provider-audits` 查看最近变更，后端写入 `aigc_provider_audit_log`，覆盖 active provider 切换、只写式凭证保存和参数模板保存；审计记录继承脚手架 `GlobalRequestContextHolder` 的 requestId、traceId、tenantId、userId、callerId 和客户端 IP，凭证审计只记录来源和状态，不记录明文。
- Provider 管理权限 V1 已接入：Provider 路由切换、凭证保存和默认参数模板保存统一经过 `AigcProviderManagementPermissionService`，默认要求 `R_SUPER` 或 `R_ADMIN`；前端统一 HTTP client 通过平台 `REQUEST_HEADERS` 透传 userId、userName、userRoles，拒绝操作会写入 `permission-denied` 审计。
- AIGC 文件保存已收口到 `AigcStorageService` adapter 边界，素材上传、资产删除和 Google 图片/视频/音频 provider 不再直接依赖本地存储实现。
- AIGC OSS adapter V1 已接入：`OssAigcStorageService` 使用 AWS SDK v2 S3-compatible 客户端，支持 endpoint、region、bucket、CDN、objectKeyPrefix、path-style 和 public-read 配置；OSS 配置完整时素材上传、Provider 结果保存和资产删除会切到 OSS adapter，默认 dev/test 仍走本地存储。
- AIGC 存储治理 V2 已接入：OSS 上传/删除支持 `retry-count` 与 `retry-interval-ms` 短重试；统一存储边界会把上传、按文件删除和按 URL 删除写入 `aigc_storage_audit_log`，审计继承脚手架 requestId、traceId、tenantId、operator、callerId 和 clientIp；存储状态接口新增 retry、cleanup audit、signed URL 配置可见性，不暴露任何密钥。
- AIGC 存储状态诊断 V1 已接入：新增 `/api/aigc/storage/status`，按 service-boundary、ApiConstants、OpenAPI 派生类型和 `openApiRequest` 贯通前后端；资产页展示 activeMode、本地目录可读写、清理能力、URL 前缀和 OSS 配置状态，响应不暴露任何密钥字段。
- Google 图片 Provider 已加入 429/5xx 短重试、统一 Provider 调用错误码和本地保存失败兜底。
- 删除资产时会清理本地生成文件和缩略图；清理失败只记录 warning，不阻断资产记录删除。
- 新增 AIGC 素材上传与列表 API，前端创作台引用参考图时不再传 base64，素材库页面可预览、筛选、删除、复制素材 URL 并显示引用次数，任务状态与结果区可回看引用素材。
- V1 参考素材策略已收口：图片创作仅支持图片素材，视频创作支持图片/视频素材，音频创作暂不支持视觉素材；后端生成入口和前端创作台均已校验。
- AIGC 前端页面已按脚手架后台体验收口：工作台有任务态和模型能力摘要，素材库/资产库有统计、搜索、筛选、卡片/表格视图，灵感广场有数据源状态、统计和 Prompt 筛选。
- Prompt 复用闭环已打通：灵感广场和资产详情可携带 prompt/contentType 跳回创作工作台，工作台会自动回填输入框并切换对应内容类型模式。

## 当前判断

当前项目已经适合做到 “V1.0 可演示创作 MVP”：

1. 本地无需外部模型密钥即可完整演示产品流程。
2. 接入真实 Google/OneRouter Key 后，可以逐步替换 mock provider。
3. 工程规范、质量门禁和脚手架契约已经足够支撑继续迭代。

不建议当前阶段扩成复杂平台。优先做深一句话创作、作品资产、参数可解释和 provider 可切换，再进入工作流、团队协作、计费和模型市场。

## 风险与缺口

- 视频/音频 mock 当前是可视化占位，不是真实媒体文件。
- 素材库和资产库已具备统一 storage adapter、OSS SDK 上传/删除、本地文件清理、存储状态诊断、OSS 短重试和清理审计；文件权限隔离、私有桶签名访问和审计查询页仍待产品化。
- Provider 配置已有运行前探测检查项、显式 smoke test、页面持久化切换、配置来源展示、只写式加密凭证保存、默认参数模板编辑、角色边界、管理审计和密钥脱敏边界；生产级 KMS 托管和批量测试策略仍需要后续管理能力承接。
- 历史记录和作品广场缺少用户维度、收藏、点赞等产品能力。
- 真实模型调用已有参数错误码、Provider 可用性/调用错误码、图片 Provider 短重试、任务重试入口、基础 Provider 调用观测和配置化成本估算；真实用量计费和聚合报表仍需继续产品化。

## 推荐下一步

1. 做存储产品化：文件权限隔离、私有桶签名访问、审计查询页和历史文件归属迁移。
2. 做 Provider 管理：KMS 托管替换、本地 legacy 凭证升级任务、真实用量计费和细粒度权限。
3. 做真实调用报表：按 Provider、模型、内容类型聚合成功率、耗时和估算成本。
4. 做用户维度：资产、素材、广场发布、收藏和点赞的用户隔离。
5. 做教学材料：以 agent-aigc 作为从脚手架生长业务项目的案例，沉淀模块边界和页面流程。

## 工程契约门禁

agent-aigc 继续继承脚手架的底座守卫，关键验证入口保留：

```bash
node scripts/check-backend-context-contract.js
node scripts/check-async-context-contract.js
mvn -q -Dtest=RequestContextTaskDecoratorTest test
node scripts/check-openapi-contract.js
node scripts/check-service-boundaries.js
curl http://localhost:10003/v3/api-docs
```

服务边界仍以 `contracts/service-boundaries.json` 为准，前后端路径从该契约派生并由脚本校验。
