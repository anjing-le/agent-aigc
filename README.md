# Agent AIGC - 全模态 AI 创作平台

用户输入一句话，系统自动理解意图、选择模型、优化提示词，生成图片 / 视频 / 音频。

**技术栈**：Spring Boot 3 + Vue 3 + MySQL + Redis + Google GenAI + OneRouter

---

## 环境要求

| 依赖 | 版本 | 用途 |
|------|------|------|
| Java | 17+ | 后端运行 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 20+ | 前端运行 |
| pnpm | 8.8+ | 前端包管理 |
| MySQL | 8.0+ | 数据存储 |
| Redis | 6+ | 缓存 & 任务队列 |

## 第一步：下载代码

```bash
git clone git@github.com:anjing-le/agent-aigc.git
cd agent-aigc
```

> 如果没有配置 SSH Key，也可以用 HTTPS：
> ```
> git clone https://github.com/anjing-le/agent-aigc.git
> ```

## 第二步：API Key 准备

本项目需要两个 API Key：

| Key | 用途 | 获取地址 |
|-----|------|---------|
| Google API Key | 图片 / 视频 / 音频生成 | https://aistudio.google.com/apikey |
| OneRouter API Key | 意图识别 & 提示词优化（LLM） | https://onerouter.pro |

## 第三步：创建数据库

```bash
mysql -u root -p
```

```sql
CREATE DATABASE agent_aigc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 表结构由 JPA 自动创建（`ddl-auto: update`），无需手动建表

## 第四步：配置后端

```bash
cd backend/src/main/resources

# 复制配置模板
cp application-local.yml.example application-local.yml
```

编辑 `application-local.yml`，填入你的真实值：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/agent_aigc?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_mysql_password        # ← 改成你的 MySQL 密码

aigc:
  providers:
    google:
      api-key: your_google_api_key       # ← 改成你的 Google API Key
      # proxy-host: 127.0.0.1            # 如需代理访问 Google API，取消注释
      # proxy-port: 7897
    onerouter:
      api-key: your_onerouter_api_key    # ← 改成你的 OneRouter API Key
```

## 第五步：启动 Redis

```bash
# macOS
brew services start redis

# Linux
sudo systemctl start redis

# 验证
redis-cli ping
# 返回 PONG 即可
```

## 第六步：启动后端

```bash
cd backend

mvn clean compile -DskipTests
mvn spring-boot:run
```

**启动成功标志**：

```
✅ Google Image Provider (Nano Banana) 初始化成功
✅ Google Video Provider (Veo) 初始化成功
✅ Google Audio Provider (TTS) 初始化成功
```

> 首次启动 JPA 会自动建表，无需手动导入 SQL

**验证后端**：浏览器打开 http://localhost:10003/api/test/health ，返回正常即可

## 第七步：启动前端

```bash
cd frontend

pnpm install
pnpm dev
```

**启动成功标志**：

```
VITE vX.X.X ready in XXX ms
➜ Local: http://localhost:5173/
```

**验证前端**：浏览器打开 http://localhost:5173 ，能看到登录页面

## 验证完整流程

后端 + 前端 + Redis 都启动后：

1. 打开 http://localhost:5173 ，登录系统
2. 进入「创作工作台」
3. 输入 "画一只猫在星空下跳舞"
4. 等待 Agent 分析意图 → 选择模型 → 生成图片
5. 查看生成结果和 Agent 分析过程

也可以用命令行验证：

```bash
# 发送生成请求
curl -X POST http://localhost:10003/api/aigc/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "一只猫在星空下跳舞"}'

# 查询任务状态（用返回的 taskId）
curl http://localhost:10003/api/aigc/task/{taskId}
```

## 常见问题

### 数据库连接失败
检查 `application-local.yml` 中 MySQL 地址、用户名、密码是否正确，数据库 `agent_aigc` 是否已创建。

### Redis 连接失败
确认 Redis 已启动，默认端口 6379。运行 `redis-cli ping` 检查。

### Google API 调用超时
国内网络可能无法直接访问 Google API，在 `application-local.yml` 中取消 `proxy-host` 和 `proxy-port` 的注释，配置你的代理地址。

### OneRouter 调用失败
检查 API Key 是否正确、余额是否充足：https://onerouter.pro

### 前端 API 返回 404
确保后端在 10003 端口正常运行，检查后端启动日志是否有报错。

### 视频生成耗时很长
正常现象，Veo 视频生成通常需要 1-5 分钟，请耐心等待。

## 项目结构

```
agent-aigc/
├── backend/                      # Spring Boot 后端
│   └── src/main/
│       ├── java/com/anjing/
│       │   └── aigc/             # AIGC 核心模块（Agent、Provider、任务管理）
│       └── resources/
│           ├── application.yml
│           └── application-local.yml.example
└── frontend/                     # Vue 3 前端
    └── src/views/
        └── aigc/                 # 创作工作台页面
```
