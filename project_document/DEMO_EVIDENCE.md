# Demo Evidence

本文档记录 `agent-aigc` 作为 AIGC 教学项目的演示证据。目标不是堆材料，而是让“继承脚手架、跑通业务闭环、通过质量门禁”有可追溯凭据。

## 最新验收记录

- 日期：2026-06-17
- 功能基线：以 `main` 最新提交为准，本轮提交可通过 `git log -1 --pretty=fuller` 查看
- Git author：`安静 <245548353+anjing-le@users.noreply.github.com>`
- 结论：通过 V1 教学演示验收，当前已具备创作、资产、灵感广场动态合集、公开分享、互动报表和 Provider 调用报表的教学闭环。

## 命令证据

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `mvn -q -Dtest=AigcServiceImplAssetTest,AigcGalleryAuditLogServiceTest test` | passed | 覆盖分享访问、Prompt 复用和互动报表漏斗 |
| `mvn -q test` | passed | 后端全量单测 |
| `cd frontend && pnpm build` | passed | 前端类型检查和生产构建 |
| `./scripts/check-contracts.sh` | passed | 脚手架契约、服务边界、OpenAPI、上下文和 AIGC 专属守卫 |
| `./scripts/probe-backend-dev.sh 18181` | passed | dev profile runtime OpenAPI probe |
| `./scripts/aigc-demo-smoke.sh http://127.0.0.1:10003` | passed | 生成、发布、分享、下载、Prompt 复用、互动报表、Provider 调用报表和灵感广场动态作品合集闭环 |
| `./scripts/quality-gate.sh` | passed | 契约、后端 package、前端 build、后端 runtime probe 一次性通过 |

## 业务冒烟证据

本轮使用默认 mock provider 完成了一次公开分享转化闭环：

可直接运行：

```bash
./scripts/aigc-demo-smoke.sh http://127.0.0.1:10003
```

1. 调用 `/api/aigc/generate` 创建图片任务。
2. 轮询 `/api/aigc/task/{taskId}` 至 `COMPLETED`。
3. 调用 `/api/aigc/gallery/save` 发布资产。
4. 访问 `/api/aigc/gallery/{assetId}/share`，记录 `share-view`。
5. 调用 `/api/aigc/gallery/{assetId}/share/reuse`，记录 `prompt-reuse`。
6. 调用 `/api/aigc/gallery/{assetId}/download`，记录 `public-download`。
7. 查询 `/api/aigc/gallery/reports/interactions?days=1`，确认 `shareFunnel` 返回分享访问、公开下载、Prompt 复用和转化率。
8. 查询 `/api/aigc/models/provider-execution-report?days=7&contentType=IMAGE`，确认当前用户上下文下 Provider/模型调用指标可见。
9. 查询 `/api/aigc/gallery/collections?size=3`，确认 `trending`、`latest` 和内容类型合集从已发布作品动态聚合，并且新发布作品出现在合集资产里。

示例结果：

```json
{
  "shareViewCount": 2,
  "downloadCount": 1,
  "promptReuseCount": 2,
  "downloadRate": 50.0,
  "promptReuseRate": 100.0
}
```

Provider 调用报表示例结果：

```json
{
  "totalTasks": 1,
  "completedTasks": 1,
  "providerMetrics": [
    {
      "label": "Mock Image Provider",
      "successRate": 100.0,
      "costStatusSummary": "MOCK_FREE: 1"
    }
  ],
  "modelMetrics": [
    {
      "label": "mock-image-preview",
      "contentType": "IMAGE"
    }
  ]
}
```

动态作品合集示例结果：

```json
{
  "collectionCount": 3,
  "firstCollection": "trending",
  "firstCollectionItems": 1,
  "heatScore": 0
}
```

## 浏览器检查证据

- `/aigc/gallery-report` 桌面视口：分享转化漏斗、动作分布、每日趋势和对比表正常渲染，无页面级横向溢出，无 console error。
- `/aigc/gallery` 桌面视口：动态精选合集、全局热门榜单和公开作品列表正常渲染，无页面级横向溢出。
- `/aigc/gallery-report` 390px 移动视口：8 个统计项单列展示，漏斗面板存在，无页面级横向溢出。
- `/aigc/models` 桌面视口：Provider 调用报表、Provider 表现表和模型表现表正常渲染，无页面级横向溢出，无 console error。
- `/aigc/models` 390px 移动视口：报表统计两列展示，表格不造成页面级横向溢出。
- `/share/gallery/:assetId` 390px 移动视口：公开分享页可见，`复用 Prompt` 按钮可见，无页面级横向溢出，无 console error。
- 点击分享页 `复用 Prompt` 后跳转 `/aigc/studio?prompt=...&contentType=IMAGE`，报表中 `prompt-reuse` 计数增加。

## 教学讲解顺序

1. 先讲 `infra-dev-scaffolding` 继承点：目录、统一响应、OpenAPI、服务边界、请求上下文和质量门禁。
2. 再讲 AIGC 业务增量：Agent 链路、Provider 路由、任务、素材、资产和灵感广场。
3. 然后讲 API 生长方式：`ApiConstants` -> `service-boundaries.json` -> Controller/Service/DTO -> OpenAPI 类型 -> `openApiRequest`。
4. 最后讲验证闭环：单测、构建、契约脚本、runtime probe、API 冒烟、Provider 调用报表和浏览器检查。

## 后续可补证据

- 按 `docs/evidence/2026-06-17/README.md` 录制一次完整演示视频或保存关键截图。
- 在真实 Google Key 环境下补一次图片 Provider smoke test 证据。
- 为作者主页、动态作品合集和榜单补产品化截图。

## 不提交内容

- 真实密钥、Cookie、Token。
- 本地个人路径截图。
- 上传文件、构建产物、后端 target、前端 dist。
