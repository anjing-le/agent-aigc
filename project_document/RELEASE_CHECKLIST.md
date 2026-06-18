# Release Checklist

用于每次发布、复制为新项目之前的检查。

## 必过命令

完整质量门禁：

```bash
./scripts/quality-gate.sh
```

V1 教学交付总验收：

```bash
./scripts/v1-teaching-acceptance.sh
```

母版自检：

```bash
./scripts/check-template.sh
./scripts/check-contracts.sh
node scripts/check-scaffold-governance.js
./scripts/smoke-copy.sh
./scripts/probe-backend-dev.sh
```

前端：

```bash
cd frontend
pnpm build
```

后端：

```bash
cd backend
mvn -q -DskipTests package
```

## 前端检查

- 登录页能正常打开。
- 登录页符合 `project_document/UI_DESIGN_GUIDE.md` 的轻玻璃、虚线和少文字基线。
- “游客访问”能进入 `/dashboard/console`。
- 游客菜单只展示允许游客访问的页面。
- 主题切换、语言切换、响应式布局不出现明显破版。
- 生产构建不出现 TypeScript 错误。
- `.env`、`.env.development`、`.env.production` 不包含真实密钥。

## 后端检查

- `backend/.env.example` 中的变量名和 `application.yml` 对应。
- 数据库连接、Druid 账号、Redis 配置都可通过环境变量覆盖。
- 健康检查、登录示例、测试 CRUD 接口能按文档验证。
- `/v3/api-docs` 在 dev/test 可返回 OpenAPI 3 JSON，prod 默认关闭。
- 日志目录、上传目录、target 产物不会被提交。

## 文档检查

- README 的技术栈、端口、启动命令和验证命令准确。
- `project_document/ROADMAP.md` 的阶段状态和当前优先级没有过期。
- `project_document/STATUS.md` 的验证证据与最近一次命令结果一致。
- `project_document/NEW_MODULE_GUIDE.md`、`project_document/UI_DESIGN_GUIDE.md` 和 `project_document/DEMO_EVIDENCE.md` 没有过期。
- `project_document/TEMPLATE_BOUNDARIES.md` 中的保留/删除边界与当前目录一致。
- `project_document/AI_ASSETS.md` 中的 Rules / Prompts 数量与 `.cursor` 目录一致。
- “复制为新项目”的改名点清楚列出。

## 开源发布检查

- 根目录存在 `LICENSE`。
- 根目录存在 `CONTRIBUTING.md`。
- 保留 `frontend/LICENSE` 中的上游许可说明。
- 没有真实 API Key、数据库密码、SSH 密钥、云存储密钥。
- 没有本地日志、上传文件、构建产物进入提交。
- 没有依赖个人机器路径的说明或配置。
- README 能让第一次打开项目的人理解它为什么存在。
- 按 `project_document/DEMO_EVIDENCE.md` 保留必要截图、后端 probe 和质量门禁记录。
