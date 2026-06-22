# agent-aigc

基于 `infra-dev-scaffolding` 重构的 AIGC 创作平台教学项目。

学习重点很简单：底层工程习惯、契约和质量门禁来自脚手架；这里只关注 AIGC 业务如何生长出来。

## 能力

- 创作：意图识别、Prompt 优化、多模型路由、异步生成。
- 资产：素材库、我的资产、发布/撤回、授权预览/下载。
- 广场：作品分享、运营规则配置、合集/专题/榜单、点赞/收藏、Prompt 复用。
- 观测：互动报表、分享漏斗、Provider 调用报表。

## 启动

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

## 验收

```bash
./scripts/check-contracts.sh
./scripts/quality-gate.sh
./scripts/v1-teaching-acceptance.sh
```

## 文档

- `project_document/STATUS.md`：当前进度和下一步
- `project_document/TEACHING_GUIDE.md`：课堂讲解顺序
- `project_document/SCAFFOLD_INHERITANCE_MAP.md`：脚手架继承关系
- `project_document/DEMO_EVIDENCE.md`：验收证据
