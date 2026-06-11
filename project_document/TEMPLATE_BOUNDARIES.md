# Template Boundaries

本文档用于说明哪些内容属于工程母版，哪些内容只是演示或示例。复制为新项目时，先按这里判断保留、替换或删除。

## 母版能力

这些内容默认应保留：

- `frontend/src/components/core/`：布局、菜单、设置面板、表格、结果页、异常页等通用组件。
- `frontend/src/router/`：静态路由、动态路由、权限过滤和菜单生成。
- `frontend/src/store/`：用户、菜单、设置、标签页等基础状态。
- `frontend/src/api/` 和 `frontend/src/types/`：API 分层、统一响应类型和业务模型位置。
- `frontend/src/views/auth/`：登录、注册、忘记密码页面。实际项目可以替换实现，但保留路由结构。
- `frontend/src/views/dashboard/console/`：默认工作台，也是游客模式落点。复制后可以替换页面内容。
- `frontend/src/views/result/`、`frontend/src/views/exception/`、`frontend/src/views/theme/`：通用体验页面。
- `backend/src/main/java/com/anjing/config/`：Web、Jackson、JPA、Redis、锁、中间件和配置属性。
- `backend/src/main/java/com/anjing/model/`：请求、响应、错误码、异常、校验和常量。
- `backend/src/main/java/com/anjing/aspect/`、`annotation/`、`util/`、`statemachine/`：可复用基础设施能力。
- `backend/src/main/java/com/anjing/controller/AuthController.java`：Mock 认证示例。真实项目接入认证中心或数据库后可替换。
- `.cursor/rules/` 和 `.cursor/prompts/`：AI 协作规范和代码生成入口。
- `contracts/service-boundaries.json`：服务/模块边界、API 归属和未来拆分计划。
- `project_document/`：规划、边界、复制、发布检查文档。
- `project_document/PROJECT_CONSTRAINTS.md`、`NEW_MODULE_GUIDE.md`、`UI_DESIGN_GUIDE.md`、`DEMO_EVIDENCE.md`：长期约束、新模块、UI 和演示证据入口。
- `project_document/ci/quality-gate.yml`：GitHub 上复用本地 `./scripts/quality-gate.sh` 的自动门禁模板。

## 示例能力

这些内容用于演示和自检，不应直接变成新项目业务代码：

- `backend/src/main/java/com/anjing/controller/TestController.java`：健康检查、异常处理、内存 CRUD 演示，使用 `@ScaffoldSample` 标记。
- `backend/src/main/java/com/anjing/example/`：远程调用、校验、状态机使用示例，包上使用 `@ScaffoldSample` 标记。
- `frontend/src/views/dashboard/console/modules/`：工作台展示数据和卡片内容。
- `frontend/src/mock/upgrade/changeLog.ts`：升级提示演示数据。

复制为业务项目时，建议先保留这些示例跑通首轮验证，再按业务边界删除或替换。

## 可选能力

这些内容是否保留取决于新项目类型：

- `frontend/src/views/system/` 和 `frontend/src/router/modules/system.ts`：后台管理能力。管理类项目建议保留，纯展示或单功能工具可删除。
- `frontend/src/views/theme/` 和 `frontend/src/router/modules/theme.ts`：主题预览页。需要演示主题能力时保留，生产项目可隐藏或删除。
- `backend/src/main/java/com/anjing/statemachine/`：状态机基础能力。存在明确状态流转业务时保留，否则可以删除。
- Redis / Redisson / 分布式锁：按 `backend/src/main/resources/application.yml` 的 `app.features` 开关决定。

## 不应存在的旧残留

这些内容来自历史模板或旧实验，如果出现应删除：

- `frontend/src/views/auth/login111/`
- `frontend/src/config/fastEnter.ts`
- `frontend/src/views/change/`
- `frontend/src/views/article/`
- `frontend/src/views/examples/`
- `frontend/src/views/widgets/`
- `frontend/src/views/template/`
- `frontend/src/views/safeguard/`
- 指向 Apifox mock 的生产 API 地址。
- 旧的 Agent Dev Scaffolding 项目名。

可在 `frontend/` 下运行以下命令检查和清理前端旧残留：

```bash
pnpm clean:dev
pnpm clean:dev -- --apply
```

第一条只报告，不删除；第二条才会删除命中的旧残留。

## 复制策略

1. 先保持母版完整，按 README 启动前端、后端并跑构建。
2. 修改项目名、端口、数据库名、品牌文案和文档定位。
3. 删除或替换“示例能力”，但不要删除统一响应、异常、权限、路由、环境配置这些母版能力。
4. 新业务代码放在新的业务包和页面目录中，不要写进 `backend/src/main/java/com/anjing/example/`。
5. 清理完后运行 `./scripts/check-template.sh`、`pnpm build`、`mvn -q -DskipTests package`。

复制项目如果删除或替换接口边界，需要同步更新 `contracts/service-boundaries.json`，再运行：

```bash
node scripts/check-service-boundaries.js
```

## 后端示例定位

`@ScaffoldSample` 是后端示例边界标记，不参与运行时逻辑。

- 母版中出现 `@ScaffoldSample` 是正常的，表示这些代码用于示例、自检或演示。
- 新业务代码不应使用 `@ScaffoldSample`。
- 如果复制项目删除了 `TestController` 和 `example/`，可以保留或删除 `ScaffoldSample` 注解类。
- 如果复制项目保留了示例代码，`./scripts/check-template.sh` 会检查这些示例是否仍带有边界标记。
