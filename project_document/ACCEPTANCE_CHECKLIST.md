# Acceptance Checklist

用于确认 `agent-aigc` 已经作为“从 `infra-dev-scaffolding` 生长出来的 AIGC 业务项目”达到 V1 教学演示标准。

## 当前验收状态

截至 2026-06-18，项目完成度约 **94%**，满足 V1 教学演示候选标准。

- 已满足：脚手架技术栈对齐、前后端契约、AIGC 创作闭环、素材/资产/广场/分享/报表、Provider 管理、存储治理、人工运营专题、创作者榜单、运营规则说明/配置和质量门禁。
- 仍可增强：真实 Provider 生产级计费、KMS 托管、运营规则权重/投放策略引擎、更多真实模型演示证据。
- 当前必跑总门禁：`./scripts/quality-gate.sh`。
- 教学交付总验收：`./scripts/v1-teaching-acceptance.sh`。

## 1. 脚手架继承验收

- 后端仍是 Spring Boot 3、JPA、OpenAPI、统一异常、统一响应和 `APIResponse/PageResult`。
- 前端仍是 Vue 3、TypeScript、Vite、Element Plus、统一 HTTP client 和 OpenAPI 派生类型。
- AIGC API 全部声明在 `ApiConstants.Aigc` 和 `contracts/service-boundaries.json`。
- 前端 AIGC 调用全部经 `frontend/src/api/aigc.ts`、`openApiRequest` 和生成类型，不在页面手写 `/api/aigc`。
- 请求上下文、requestId、traceId、userId、tenantId、locale、timeZone 继续由脚手架链路传递。
- 业务新增后必须通过 `./scripts/check-contracts.sh`，证明没有绕开底座规范。

## 2. AIGC 业务闭环验收

- 创作工作台支持一句话输入、内容类型提示、参数提示、参考素材和任务进度轮询。
- Agent 链路能展示意图识别、Prompt 清洗、Prompt 增强、模型路由和参数摘要。
- Mock provider 在无外部 Key 时可完成图片/视频/音频演示任务。
- 生成任务完成后能沉淀资产，并在资产详情里回看来源任务和 Agent 快照。
- 资产可发布到灵感广场，也可从广场撤回。
- 灵感广场支持筛选、运营规则说明/配置、动态精选合集、人工运营专题、全局热门榜单、公开创作者榜单、点赞、收藏、我的收藏、公开预览、公开下载和 Prompt 复用。
- 公开分享页支持 SEO 元数据、海报、复制链接、下载作品和复用 Prompt 回创作台。
- 互动报表支持动作分布、内容类型分布、每日趋势、高互动作品、创作者对比、作品互动结构和分享转化漏斗。
- Provider 调用报表支持按当前上下文查看任务数、成功率、耗时、成本状态，以及 Provider/模型维度聚合。

## 3. 页面验收

| 页面 | 验收点 |
| --- | --- |
| `/aigc/studio` | 可创建任务、查看 Agent 决策、轮询结果、复用历史 Prompt |
| `/aigc/materials` | 可上传/筛选/预览/删除素材，并查看引用任务 |
| `/aigc/assets` | 可查看资产、来源任务、存储状态，支持下载/预览/发布/撤回/删除 |
| `/aigc/gallery` | 可浏览公开作品、筛选、运营规则说明/配置、动态精选合集、人工运营专题、全局热门榜单、创作者榜单、点赞、收藏、下载、复制分享入口 |
| `/aigc/gallery-report` | 可查看互动报表、分享转化漏斗、作者/作品对比并导出 CSV |
| `/aigc/models` | 可探测 Provider、切换 active provider、管理凭证/参数、查看审计和调用报表 |
| `/share/gallery/:assetId` | 未登录可访问已发布作品，复用 Prompt 会记录 `prompt-reuse` |
| `/share/creators/:authorId` | 可查看公开作者作品、Top 作品和互动统计 |

## 4. 后端模块验收

- `agent`：只处理意图、Prompt 和路由决策，不直接做存储和 HTTP 响应。
- `provider`：封装 Mock、Google、OneRouter 等外部模型差异。
- `service`：承载任务、素材、资产、广场、存储、Provider 管理和审计业务。
- `controller`：只做 API 边界和统一响应，不写复杂业务。
- `repository`：只做实体持久化和聚合查询，不承载跨模块流程。
- `model/request/response/dto/entity/enums`：保持 AIGC 领域对象清晰分层。

## 5. 数据与审计验收

- 任务、素材、资产写入 owner/user/tenant 上下文，并兼容历史空归属演示数据。
- Provider 管理审计不记录密钥明文。
- 存储审计记录上传、删除和清理结果。
- 广场审计记录发布、撤回、点赞、收藏、公开下载、分享访问和 Prompt 复用。
- 互动报表和 Provider 调用报表聚合只读取当前上下文可见数据。

## 6. 必跑验证

```bash
cd backend
mvn -q test
```

```bash
cd frontend
pnpm build
```

```bash
./scripts/check-contracts.sh
./scripts/probe-backend-dev.sh
```

业务闭环冒烟：

```bash
./scripts/aigc-demo-smoke.sh http://127.0.0.1:10003
```

该脚本会覆盖生成、发布、分享、公开下载、Prompt 复用、互动报表、Provider 调用报表、灵感广场动态作品合集、人工运营专题、创作者榜单、运营规则说明和运营规则配置保存。

完整发布前再跑：

```bash
./scripts/quality-gate.sh
```

教学交付冻结前再跑：

```bash
./scripts/v1-teaching-acceptance.sh
```

## 7. 演示路径

1. 启动后端和前端，使用默认 mock provider。
2. 在创作工作台输入一句话生成图片。
3. 查看任务进度、Agent 决策和生成结果。
4. 进入我的资产，查看来源任务并发布到灵感广场。
5. 在灵感广场查看并保存运营规则配置，再查看动态精选合集、人工专题和创作者榜单；打开分享页，复制链接、下载作品、复用 Prompt。
6. 回到互动报表，确认 `share-view`、`public-download`、`prompt-reuse` 和分享转化漏斗出现。
7. 回到模型配置，确认 Provider 调用报表出现任务数、成功率和模型指标。
8. 运行质量门禁，说明业务是沿脚手架契约生长出来的。

## 8. 交付判断

- 能讲清楚“脚手架给底座，agent-aigc 只做 AIGC 业务设计”。
- 能从一个 AIGC API 反查到 `ApiConstants`、`service-boundaries.json`、OpenAPI 类型和前端 API 模块。
- 能通过 mock provider 演示完整创作链路，不依赖外部 Key。
- 能通过 `quality-gate` 证明构建、契约、OpenAPI 运行探针和前端构建没有退化。

## 9. V1 非目标

- 不做复杂工作流编排。
- 不做团队协作、套餐计费和模型市场。
- 不要求视频/音频 mock 具备真实媒体生成能力。
- 不把所有 Provider 做到生产级 SLA；V1 重点是业务边界、工程习惯和可演示闭环。
