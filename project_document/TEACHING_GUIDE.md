# Teaching Guide

本文档用于把 `agent-aigc` 讲成一个“从 `infra-dev-scaffolding` 生长出来的业务项目”。学习者不需要先背技术栈，只需要观察：底座怎么规定工程习惯，AIGC 业务如何在这些习惯上继续生长。

## 课程目标

完成本案例后，学习者应能说明：

- 为什么先继承脚手架，再设计业务模块。
- 一个 AIGC 项目如何拆成 Agent、Provider、Task、Asset、Material、Gallery、Report。
- 新增接口为什么必须经过 `ApiConstants`、`service-boundaries.json`、OpenAPI 类型和前端 API 模块。
- 为什么页面只关心体验，底层请求、响应、上下文、错误和时间都交给脚手架。
- 如何用测试、构建、契约脚本和 runtime probe 证明改动没有破坏工程底座。
- 为什么当前项目已经达到约 92% 的 V1 教学演示候选完成度，剩余工作主要是生产化增强。

## 推荐课时

| 课时 | 主题 | 产出 |
| --- | --- | --- |
| 1 | 认识脚手架底座 | 能画出后端、前端、contracts、scripts 的关系 |
| 2 | 拆解 AIGC 业务边界 | 能解释 Agent、Provider、任务、资产和广场的职责 |
| 3 | 新增一个接口的生长路径 | 能从 service boundary 走到前端 typed API |
| 4 | 完整演示业务闭环 | 能跑通创作、发布、分享、复用和报表 |
| 5 | 质量门禁和交付 | 能运行门禁并用证据说明项目可交付 |

## 讲解顺序

### 1. 先看底座

从这些文件开始：

- `README.md`
- `project_document/SCAFFOLD_INHERITANCE_MAP.md`
- `contracts/service-boundaries.json`
- `scripts/check-contracts.sh`
- `backend/src/main/java/com/anjing/model/response/APIResponse.java`
- `frontend/src/api/openapiClient.ts`

讲清楚一句话：工程习惯来自脚手架，AIGC 只在业务边界里生长。

### 2. 再看业务模块

后端从 `backend/src/main/java/com/anjing/aigc` 进入：

- `agent`：分析用户意图，清洗和增强 Prompt。
- `provider`：屏蔽 Mock、Google、OneRouter 等模型差异。
- `service`：承载任务、素材、资产、广场、存储、报表和审计。
- `controller`：只作为 API 边界，统一返回脚手架响应。
- `repository`：持久化和聚合查询。

前端从 `frontend/src/views/aigc` 进入：

- `studio`：创作工作台。
- `materials`：素材库。
- `assets`：我的资产。
- `gallery`：灵感广场。
- `gallery-report`：互动报表。
- `models`：Provider 配置。

### 3. 讲一条接口如何生长

以公开分享转化漏斗为例：

1. 后端在 `ApiConstants.Aigc` 声明 `/gallery/{assetId}/share/reuse`。
2. 在 `contracts/service-boundaries.json` 声明 `galleryShareReuse`。
3. Controller 暴露 `recordGallerySharePromptReuse`，返回 `APIResponse<Void>`。
4. Service 查已发布资产，并写入 `prompt-reuse` 审计。
5. OpenAPI 生成前端 operation 和 schema。
6. `frontend/src/api/aigc.ts` 封装 `fetchRecordGallerySharePromptReuse`。
7. 分享页点击“复用 Prompt”时调用 API，再跳转创作台。
8. 报表页读取 `shareFunnel`，展示分享访问、下载和复用转化。

这条线最适合讲“业务功能不是孤立页面按钮，而是从契约、服务、数据、前端和报表一起长出来”。

## 课堂任务

### 任务 A：找出脚手架继承点

要求学习者在 15 分钟内指出：

- 后端统一响应在哪里。
- 前端 typed API 从哪里来。
- 服务边界源文件在哪里。
- 请求上下文如何进入审计记录。
- 哪个脚本能防止页面手写 `/api/aigc`。

验收：能用 `SCAFFOLD_INHERITANCE_MAP.md` 解释每个答案。

### 任务 B：跑通一次 AIGC 创作闭环

操作路径：

1. 启动后端和前端。
2. 在 `/aigc/studio` 输入一句话生成图片。
3. 查看 Agent 决策和生成结果。
4. 在 `/aigc/assets` 发布作品。
5. 在 `/share/gallery/:assetId` 打开分享页。
6. 点击复用 Prompt。
7. 在 `/aigc/gallery-report` 查看漏斗数据。

验收：报表里能看到 `share-view`、`public-download` 或 `prompt-reuse`。

### 任务 C：设计一个新业务能力

建议题目：给 Provider 配置页增加“批量 smoke test 设计”。

要求只写设计，不急着编码：

- 后端模块放在哪里。
- API 常量和 service boundary 怎么命名。
- DTO/VO 怎么拆。
- 前端页面入口在哪里。
- 需要哪些审计和报表指标。
- 要跑哪些校验命令。

验收：设计必须能沿用脚手架路径，不允许页面直接拼 URL 或绕过 OpenAPI 类型。

## 演示脚本

开场：

> 这个项目不是从零搭框架，而是从 `infra-dev-scaffolding` 长出一个 AIGC 创作业务。我们今天只关注设计，底层技术栈、契约和质量门禁都已经由脚手架给出。

第一段：

> 先看 contracts 和 scripts。这里决定了接口怎么声明、前后端怎么同步、哪些行为会被脚本拦住。

第二段：

> 再看 AIGC 业务。用户一句话输入后，Agent 判断意图，Provider 负责模型差异，Task 记录异步状态，Asset 沉淀作品，Gallery 负责公开传播。

第三段：

> 最后看分享转化漏斗。它不是单独做一张图，而是分享页访问、Prompt 复用、公开下载都进入审计，再由报表聚合出来。

收尾：

> 当我们跑过 `check-contracts`、`probe-backend-dev` 和 `quality-gate`，就能证明这个业务没有背离脚手架。以后换成别的 Agent 项目，也只需要重新设计业务模块。

## 讲师检查清单

- 讲解前确认 `git status --short --branch` 只有已知未跟踪参考文件。
- 演示前确认后端默认 mock provider 可用。
- 演示时不要展示真实 Provider Key。
- 截图时避开本地个人路径、Cookie、Token。
- 每完成一段功能讲解，都回到脚手架继承点说明它为什么可复制。
- 结尾运行 `./scripts/quality-gate.sh` 或展示 `project_document/DEMO_EVIDENCE.md` 的最新结果。
- 收尾时明确当前完成度约 92%，V1 已可教学演示，生产级计费/KMS/运营后台配置化仍是后续增强。

## 学员常见问题

**为什么不是直接写业务接口？**

因为教学目标不是做一次能跑的页面，而是做一个可复制、可验证、可长期维护的业务项目。接口先进入契约，后续前端类型、文档、runtime probe 和质量脚本才能一起工作。

**Mock provider 会不会让项目不真实？**

Mock provider 是本地教学能力，不是生产模型能力。它保证没有外部 Key 时仍能演示完整创作闭环；真实 Provider 通过同一套 Provider 边界替换。

**为什么要做分享转化漏斗？**

因为 AIGC 创作平台不只生成内容，还要证明内容如何传播、被下载、被复用。这个漏斗刚好连接公开分享页、下载、Prompt 复用和后台报表。

**下一个项目怎么复用？**

复制 `SCAFFOLD_ADOPTION_PROMPT.md` 的提示词，先审计目标项目，再把业务模块按同样方式接入 service boundary、OpenAPI、前端 API model 和质量门禁。
