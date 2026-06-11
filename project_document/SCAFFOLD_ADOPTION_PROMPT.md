# Scaffold Adoption Prompt

本文档提供一段可复制给 Codex / Cursor / 其他 AI 编程助手的接入提示词，用于让旧项目按 `infra-dev-scaffolding` 的工程母版逐步重构，或让新项目初始化时直接继承本母版的边界、契约和质量门禁。

## 使用方式

1. 打开需要改造的目标项目。
2. 复制下面的提示词给 AI 编程助手。
3. 按项目实际情况补充目标项目名、路径、端口和保留业务范围。
4. 让 AI 先做审计和迁移计划，再分批落地，不要一次性大改。

## 可复制提示词

```text
你是一个资深全栈工程协作者。请基于 Anjing 工程母版重构或初始化当前项目。

母版仓库：
https://github.com/anjing-le/infra-dev-scaffolding

目标：
- 让当前项目逐步对齐 Anjing 的全栈工程母版。
- 保留当前项目已有业务价值，不要为了套模板而重写业务。
- 优先建立清晰、简单、可验证、可长期迭代的工程结构。

请先阅读或参考母版中的这些文档：
- README.md
- project_document/PROJECT_CONSTRAINTS.md
- project_document/NEW_MODULE_GUIDE.md
- project_document/UI_DESIGN_GUIDE.md
- project_document/API_CONTRACT_GUIDE.md
- project_document/OPENAPI_CONTRACT_GUIDE.md
- project_document/SERVICE_BOUNDARY_GUIDE.md
- project_document/PLATFORM_CONTRACT_GUIDE.md
- project_document/COPY_GUIDE.md
- project_document/RELEASE_CHECKLIST.md

当前项目上下文：
- 项目名称：[填写目标项目名]
- 当前项目路径：[填写本地路径]
- 前端技术栈：[填写或让 AI 检查]
- 后端技术栈：[填写或让 AI 检查]
- 希望保留的业务模块：[填写]
- 可以替换或删除的旧模板/旧页面：[填写]
- 期望端口：frontend [填写]，backend [填写]

工作方式：
1. 先审计当前项目结构、启动方式、前后端技术栈、API 层、响应结构、路由、环境变量、文档和质量脚本。
2. 对照 infra-dev-scaffolding，列出差距：能直接复用的、需要迁移的、应该保留的、应该删除的。
3. 给出分阶段迁移计划。每个阶段必须能独立验证，不要一次性重构所有模块。
4. 优先落地工程契约，再迁移业务页面：
   - 统一 API 路径管理。
   - 统一响应 envelope：code/message/data/timestamp/requestId。
   - 分页 payload 使用 records/current/size/total。
   - 前端 API model 与 API 调用分层。
   - 运行接口优先接 OpenAPI 类型生成或等价 typed client。
   - 请求上下文保留 requestId、traceId、语言和时区。
   - 时间、语言和错误处理收口到统一工具。
   - UI 遵守极简、虚线、轻玻璃、少文字和 hover 承载次级信息。
5. 不把重型平台能力默认塞进项目：
   - 不默认引入网关、注册中心、MQ、对象存储、链路追踪平台。
   - 只保留接口、配置样例和扩展点。
6. 修改代码时遵守当前项目已有风格；如果和母版冲突，先说明权衡，再做小步迁移。
7. 每一批改动后运行可用的验证命令，并更新 README / 项目文档 / 迁移记录。

输出要求：
- 先给当前项目审计结论。
- 再给迁移阶段计划。
- 然后开始实现第一阶段。
- 每次改动保持小而可验证。
- 不要删除用户已有改动，不要为了模板一致性破坏已有业务。
```

## 适用场景

- 从旧 Vue / Spring Boot 项目迁移到 Anjing 工程规范。
- 新建 Infra / Agent 项目时快速建立工程边界。
- 让外部 Codex 在不了解本仓库历史的情况下，按同一套文档和质量门禁工作。

## 不适用场景

- 只想复制 UI 皮肤，不想采用工程约束。
- 需要一次性重写所有业务。
- 目标项目没有前后端工程结构，也不准备建立质量门禁。
