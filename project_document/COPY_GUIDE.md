# Copy Guide

将本仓库复制为新项目时，按下面顺序处理。

## 1. 复制目录

```bash
cp -r infra-dev-scaffolding my-new-project
cd my-new-project
```

复制后先删除旧构建产物和本地日志，再初始化新的 Git 仓库或切换到目标仓库。

## 2. 修改项目标识

前端：

- `frontend/package.json`：修改 `name`。
- `frontend/.env`：修改 `VITE_PORT`、`VITE_VERSION`、`VITE_LOCK_ENCRYPT_KEY`。
- `frontend/.env.development`：修改 `VITE_API_PROXY_URL`。
- `frontend/.env.production`：修改生产 API 地址。
- `frontend/src/config` 或品牌相关文件：替换系统名、Logo 和文案。

后端：

- `backend/pom.xml`：修改 `artifactId`、`name`、`description`。
- `backend/src/main/resources/application.yml`：修改 `spring.application.name`、`server.port`、默认数据库名。
- `backend/src/main/resources/application-dev.yml` / `application-test.yml` / `application-prod.yml`：按新项目确认 profile 默认开关。
- `backend/.env.example`：同步数据库 URL、账号变量说明和 Druid 默认说明。
- `backend/src/main/resources/logback-spring.xml`：确认日志文件名和 app id。

文档：

- `README.md`：替换项目名、定位、端口和快速开始说明。
- `project_document/ROADMAP.md`：替换路线图和非目标。
- `project_document/PROJECT_CONSTRAINTS.md`：保留母版约束，按新项目裁剪非目标。
- `project_document/NEW_MODULE_GUIDE.md`：作为新业务模块的默认生成和验收入口。
- `project_document/UI_DESIGN_GUIDE.md`：按新项目品牌微调，但保留极简、虚线和可读玻璃基线。
- `project_document/DEMO_EVIDENCE.md`：记录复制后的本地截图、后端 probe 和质量门禁证据。

## 3. 调整示例与菜单

- 保留登录、游客模式、工作台、主题、结果页、异常页作为基础体验。
- 按新项目需要决定是否保留系统管理。
- 参考 `project_document/TEMPLATE_BOUNDARIES.md` 判断母版能力、示例能力和旧残留。
- 在 `frontend/` 下运行 `pnpm clean:dev` 查看旧残留；确认后再运行 `pnpm clean:dev -- --apply`。
- 后端用 `@ScaffoldSample` 标记示例代码；真实业务代码不要放在带有该标记的包或类中。
- 在 `frontend/src/router/modules/` 中新增业务路由。
- 在 `backend/src/main/java/com/anjing/example/` 之外新增业务包，避免把示例代码当成业务代码。

## 4. 配置本地环境

前端：

```bash
cd frontend
pnpm install
pnpm dev
```

后端：

```bash
cd backend
mvn spring-boot:run
```

默认使用 `SPRING_PROFILES_ACTIVE=dev` 和内存 H2，可选中间件保持轻启动。需要连接 MySQL 或验证 profile 矩阵时参考 `project_document/ENVIRONMENT_PROFILE_GUIDE.md`。

## 5. 验证

```bash
./scripts/check-template.sh
node scripts/check-scaffold-governance.js
```

这条命令会检查前端包名、后端 `artifactId`、`spring.application.name` 是否一致，并确认保留下来的示例代码带有边界标记。

发布母版前可以运行复制烟测，确认“复制 + 改名 + 自检”链路可用：

```bash
./scripts/smoke-copy.sh
```

```bash
cd frontend
pnpm build
```

```bash
cd backend
mvn -q -DskipTests package
```

## 6. 开源前检查

- 删除 `frontend/dist`、`backend/target`、`backend/logs`、上传文件和本地缓存。
- 检查 `.env`、`.env.*`、`application.yml`、`.env.example` 没有真实密钥。
- 确认 README 的命令能被第一次打开项目的人直接执行。
