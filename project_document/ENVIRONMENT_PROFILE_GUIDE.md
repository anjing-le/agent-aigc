# Environment Profile Guide

本文档定义后端 `dev/test/prod` profile 和前端 Vite mode 的使用边界。目标是让母版保持开箱轻启动，同时给未来微服务、CI 和生产部署留下清楚的配置入口。

## Backend Profiles

默认 profile：

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

| Profile | 适用场景 | 外部中间件策略 | 典型命令 |
|---------|----------|----------------|----------|
| `dev` | 本地开发和轻量演示 | 内存 H2；Redis、Kafka、MinIO、OSS 默认关闭；缓存为 memory；锁为 local | `SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run` |
| `test` | CI、复制烟测、自动化验证 | 内存 H2；尽量本地化和确定性；关闭 SQL 展示和可选中间件 | `SPRING_PROFILES_ACTIVE=test mvn -q -DskipTests package` |
| `prod` | 生产或预生产部署 | MySQL；外部能力必须通过环境变量显式开启 | `SPRING_PROFILES_ACTIVE=prod java -jar target/*.jar` |

## Database Matrix

| Profile | 默认数据库 | 说明 |
|---------|------------|------|
| `dev` | H2 in-memory, MySQL compatibility mode | 用于第一次启动、轻量演示和无外部依赖本地开发 |
| `test` | H2 in-memory, MySQL compatibility mode | 用于 CI 和烟测，不依赖本机 MySQL |
| `prod` | MySQL | 通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 配置 |

需要在 `dev/test` 联调真实 MySQL 时，显式传入：

```bash
DB_URL='jdbc:mysql://localhost:3306/anjing?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true' \
DB_USERNAME=root \
DB_PASSWORD=your_password \
DB_DRIVER=com.mysql.cj.jdbc.Driver \
DB_DIALECT=org.hibernate.dialect.MySQL8Dialect \
mvn spring-boot:run
```

## Middleware Matrix

| 能力 | dev 默认 | test 默认 | prod 默认 | 开启变量 |
|------|----------|-----------|-----------|----------|
| Redis | disabled | disabled | disabled | `FEATURE_REDIS_ENABLED=true` |
| Cache | memory | memory | memory | `FEATURE_CACHE_TYPE=redis` |
| Distributed Lock | local | local | local | `FEATURE_DISTRIBUTED_LOCK_PROVIDER=redisson` |
| Kafka | disabled | disabled | disabled | `FEATURE_KAFKA_ENABLED=true` |
| MinIO | disabled | disabled | disabled | `FEATURE_MINIO_ENABLED=true` |
| OSS | disabled | disabled | disabled | `FEATURE_OSS_ENABLED=true` |
| Monitoring | enabled | enabled, metrics off | enabled | `FEATURE_MONITORING_METRICS_ENABLED=true` |
| OpenAPI API Docs | enabled | enabled | disabled | `OPENAPI_API_DOCS_ENABLED=true` |

能力运行状态通过：

```http
GET /api/test/features
```

状态语义见 `project_document/FEATURE_STATUS_GUIDE.md`。

## Time And Runtime Context

- 默认时区为 UTC：`APP_TIME_ZONE=UTC`。
- 前端展示时区由用户偏好或浏览器决定，接口层继续透传 `X-Time-Zone`。
- 服务间调用方默认值：`APP_REMOTE_CALLER_ID=infra-dev-scaffolding`。
- 异步执行器默认传播请求上下文和 MDC，可通过 `APP_ASYNC_CORE_POOL_SIZE`、`APP_ASYNC_MAX_POOL_SIZE`、`APP_ASYNC_QUEUE_CAPACITY` 和 `APP_ASYNC_THREAD_NAME_PREFIX` 调整线程池。
- 后端健康检查通过 `/api/test/health` 返回真实 active profiles。

## Frontend Modes

| 文件 | 用途 |
|------|------|
| `frontend/.env` | 所有 mode 的通用变量 |
| `frontend/.env.development` | 本地开发，`VITE_API_URL=/` 并通过 Vite 代理到后端 |
| `frontend/.env.production` | 生产构建，按部署方式设置 `VITE_API_URL` |

前端路径仍统一放在 `src/api/paths.ts`，不要通过 env 拼散落业务 URL。

## Extension Rules

- 新增 profile 时同步更新本指南、`backend/.env.example` 和 `scripts/check-template.sh`。
- 新增可选中间件时必须先给出 feature flag，再考虑真实连接和健康探测。
- 生产 profile 不默认开启外部中间件，避免复制项目在缺少环境配置时启动失败。
- profile 文件只覆盖环境差异，不承载业务模块配置。
