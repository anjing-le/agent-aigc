# Status

更新时间：2026-06-12

`agent-aigc` 当前定位为 AIGC 创作平台。工程骨架已对齐 `infra-dev-scaffolding`，业务主线围绕“用户一句话输入 -> Agent 意图分析 -> Prompt 优化 -> Provider 路由 -> 异步生成任务 -> 创作资产沉淀”展开。

## 当前阶段

| 维度 | 状态 | 说明 |
| --- | --- | --- |
| 工程规范 | Ready | 已继承脚手架的前后端目录、统一 API 响应、OpenAPI、服务边界、请求上下文和质量门禁 |
| AIGC 主链路 | V0.9 in progress | 已具备创作任务、意图识别、Prompt 优化、Provider 路由、任务轮询、资产保存，并开始支持显式类型、参数提示和参考素材上传 |
| 本地演示 | Ready | 默认使用 mock provider，无 Google/OneRouter Key 也能端到端创建作品 |
| 真实模型接入 | In progress | Google GenAI 和 OneRouter 路径保留，通过环境变量切换 provider 与 key |
| 作品管理 | V0.8 | 支持资产列表、发布到广场、广场按类型/模型/关键词筛选 |
| 素材库 | V0.9 | 已提供上传 API、素材记录、列表/删除 API 和前端素材库；后续补引用关系、清理和权限 |

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
- 创作输入支持自动识别、图片、视频、音频模式切换，并可传入宽高比、尺寸、时长、音色等轻量参数。
- Agent 决策展示已补充清洗 Prompt、参数摘要和置信度；历史记录支持复用 Prompt，结果区支持重新生成回填。
- 模型列表已从后端 ProviderRouter 派生，创作页展示可用模型能力，并新增只读模型配置页。
- AIGC 本地文件保存已收口到 `LocalAigcStorageService`，Google 图片/视频/音频 provider 不再各自硬编码上传目录和访问前缀。
- 新增 AIGC 素材上传与列表 API，前端创作台引用参考图时不再传 base64，素材库页面可预览、筛选和复制素材 URL。

## 当前判断

当前项目已经适合做到 “V1.0 可演示创作 MVP”：

1. 本地无需外部模型密钥即可完整演示产品流程。
2. 接入真实 Google/OneRouter Key 后，可以逐步替换 mock provider。
3. 工程规范、质量门禁和脚手架契约已经足够支撑继续迭代。

不建议当前阶段扩成复杂平台。优先做深一句话创作、作品资产、参数可解释和 provider 可切换，再进入工作流、团队协作、计费和模型市场。

## 风险与缺口

- 视频/音频 mock 当前是可视化占位，不是真实媒体文件。
- 素材库已具备上传、记录、列表、预览和删除入口，但创作引用关系、存储清理和权限隔离仍待产品化。
- Provider 配置已有只读状态页，写配置能力仍主要通过环境变量切换。
- 历史记录和作品广场缺少用户维度、收藏、点赞、复用 Prompt 等产品能力。
- 真实模型调用的错误码、限流、重试和成本统计需要继续产品化。

## 推荐下一步

1. 做 V1 创作闭环：模型配置页、参数面板、Prompt 优化解释、失败重试。
2. 做素材库：引用关系、存储清理、本地/OSS 存储抽象。
3. 做作品资产：发布、复制 Prompt、重新生成、下载、删除、筛选。
4. 做 Provider 管理：可用性探测、默认 provider、模型参数模板、成本字段。
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
