# Error Code Guide

错误码是脚手架的跨端、跨服务契约。后端、前端、网关、日志平台和未来微服务都应按同一套分段理解错误来源、用户提示方式和重试策略。

## 总体规则

- 所有错误码枚举必须实现 `ErrorCode`。
- 错误码使用字符串，保持 4 位数字；成功固定为 `0`。
- 新增模块先确认分段，再新增 `XxxErrorCode`，不要复用已有 code。
- 错误码分段事实来源是 `contracts/platform-contract.json`，Java 枚举由 `scripts/check-error-codes.js` 校验唯一性、格式和范围。
- 错误信息可以给用户展示时才写成用户语言；系统、远程、集成类错误不要暴露内部细节。
- Controller 不手动拼错误响应，优先抛出 `BizException(ErrorCode)` 或 `SystemException(ErrorCode)`。

## 分段表

| 范围 | 层级 | 当前归属 | 说明 |
|------|------|----------|------|
| `0` | success | `APIResponse.SUCCESS_CODE` | 成功响应固定值 |
| `1000-1499` | system | `CommonErrorCode` | 系统、数据库、缓存、配置、基础设施通用错误 |
| `1500-1599` | infrastructure | `LockErrorCode` | 分布式锁相关错误 |
| `1600-1899` | remote | `RemoteErrorCode` | RPC / HTTP / 第三方远程调用错误 |
| `1900-1999` | workflow | `StateMachineErrorCode` | 状态机、流程编排和状态转换错误 |
| `2000-2099` | business-common | `CommonErrorCode` | 通用业务错误，如数据不存在、重复、不可操作 |
| `2100-2399` | auth | `AuthErrorCode` | 登录、账号、权限、角色、Token |
| `2400-2999` | business-module | `AigcErrorCode` / 下游业务模块 | AIGC 使用 `2400-2499`，其他业务模块每个模块建议 100 个 code |
| `3000-3999` | validation | `CommonErrorCode` | 参数、格式、范围、校验错误 |
| `4000-4099` | security/gateway | `CommonErrorCode` | 全局未登录、无权、Token 失效等网关/安全入口错误 |
| `4100-4999` | gateway/reserved | 网关或平台保留 | 限流、熔断、灰度、跨服务鉴权等平台能力 |
| `5000-7999` | downstream-app | 下游应用保留 | 从脚手架复制出去的业务系统可按应用分配 |
| `8000-8999` | integration | 外部集成保留 | 支付、短信、OSS、地图、LLM Provider 等外部系统 |
| `9000-9999` | platform-ops | 平台运维保留 | 部署、配置、迁移、数据修复等平台级异常 |

## 远程调用重试

`RemoteCallWrapper` 默认只把 `1800-1899` 视作可重试远程错误，因为这类错误通常代表网络、连接、读写超时或熔断瞬时状态。
实际判断读取 `PlatformContractConstants.ErrorCodes.RETRYABLE_RANGES`，不要在业务代码里重新硬编码重试范围。

- `1600-1699`：调用契约或远程服务基础错误，默认不自动重试。
- `1700-1799`：响应格式或状态校验错误，默认不自动重试。
- `1800-1899`：网络、连接、超时、熔断，可按调用场景重试。
- `2xxx`、`3xxx`、`4xxx`：业务、参数、权限错误，默认不重试，直接提示或引导用户处理。

`RemoteCallPolicy` 拒绝服务间调用时，默认使用 `RemoteErrorCode.REMOTE_CALL_CIRCUIT_BREAKER_OPEN`；下游如果实现独立网关/限流平台，也可以在 `4100-4999` 网关保留区分配更细的治理错误码。

## 新模块分配方式

业务模块优先使用 `2400-2999`，每个模块按 100 个 code 分配：

| 示例模块 | 建议范围 |
|----------|----------|
| aigc | `2400-2499` |
| file | `2500-2599` |
| audit-log | `2600-2699` |
| tenant | `2700-2799` |
| user-profile | `2800-2899` |

下游独立项目如果需要更多业务错误码，可以在 `5000-7999` 中按应用继续分配，并在自己的 `project_document/ERROR_CODE_GUIDE.md` 中记录。

## 前端处理建议

- `0`：正常解析 `data`。
- `2xxx` / `3xxx` / `4xxx`：可以展示后端 `message`，必要时跳转登录或权限页。
- `1xxx` / `16xx-19xx` / `8xxx`：展示通用失败文案，同时保留 `requestId` 方便排查。
- 所有错误提示都应携带或可复制 `requestId`，日志中用 `requestId` / `traceId` 追踪完整链路。

## Verification

新增或调整错误码后运行：

```bash
node scripts/check-error-codes.js
```

发布母版或复制项目前运行：

```bash
./scripts/check-contracts.sh
```
