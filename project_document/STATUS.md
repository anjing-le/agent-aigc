# Status

更新时间：2026-06-12

`agent-aigc` 当前定位为 AIGC 创作平台。工程骨架已对齐 `infra-dev-scaffolding`，业务主线围绕“用户一句话输入 -> Agent 意图分析 -> Prompt 优化 -> Provider 路由 -> 异步生成任务 -> 创作资产沉淀”展开。

## 当前阶段

| 维度 | 状态 | 说明 |
| --- | --- | --- |
| 工程规范 | Ready | 已继承脚手架的前后端目录、统一 API 响应、OpenAPI、服务边界、请求上下文和质量门禁 |
| AIGC 主链路 | V0.9 in progress | 已具备创作任务、意图识别、Prompt 优化、Provider 路由、任务轮询、失败重试、资产保存，并开始支持显式类型、参数提示和参考素材上传 |
| 本地演示 | Ready | 默认使用 mock provider，无 Google/OneRouter Key 也能端到端创建作品 |
| 真实模型接入 | In progress | Google GenAI 和 OneRouter 路径保留，通过环境变量切换 provider 与 key |
| 作品管理 | V0.8 | 支持资产列表、发布到广场、广场按类型/模型/关键词筛选 |
| 素材库 | V0.9 | 已提供上传 API、素材记录、列表/删除 API、前端素材库、任务引用 ID 记录、结果区引用展示、基础引用统计和本地文件清理；后续补 OSS 和权限 |

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
- 任务状态新增 Provider 调用观测摘要，创作台和资产详情可查看实际 Provider、模型、耗时和成本统计状态。
- 创作输入支持自动识别、图片、视频、音频模式切换，并可传入宽高比、尺寸、时长、音色等轻量参数。
- Agent 参数覆盖层已加入生成参数白名单与范围校验，非法类型、比例、尺寸、时长、BPM 会返回 AIGC 业务错误码；任务失败状态会保留 errorCode 并在前端失败态展示。
- Agent 决策展示已补充清洗 Prompt、参数摘要和置信度；历史记录支持复用 Prompt，结果区支持重新生成回填。
- 模型列表已从后端 ProviderRouter 派生，创作页展示可用模型能力，并新增只读模型配置页。
- 模型配置页已升级为 Provider 配置视图，可展示 active provider、可用性、配置模型、缺失配置和默认参数模板。
- Provider 配置页已接入运行前探测接口，按 service-boundary、ApiConstants、明确 DTO/VO、OpenAPI 生成类型和 `openApiRequest` 贯通脚手架契约。
- AIGC 本地文件保存已收口到 `LocalAigcStorageService`，Google 图片/视频/音频 provider 不再各自硬编码上传目录和访问前缀。
- Google 图片 Provider 已加入 429/5xx 短重试、统一 Provider 调用错误码和本地保存失败兜底。
- 删除资产时会清理本地生成文件和缩略图；清理失败只记录 warning，不阻断资产记录删除。
- 新增 AIGC 素材上传与列表 API，前端创作台引用参考图时不再传 base64，素材库页面可预览、筛选、删除、复制素材 URL 并显示引用次数，任务状态与结果区可回看引用素材。
- V1 参考素材策略已收口：图片创作仅支持图片素材，视频创作支持图片/视频素材，音频创作暂不支持视觉素材；后端生成入口和前端创作台均已校验。

## 当前判断

当前项目已经适合做到 “V1.0 可演示创作 MVP”：

1. 本地无需外部模型密钥即可完整演示产品流程。
2. 接入真实 Google/OneRouter Key 后，可以逐步替换 mock provider。
3. 工程规范、质量门禁和脚手架契约已经足够支撑继续迭代。

不建议当前阶段扩成复杂平台。优先做深一句话创作、作品资产、参数可解释和 provider 可切换，再进入工作流、团队协作、计费和模型市场。

## 风险与缺口

- 视频/音频 mock 当前是可视化占位，不是真实媒体文件。
- 素材库和资产库已具备本地文件清理；OSS 清理和权限隔离仍待产品化。
- Provider 配置已有只读配置视图和运行前探测，写配置能力仍主要通过环境变量切换。
- 历史记录和作品广场缺少用户维度、收藏、点赞等产品能力。
- 真实模型调用已有参数错误码、Provider 可用性/调用错误码、图片 Provider 短重试、任务重试入口和基础 Provider 调用观测；真实成本金额仍需继续产品化。

## 推荐下一步

1. 做 V1 创作闭环：模型配置页、参数面板、Prompt 优化解释、失败态体验。
2. 做素材库：OSS 存储抽象、素材与任务详情联动、权限隔离。
3. 做作品资产：发布、复制 Prompt、重新生成、下载、删除、筛选。
4. 做 Provider 管理：运行时切换默认 provider、真实成本金额。
5. 做真实模型验证：至少打通图片生成的一条生产级 provider 链路。

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
