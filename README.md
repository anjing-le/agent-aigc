# agent-aigc

基于 `infra-dev-scaffolding` 生长出来的 AIGC 创作平台示例。

学习重点：底层技术栈、目录习惯、服务边界、OpenAPI、统一响应、请求上下文和质量门禁都继承脚手架；业务层只关注 AIGC 设计。

## 业务能力

- 一句话创作：意图识别、Prompt 清洗/增强、模型路由、异步任务。
- 多模态生成：图片、视频、音频，默认 mock provider 可本地演示。
- 创作资产：素材库、我的资产、发布/撤回、下载/预览。
- 灵感广场：公开作品、动态合集、人工运营专题、榜单、点赞/收藏、分享页、Prompt 复用。
- 运营观测：互动报表、分享转化漏斗、Provider 调用报表。

## 运行

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
- `project_document/ACCEPTANCE_CHECKLIST.md`
- `project_document/TEACHING_GUIDE.md`
- `project_document/SCAFFOLD_INHERITANCE_MAP.md`
- `project_document/DEMO_EVIDENCE.md`

## License

MIT
