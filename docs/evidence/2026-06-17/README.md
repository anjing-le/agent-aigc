# agent-aigc Teaching Screenshot Pack

本目录用于记录 2026-06-17 这一版教学截图素材包的采集清单。截图文件建议放在课件仓库或本机素材目录，当前代码仓库只保留索引和采集说明。

## 基线

- 功能基线：`e0fc2bfdccfe17dab118d1b93f76fb32043fdfa6`
- 远端分支：`main` / `master`
- 作者：`安静 <245548353+anjing-le@users.noreply.github.com>`
- 验证：`./scripts/check-contracts.sh`、`mvn -q test`、`pnpm build` 已通过；发布前再跑 `./scripts/quality-gate.sh`

## 环境准备

后端：

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev SERVER_PORT=18180 mvn -q spring-boot:run
```

前端：

```bash
cd frontend
VITE_API_PROXY_URL=http://127.0.0.1:18180 pnpm dev --host 127.0.0.1 --port 5174
```

截图前先使用默认 mock provider 跑通一次演示资产：

1. 打开 `http://127.0.0.1:5174/#/aigc/studio`。
2. 输入 `生成一张蓝色科技感课程封面图` 并创建任务。
3. 任务完成后进入资产页发布作品。
4. 打开分享页，点击“复用 Prompt”。
5. 打开互动报表确认分享转化漏斗有数据。
6. 打开模型配置页，确认 Provider 调用报表能看到任务、成功率、耗时和成本状态。

## 截图清单

| 文件名 | 视口 | 页面或文件 | 重点 |
| --- | --- | --- | --- |
| `01-readme-positioning.png` | 桌面 | `README.md` | 项目定位、脚手架来源、文档入口 |
| `02-scaffold-inheritance-map.png` | 桌面 | `project_document/SCAFFOLD_INHERITANCE_MAP.md` | 底座能力和 AIGC 增量对照表 |
| `03-service-boundary-aigc.png` | 桌面 | `contracts/service-boundaries.json` | `aigc` boundary 和 route 声明 |
| `04-studio-agent-decision.png` | 1440x900 | `/aigc/studio` | 一句话输入、Agent 决策、生成结果 |
| `05-assets-publication.png` | 1440x900 | `/aigc/assets` | 资产沉淀、来源任务、发布动作 |
| `06-gallery-card.png` | 1440x900 | `/aigc/gallery` | 已发布作品、点赞/收藏/下载/分享 |
| `07-share-page-reuse-prompt.png` | 390x844 | `/share/gallery/:assetId` | 公开分享页、复用 Prompt 按钮 |
| `08-gallery-report-funnel.png` | 1440x900 | `/aigc/gallery-report` | 分享访问、公开下载、Prompt 复用漏斗 |
| `09-model-provider-execution-report.png` | 1440x900 | `/aigc/models` | Provider 调用报表、模型配置、审计入口 |
| `10-mobile-model-report.png` | 390x844 | `/aigc/models` | 移动端报表统计和表格无页面级横向溢出 |
| `11-mobile-report-funnel.png` | 390x844 | `/aigc/gallery-report` | 移动端统计卡和漏斗无横向溢出 |
| `12-contract-check.png` | 终端 | `./scripts/check-contracts.sh` | 契约检查通过 |
| `13-quality-gate.png` | 终端 | `./scripts/quality-gate.sh` | 完整质量门禁通过 |
| `14-demo-evidence.png` | 桌面 | `project_document/DEMO_EVIDENCE.md` | 命令证据和浏览器检查结果 |

## 截图规范

- 不截取真实 API Key、Cookie、Token、数据库密码或云存储密钥。
- 浏览器地址栏可保留 localhost，但不要出现个人目录路径。
- 终端截图只保留命令和通过结果，不展示环境变量值。
- 页面截图优先保留完整业务上下文，避免只截局部按钮。
- 移动端截图使用 390x844，桌面端建议使用 1440x900。
- PNG 不建议直接提交到当前仓库；如需归档，放入课件素材仓库或对象存储。

## 讲解搭配

- `01` 到 `03` 用于说明“为什么先继承脚手架”。
- `04` 到 `09` 用于说明“业务如何从创作走到传播、运营和 Provider 观测”。
- `10` 和 `11` 用于说明“页面响应式也属于验收范围”。
- `12` 和 `13` 用于说明“质量门禁保证业务没有背离底座”。
- `14` 用于说明“交付不是口头完成，而是有可追溯证据”。
