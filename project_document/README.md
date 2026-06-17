# agent-aigc 项目文档

本目录存放 agent-aigc 的工程契约、AIGC 业务边界、启动说明和质量门禁记录。

阅读顺序建议：先看项目状态，再看业务路线；如果要学习“如何从脚手架长出一个业务项目”，再看脚手架采用提示词和工程契约文档。

## 项目主线

- `STATUS.md`：当前状态、已完成能力、风险和下一步
- `ACCEPTANCE_CHECKLIST.md`：V1 教学演示验收清单
- `ROADMAP.md`：V0.8 到 V1.2 的业务演进路线
- `LOCAL_STARTUP_GUIDE.md`：本地启动和验证流程
- `DEMO_EVIDENCE.md`：演示证据和验证记录
- `TEACHING_GUIDE.md`：教学讲解顺序、课堂任务和讲师检查清单

## 脚手架继承层

- `PROJECT_CONSTRAINTS.md`：项目约束和重构边界
- `SCAFFOLD_INHERITANCE_MAP.md`：脚手架继承地图，用于教学说明哪些来自底座、哪些是 AIGC 增量
- `API_CONTRACT_GUIDE.md`：统一响应、分页和错误码契约
- `API_PATH_GUIDE.md`：前后端 API 路径常量规范
- `PLATFORM_CONTRACT_GUIDE.md`：平台级契约来源和生成规则
- `SERVICE_BOUNDARY_GUIDE.md`：服务边界与 OpenAPI 路由声明
- `OPENAPI_CONTRACT_GUIDE.md`：OpenAPI 生成和运行时校验
- `REMOTE_CALL_GUIDE.md`：跨服务调用、请求头透传和调用方识别
- `ENVIRONMENT_PROFILE_GUIDE.md`：开发、测试、生产配置约定
- `SHARED_KERNEL_GUIDE.md`：共享内核与业务模块边界

## AIGC 业务层

- `FEATURE_STATUS_GUIDE.md`：功能可用性标记规范
- `AI_ASSETS.md`：AIGC 资源和素材说明
- `UI_DESIGN_GUIDE.md`：后台页面和 AIGC 页面设计约束
- `SCAFFOLD_ADOPTION_PROMPT.md`：复用 `infra-dev-scaffolding` 改造其他项目的提示词

## 质量门禁

常用入口：

```bash
./scripts/check-contracts.sh
./scripts/probe-backend-dev.sh
./scripts/quality-gate.sh
```

这些门禁来自 `infra-dev-scaffolding`，agent-aigc 的业务迭代必须继续遵守。
