# Agent AIGC - 全模态 AI 创作平台

> 用户输入一句话，系统自动理解意图、选择模型、优化提示词，生成图片 / 视频 / 音频。

## 环境要求

| 环境 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 20+  |
| pnpm | 8.8+ |
| MySQL | 8.x |
| Redis | 6.x+ |

## 1. 克隆项目

```bash
git clone git@github.com:anjing-le/agent-aigc.git
cd agent-aigc
```

## 2. 创建数据库

```sql
CREATE DATABASE agent_aigc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

表结构由 JPA 自动创建（`ddl-auto: update`），无需手动建表。

## 3. 配置 API Key

```bash
cp backend/src/main/resources/application-local.yml.example \
   backend/src/main/resources/application-local.yml
```

编辑 `application-local.yml`，填入以下配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/agent_aigc?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_mysql_password        # 改成你的 MySQL 密码

aigc:
  providers:
    google:
      api-key: your_google_api_key       # Google GenAI API Key
      # proxy-host: 127.0.0.1            # 如需代理访问 Google API，取消注释
      # proxy-port: 7897
    onerouter:
      api-key: your_onerouter_api_key    # OneRouter API Key
```

**API Key 获取方式：**
- Google API Key：https://aistudio.google.com/apikey
- OneRouter API Key：https://onerouter.pro

## 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

看到以下日志说明启动成功：

```
✅ Google Image Provider (Nano Banana) 初始化成功
✅ Google Video Provider (Veo) 初始化成功
✅ Google Audio Provider (TTS) 初始化成功
```

后端运行在 http://localhost:10003

## 5. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

前端运行在 http://localhost:5173，自动打开浏览器。

## 6. 验证

### 方式一：命令行验证

```bash
# 发送生成请求
curl -X POST http://localhost:10003/api/aigc/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "一只猫在星空下跳舞"}'

# 正常返回示例：
# {
#   "code": 200,
#   "data": {
#     "taskId": "xxx",
#     "status": "PENDING",
#     "agentAnalysis": {
#       "intent": "text_to_image",
#       "contentType": "IMAGE",
#       "selectedModel": "gemini-2.5-flash-image"
#     }
#   }
# }

# 查询任务状态（用上面返回的 taskId）
curl http://localhost:10003/api/aigc/task/{taskId}
```

### 方式二：前端验证

1. 打开 http://localhost:5173，进入创作工作台
2. 输入"画一只猫在跳舞"
3. 等待 Agent 分析 → 模型生成 → 图片展示

## 常见问题

| 问题 | 解决方案 |
|------|---------|
| Google API 调用超时 | 检查代理配置，取消 `proxy-host` 和 `proxy-port` 的注释 |
| OneRouter 调用失败 | 检查 API Key 和余额：https://onerouter.pro |
| 数据库连接失败 | 确认 MySQL 已启动，密码配置正确 |
| 前端 API 404 | 确保后端在 10003 端口运行 |
| 视频生成耗时长 | 正常现象，Veo 生成需要 1-5 分钟 |
| Redis 连接失败 | 确认 Redis 已启动，默认端口 6379 |

## 许可证

本项目仅供教学和学习使用。
