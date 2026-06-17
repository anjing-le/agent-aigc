# Lecture Script

本文档是讲师可直接照着使用的课堂讲稿。`TEACHING_GUIDE.md` 说明怎么组织课程，本文件说明每一步讲什么、展示哪里、截图留什么证据。

## 30 分钟演示版

适合给第一次接触脚手架的人快速建立整体感觉。

### 0-3 分钟：开场

讲师说：

> 今天看的不是一个从零堆出来的 AIGC Demo，而是一个从 `infra-dev-scaffolding` 生长出来的业务项目。底层技术栈、接口契约、请求上下文、OpenAPI、质量门禁都由脚手架提供，我们只关注 AIGC 业务怎么设计。

展示：

- `README.md`
- `project_document/SCAFFOLD_INHERITANCE_MAP.md`

强调：

- 学技术栈不是重点，理解边界才是重点。
- 以后换 CRM、知识库、Agent 平台，也应该先看脚手架继承点，再看业务增量。

### 3-8 分钟：看工程底座

讲师说：

> 一个业务接口不是从 Controller 开始写，而是先进入契约。这里的源头是 `contracts/service-boundaries.json`，前后端路径、OpenAPI、脚本检查都会围绕它工作。

展示：

- `contracts/service-boundaries.json`
- `backend/src/main/java/com/anjing/model/constants/ApiConstants.java`
- `frontend/src/api/openapiClient.ts`
- `scripts/check-contracts.sh`

强调：

- API 路径集中管理。
- 响应 envelope 统一。
- 前端页面不直接拼 `/api/aigc`。
- 脚本会守住这些习惯。

### 8-15 分钟：拆 AIGC 业务

讲师说：

> AIGC 的复杂度不是页面多，而是链路长。用户一句话进来，要经过 Agent 判断、Prompt 优化、Provider 路由、异步任务、资产沉淀和公开传播。

展示：

- `backend/src/main/java/com/anjing/aigc/agent`
- `backend/src/main/java/com/anjing/aigc/provider`
- `backend/src/main/java/com/anjing/aigc/service`
- `frontend/src/views/aigc`

强调：

- `agent` 不做存储。
- `provider` 不关心页面。
- `service` 承载流程。
- `controller` 只是 API 边界。
- 页面只处理体验，不绕过 API 模块。

### 15-24 分钟：跑业务闭环

讲师操作：

1. 打开 `/aigc/studio`。
2. 输入一句话，例如：`生成一张蓝色科技感课程封面图`。
3. 查看 Agent 决策和生成结果。
4. 到 `/aigc/assets` 查看资产并发布。
5. 到 `/aigc/gallery` 打开作品。
6. 到 `/share/gallery/:assetId` 查看公开分享页。
7. 点击“复用 Prompt”。
8. 到 `/aigc/gallery-report` 查看分享转化漏斗。
9. 到 `/aigc/models` 查看 Provider 调用报表。

讲师说：

> 这一步的重点不是生成图片多漂亮，而是看业务闭环：创作、资产、公开、传播、复用、运营报表和 Provider 观测都串起来了。

### 24-28 分钟：看一次接口生长

讲师说：

> Provider 调用报表是一个很适合教学的功能：它不改变创作主流程，却穿过了完整工程链路。

展示：

- `ApiConstants.Aigc.MODEL_PROVIDER_EXECUTION_REPORT`
- `contracts/service-boundaries.json` 里的 `modelProviderExecutionReport`
- `AigcController.getProviderExecutionReport`
- `AigcTaskRepository.findVisibleForExecutionReport`
- `ProviderExecutionReportResponse`
- `frontend/src/api/aigc.ts`
- `frontend/src/views/aigc/models/index.vue`

强调：

- 查询先遵守当前 user/tenant 可见性。
- 报表从任务执行事实聚合。
- 前端通过 typed API 调用。
- 契约脚本保证链路没有绕开脚手架。

### 28-30 分钟：收尾

讲师执行或展示：

```bash
./scripts/check-contracts.sh
```

讲师说：

> 一个业务项目可教学，不是因为代码很多，而是因为它的生长方式稳定。脚手架解决习惯，业务只负责设计。

## 60 分钟课堂版

适合边讲边让学员操作。

| 时间 | 内容 | 学员动作 |
| --- | --- | --- |
| 0-5 分钟 | 项目定位和脚手架继承 | 打开 README 和继承地图 |
| 5-15 分钟 | 工程底座和契约链路 | 找出 service boundary、ApiConstants、OpenAPI client |
| 15-25 分钟 | AIGC 后端模块拆解 | 画出 Agent、Provider、Task、Asset、Gallery 的关系 |
| 25-35 分钟 | AIGC 前端页面拆解 | 逐个打开 studio、assets、gallery、report、models |
| 35-45 分钟 | 跑通创作到分享和 Provider 观测闭环 | 生成作品、发布、分享、复用、看互动报表和调用报表 |
| 45-55 分钟 | 设计一个新功能 | 以“作品合集”为例写 service boundary 和页面规划 |
| 55-60 分钟 | 质量门禁和总结 | 运行 `check-contracts`，回顾哪些来自脚手架 |

## 截图清单

截图建议保存在本机或教学材料仓库，不建议直接提交大图到当前代码仓库。

本版截图素材包索引见 `docs/evidence/2026-06-17/README.md`。

| 编号 | 页面或文件 | 截图内容 | 说明 |
| --- | --- | --- | --- |
| 01 | `README.md` | 项目定位和文档入口 | 开场使用 |
| 02 | `SCAFFOLD_INHERITANCE_MAP.md` | 继承关系表 | 说明底座和业务增量 |
| 03 | `contracts/service-boundaries.json` | `aigc` boundary | 说明接口源头 |
| 04 | `/aigc/studio` | 创作工作台输入区和 Agent 决策 | 展示一句话创作 |
| 05 | `/aigc/assets` | 资产列表和发布动作 | 展示资产沉淀 |
| 06 | `/aigc/gallery` | 灵感广场卡片 | 展示公开传播 |
| 07 | `/share/gallery/:assetId` | 分享页和复用 Prompt 按钮 | 展示公开页面 |
| 08 | `/aigc/gallery-report` | 分享转化漏斗 | 展示运营闭环 |
| 09 | `/aigc/models` | Provider 调用报表 | 展示模型调度观测 |
| 10 | `scripts/check-contracts.sh` | 检查通过输出 | 展示质量门禁 |
| 11 | `project_document/DEMO_EVIDENCE.md` | 命令证据表 | 展示交付证据 |

## 截图命名建议

```text
agent-aigc-teaching/
  01-readme-positioning.png
  02-scaffold-inheritance-map.png
  03-service-boundary-aigc.png
  04-studio-agent-decision.png
  05-assets-publication.png
  06-gallery-card.png
  07-share-page-reuse-prompt.png
  08-gallery-report-funnel.png
  09-model-provider-execution-report.png
  10-contract-check.png
  11-demo-evidence.png
```

## 课堂练习答案参考

### 练习 1：一个新 API 应该怎么加？

参考答案：

1. 在 `ApiConstants.Aigc` 加路径常量。
2. 在 `contracts/service-boundaries.json` 声明 route。
3. 在 Controller 暴露 API，返回 `APIResponse<T>`。
4. 在 Service 实现业务，不把逻辑塞进 Controller。
5. 通过 OpenAPI 生成前端 operation/schema。
6. 在 `frontend/src/api/aigc.ts` 封装调用。
7. 页面只调用 API 模块。
8. 运行 `./scripts/check-contracts.sh`。

### 练习 2：为什么互动报表从审计表聚合？

参考答案：

报表要回答的是“发生过什么”，而不是“现在对象状态是什么”。点赞、下载、分享访问、Prompt 复用都是行为事件，进入审计后可以按时间、动作、内容类型、作品和作者聚合，也能继承脚手架 request context。

### 练习 3：为什么 Provider 调用报表从任务表聚合？

参考答案：

Provider 调用报表关心的是生成任务执行事实：Provider、模型、状态、耗时、成本状态和归属上下文都已经沉淀在 `aigc_task`。它先按当前 user/tenant 可见性过滤，再按 Provider、模型和内容类型聚合，既能支撑管理台观测，也不会绕开脚手架权限边界。

### 练习 4：为什么保留 mock provider？

参考答案：

Mock provider 保证教学和本地开发不依赖外部 Key。真实模型只需要替换 Provider 边界，任务、资产、广场和报表都不需要改一遍。

## 讲师结束语

> `agent-aigc` 的价值不是它一次做完所有 AIGC 平台能力，而是它展示了一种可复用的生长方式。脚手架负责工程习惯，业务负责领域设计。下一个项目只要换业务边界，底层方法仍然一样。
