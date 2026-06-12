# agent-aigc

全模态 AIGC 创作平台，核心是多模型调度、Prompt 优化链、文生图/音频/视频、创作历史和模型参数配置。

## 核心能力

- 一句话创作：识别意图、选择模型、优化 Prompt、创建生成任务
- 多模态生成：图片、音频、视频能力预留统一入口
- 创作资产：任务状态、作品资产、灵感广场、历史记录
- 工程契约：统一 API 响应、OpenAPI 类型、服务边界、请求上下文

## 技术栈

- 后端：Spring Boot 3、JPA、H2/MySQL、Redis/Local Lock、OpenAPI
- 前端：Vue 3、TypeScript、Vite、Element Plus、pnpm
- 模型：Google GenAI、OneRouter 等 Provider 可扩展

## 本地启动

默认使用 mock provider，本地不配置模型 Key 也能跑通创作任务。

后端默认 dev 端口：`10003`。

```bash
cd backend
mvn spring-boot:run
```

前端默认 dev 端口：`20003`。

```bash
cd frontend
pnpm install
pnpm dev
```

## 质量门禁

```bash
./scripts/check-contracts.sh
./scripts/probe-backend-dev.sh
./scripts/quality-gate.sh
```

更多工程约束见 `project_document/`。

## License

MIT
