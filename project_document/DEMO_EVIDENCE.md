# Demo Evidence

本文档记录母版发布或复制前需要保留的演示证据。目标不是堆材料，而是让“能跑、好看、可复制”有可追溯凭据。

## 当前证据

- `./scripts/quality-gate.sh`：聚合模板、契约、复制烟测、后端测试、前端构建、后端 dev runtime probe。
- `./scripts/probe-backend-dev.sh`：启动 dev profile，验证 `/api/test/health`、`/api/test/features` 和 `/v3/api-docs`。
- `project_document/STATUS.md`：记录每个阶段的状态和证据链。

## 发布前证据清单

1. 运行 `./scripts/quality-gate.sh` 并保留通过的提交号。
2. 打开 `http://localhost:13006`，保留登录页桌面截图和移动端截图。
3. 登录或游客进入工作台，保留首页截图。
4. 启动后端 dev profile，记录 `/api/test/health`、`/api/test/features`、`/v3/api-docs` 可访问。
5. 如演示 MySQL profile，记录使用的 `.env` 模板字段，不记录真实密码。

## 建议目录

```text
docs/evidence/YYYY-MM-DD/
  README.md
  login-desktop.png
  login-mobile.png
  dashboard.png
  backend-probe.txt
```

## 记录模板

```markdown
# Evidence YYYY-MM-DD

- Commit: `<commit>`
- Frontend: `http://localhost:13006`
- Backend: `http://localhost:18080`
- Gate: `./scripts/quality-gate.sh`
- Result: passed
```

## 不提交内容

- 真实密钥、Cookie、Token。
- 本地个人路径截图。
- 上传文件、构建产物、后端 target、前端 dist。
