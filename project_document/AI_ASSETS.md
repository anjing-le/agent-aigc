# AI Assets

本项目把 Cursor Rules 和 Prompts 作为工程母版的一等能力维护。

## 资产清单

| 位置 | 数量 | 说明 |
|------|------|------|
| `frontend/.cursor/rules/*.mdc` | 11 | 前端开发规则，不含 README |
| `backend/.cursor/rules/*.mdc` | 4 | 后端开发规则，不含 README |
| `frontend/.cursor/prompts/*.md` | 4 | 前端生成模板，不含 README |
| `backend/.cursor/prompts/*.md` | 2 | 后端生成模板，不含 README |

README 文件只用于索引和说明，不计入规则或模板数量。

## 前端 Rules

- `code-style.mdc`：代码风格、命名和目录组织。
- `typescript.mdc`：类型定义、泛型、API 类型和 Vue 类型。
- `vue-component.mdc`：组件结构、Props、Emits、生命周期和组件通信。
- `api-development.mdc`：API 文件、请求函数、模型类型和错误处理。
- `state-management.mdc`：Pinia Store 定义、使用和持久化。
- `router.mdc`：静态路由、动态路由、权限和懒加载。
- `style-development.mdc`：Tailwind CSS、SCSS、Element Plus 和主题。
- `performance.mdc`：组件、列表、请求、图片和打包优化。
- `git-commit.mdc`：Conventional Commits 和提交流程。
- `best-practices.mdc`：项目配置、代码质量、安全和审查清单。
- `troubleshooting.mdc`：开发、类型、路由、API、状态和构建问题排查。

## 后端 Rules

- `java-style.mdc`：Java 命名、结构、注释和 Lombok 使用。
- `spring-boot.mdc`：Controller、Service、Repository、事务和配置。
- `api-design.mdc`：REST API、统一响应、错误码和异常。
- `git-commit.mdc`：后端提交信息和变更组织。

## Prompts

前端：

- `api-module.md`：生成 API 文件和 Model 类型。
- `vue-list-page.md`：生成列表页。
- `vue-business-component.md`：生成业务组件。
- `vue-modal-form.md`：生成新增/编辑弹窗。

后端：

- `crud-module.md`：生成 Entity、Repository、Service、Controller、DTO、VO 和 ErrorCode。
- `api-endpoint.md`：在已有 Controller 中新增 API 端点。

## 维护规则

- 新增、删除或重命名 Rule / Prompt 后，同步更新本文件、对应 `.cursor/**/README` 和 `scripts/check-template.sh`。
- Rules 和 Prompts 中不得保留旧项目名、旧 mock 地址或已废弃技术栈。
- 前端样式口径统一为 Tailwind CSS + SCSS + Element Plus。
- 前端 API 删除请求使用项目 HTTP 工具的 `request.del` 方法。
- 后端接口统一返回 `APIResponse<T>`，列表响应字段使用 `records`、`current`、`size`、`total` 以对齐前端 `PaginatedResponse<T>`。
- 示例代码需使用 `@ScaffoldSample` 标记。
- 每轮发布前运行 `./scripts/check-template.sh` 和 `./scripts/smoke-copy.sh`。
