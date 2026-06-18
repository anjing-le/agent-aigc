# agent-aigc Gallery Topics Evidence

本目录记录 2026-06-18 对灵感广场“人工运营专题”的浏览器检查结论。截图文件仍建议放在课件素材仓库或本机素材目录，当前代码仓库只保留可追溯索引和检查结果。

## 基线

- 功能基线：`feat: add gallery editorial topics`
- 后端端口：`18187`
- 前端端口：`5177`
- 后端命令：`SPRING_PROFILES_ACTIVE=dev SERVER_PORT=18187 mvn -q spring-boot:run`
- 前端命令：`VITE_API_PROXY_URL=http://127.0.0.1:18187 pnpm dev --host 127.0.0.1 --port 5177`
- 数据准备：`./scripts/aigc-demo-smoke.sh http://127.0.0.1:18187`

## Smoke 输出

```text
aigc-demo-smoke: ok
aigc-demo-smoke: gallery collections=3 matched=trending
aigc-demo-smoke: gallery topics=2 matched=course-cover
```

## 浏览器检查

桌面视口：

- URL：`http://127.0.0.1:5177/#/aigc/gallery`
- 页面标题：`灵感广场 - Anjing AI`
- `人工运营专题` 可见。
- `课程封面专题` 可见。
- `高复用传播位` 可见。
- 专题数量：`2`
- `documentElement.scrollWidth === clientWidth`，无页面级横向溢出。
- console error：`0`

移动视口 `390x844`：

- `人工运营专题` 可见。
- `课程封面专题` 可见。
- `高复用传播位` 可见。
- 专题数量：`2`
- 专题区为单列，`gridTemplateColumns=294px`。
- `documentElement.scrollWidth === clientWidth`，无页面级横向溢出。
- console error：`0`

## 讲解重点

- 这次检查证明人工运营专题不是静态文案，而是由 `/api/aigc/gallery/topics` 返回真实已发布作品。
- 专题能力继续沿用 `ApiConstants -> service-boundaries.json -> OpenAPI -> openApiRequest -> 页面` 的脚手架链路。
- 移动端检查可用于课堂强调：业务功能完成后，也要验证后台页面能在常见视口下稳定展示。
