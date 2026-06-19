# agent-aigc

基于 `infra-dev-scaffolding` 的 AIGC 创作平台教学项目。

## 业务能力

- 创作：意图识别、Prompt 优化、多模型路由、异步生成。
- 资产：素材库、我的资产、发布/撤回、授权预览/下载。
- 广场：作品分享、运营规则配置、合集/专题/榜单、点赞/收藏、Prompt 复用。
- 观测：互动报表、分享漏斗、Provider 调用报表。

## 本地运行

默认使用 mock provider，无需模型 Key。

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

```bash
cd frontend
pnpm install
pnpm dev
```

## 验证

```bash
./scripts/check-contracts.sh
./scripts/aigc-demo-smoke.sh http://127.0.0.1:10003
./scripts/quality-gate.sh
./scripts/v1-teaching-acceptance.sh
```

## 文档

- `project_document/STATUS.md`
- `project_document/TEACHING_GUIDE.md`
- `project_document/SCAFFOLD_INHERITANCE_MAP.md`
- `project_document/DEMO_EVIDENCE.md`
