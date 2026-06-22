# agent-aigc V1 Delivery Evidence

本目录记录 2026-06-22 的 V1 教学交付检查。它只保留可追溯结论，不提交截图、密钥、构建产物或本地私有资料。

## 基线

- 目标：确认 `agent-aigc` 已按 `infra-dev-scaffolding` 的技术栈和工程习惯完成 V1 教学交付。
- 当前完成度：约 `96%`。
- Git author：`安静 <245548353+anjing-le@users.noreply.github.com>`。
- 重点闭环：创作、资产、素材、灵感广场、公开分享、运营规则配置、规则配置影响数据返回、互动报表、Provider 调用报表。

## 必看入口

- `README.md`：最短启动和验收入口。
- `project_document/STATUS.md`：当前进度、已完成能力和剩余生产化增强。
- `project_document/TEACHING_GUIDE.md`：课堂讲解顺序。
- `project_document/SCAFFOLD_INHERITANCE_MAP.md`：脚手架继承关系。
- `project_document/DEMO_EVIDENCE.md`：总体验收证据。

## 验收命令

```bash
./scripts/check-contracts.sh
./scripts/quality-gate.sh
./scripts/v1-teaching-acceptance.sh
```

`v1-teaching-acceptance.sh` 会覆盖脚手架契约、后端构建、前端构建、runtime OpenAPI probe 和 AIGC 业务 smoke。

本次输出摘录：

```text
check-contracts: ok
quality-gate: ok
v1-teaching-acceptance: ok
v1-teaching-acceptance: verified=scaffold-contracts,backend-build,frontend-build,runtime-openapi,aigc-smoke
aigc-demo-smoke: gallery collections=3 matched=trending
aigc-demo-smoke: gallery topics=2 matched=course-cover
aigc-demo-smoke: gallery creators=1 matched=demo-smoke
aigc-demo-smoke: gallery curationRules=9 version=v1
aigc-demo-smoke: gallery curationConfig=database rule=trending maxSize=2
aigc-demo-smoke: gallery curationDataPlane=trending itemCount=1
```

## 交付判断

V1 已经适合用于教学：学习者可以只关注 AIGC 业务设计，底层技术栈、请求上下文、OpenAPI、服务边界、前端 typed API 和质量门禁都沿用脚手架。后续增强应放在生产级 KMS、真实计费、规则权重投放和真实模型演示证据。
