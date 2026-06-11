# Feature Status Guide

本文档定义脚手架中“可选能力 / 中间件状态”的统一表达。它面向本地启动、自检接口、未来运维页和微服务拆分后的健康面板。

## Status Model

| 状态 | 含义 | 处理建议 |
|------|------|----------|
| `disabled` | 能力被显式关闭 | 不展示告警，不作为启动失败 |
| `configured` | 能力已启用且配置存在，但当前状态检查不主动连接外部服务 | 可在运维页提示“已配置，待真实探测” |
| `ready` | 本地能力或核心能力可直接使用 | 正常展示 |
| `degraded` | 能力已启用，但必要配置缺失或依赖冲突 | 需要提示修复配置 |

## Runtime Endpoint

脚手架自检接口：

```http
GET /api/test/features
```

响应使用标准 `APIResponse<T>`：

```json
{
  "code": "0",
  "message": "操作成功",
  "data": {
    "status": "configured",
    "statusCode": "configured",
    "statusDescription": "已配置",
    "summary": {
      "total": 8,
      "enabled": 5,
      "byStatus": {
        "disabled": 3,
        "configured": 3,
        "ready": 2,
        "degraded": 0
      }
    },
    "features": []
  }
}
```

## Design Rules

- 默认不主动连接 Redis、Kafka、MinIO、OSS 等外部中间件，避免脚手架本地启动被可选能力拖垮。
- `configured` 表示配置完整，不等于远程服务真实可用。
- `ready` 适合本地能力、随应用启动即可确定的能力，或后续接入真实健康探测后的成功状态。
- `degraded` 只用于“启用了但配置不完整 / 依赖矛盾”的情况。
- 新增可选能力时同步补充 `MiddlewareManager`、`application.yml` / `.env.example`、本指南和需要展示的前端路径。

## Future Direction

后续如果接入 Actuator health group、OpenTelemetry 或网关运维页，可以继续复用这套状态词典。真实探测应当是可选开关，不改变母版默认轻启动策略。
