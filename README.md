# agent-aigc

基于 `infra-dev-scaffolding` 生长出来的 AIGC 创作平台。

底层工程规范、质量门禁、前后端分层和契约习惯来自脚手架；本项目只把注意力放在 AIGC 业务设计上：多模型调度、Prompt 优化链、文生图/音频/视频、素材库、资产库和灵感广场。

## 能力边界

- 创作工作台：一句话输入、参考素材、参数提示、任务进度和生成结果。
- Agent 链路：意图识别、Prompt 清洗/增强、内容类型判断和模型路由。
- Provider：Mock 本地演示、Google / OneRouter 等真实模型扩展点。
- 创作资产：素材库、我的资产、灵感广场、Prompt 复用闭环。
- 工程契约：统一响应、OpenAPI 类型、服务边界、请求上下文和质量门禁。

## 技术栈

- 后端：Spring Boot 3、JPA、H2/MySQL、Redis/Local Lock、OpenAPI
- 前端：Vue 3、TypeScript、Vite、Element Plus、pnpm
- 模型：Google GenAI、OneRouter 等 Provider 可扩展

## 运行

默认使用 mock provider，本地不配置模型 Key 也能跑通创作任务。

```bash
cd backend
mvn spring-boot:run
```

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

## 文档入口

- `project_document/STATUS.md`：当前状态。
- `project_document/ACCEPTANCE_CHECKLIST.md`：V1 验收清单。
- `project_document/TEACHING_GUIDE.md`：教学讲解顺序和课堂任务。
- `project_document/ROADMAP.md`：阶段规划。
- `project_document/SCAFFOLD_INHERITANCE_MAP.md`：脚手架继承地图。
- `project_document/LOCAL_STARTUP_GUIDE.md`：本地启动。
- `project_document/SCAFFOLD_ADOPTION_PROMPT.md`：如何复用脚手架改造其他项目。

## License

MIT
