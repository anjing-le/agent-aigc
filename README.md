# Agent AIGC - 全模态 AI 创作平台

> 用户输入一句话，系统自动理解意图、选择模型、优化提示词，生成图片 / 视频 / 音频。

## 核心设计

```
用户："帮我画一只猫在星空下跳舞，竖屏4K"
                    ↓
    ┌───────────────────────────────┐
    │  IntentAnalyzer (意图分析)     │  ← gpt-4o-mini 解析自然语言
    │  提取: IMAGE, 9:16, 4K        │
    └───────────────┬───────────────┘
                    ↓
    ┌───────────────────────────────┐
    │  RoutingAgent (智能路由)       │  ← 4K → 选 gemini-3-pro
    └───────────────┬───────────────┘
                    ↓
    ┌───────────────────────────────┐
    │  PromptEnhancer (提示词增强)   │  ← 自动加 "high quality, detailed"
    └───────────────┬───────────────┘
                    ↓
    ┌───────────────────────────────┐
    │  ProviderRouter → Provider    │  ← 按配置路由到 GoogleImageProvider
    │  调用 Gemini API 生成图片      │
    └───────────────────────────────┘
```

**设计要点：**
- 意图分析用便宜的 gpt-4o-mini，内容生成才用 Google GenAI，控制成本
- Provider 接口抽象（Image / Video / Audio），新增模型只需实现接口
- 异步任务 + 前端轮询，适配 AI 生成的长耗时场景

## 功能

| 能力 | 模型 | 说明 |
|------|------|------|
| 文生图 | Gemini 2.5 Flash / 3 Pro | 支持多风格、多比例，最高 4K |
| 图生图 | Gemini | 基于参考图片风格迁移 |
| 文生视频 | Veo 3.1 | 4-8 秒视频，含音频 |
| 图生视频 | Veo 3.1 | 静态图片变动态视频 |
| 语音合成 | Gemini TTS | 5 种预设语音 |

## 技术栈

**后端**：Spring Boot 3.4.5 / Java 17 / JPA / MySQL / Redis / OkHttp

**前端**：Vue 3.5 / TypeScript / Vite 7 / Element Plus / TailwindCSS

## 快速开始

### 环境要求

- JDK 17+、Node.js 20+（pnpm）、MySQL 8.x、Redis

### 1. 创建数据库

```sql
CREATE DATABASE agent_aigc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

表结构由 JPA 自动创建。

### 2. 配置 API Key

```bash
cp backend/src/main/resources/application-local.yml.example \
   backend/src/main/resources/application-local.yml
```

编辑 `application-local.yml`，填入：

```yaml
aigc:
  providers:
    google:
      api-key: <your_google_api_key>       # https://aistudio.google.com/apikey
      # proxy-host: 127.0.0.1              # 如需代理
      # proxy-port: 7897
    onerouter:
      api-key: <your_onerouter_api_key>    # https://onerouter.pro
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
# 端口 10003
```

看到以下日志说明成功：
```
✅ Google Image Provider (Nano Banana) 初始化成功
✅ Google Video Provider (Veo) 初始化成功
✅ Google Audio Provider (TTS) 初始化成功
```

### 4. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
# 端口 5173，自动打开浏览器
```

### 5. 验证

```bash
curl -X POST http://localhost:10003/api/aigc/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "一只猫在星空下跳舞"}'
```

## 项目结构

```
backend/src/main/java/com/anjing/aigc/
├── agent/                          # 智能路由核心
│   ├── IntentAnalyzer.java         # 意图分析（调用 gpt-4o-mini）
│   ├── RoutingAgent.java           # 路由决策 + 模型选择
│   └── PromptEnhancer.java         # 提示词增强
├── provider/                       # 内容生成
│   ├── ContentProvider.java        # 基接口
│   ├── ProviderRouter.java         # 按配置路由到对应 Provider
│   └── google/
│       ├── GoogleImageProvider.java    # Gemini 图片
│       ├── GoogleVideoProvider.java    # Veo 视频（异步轮询）
│       └── GoogleAudioProvider.java    # TTS 语音
├── controller/AigcController.java  # REST API
├── service/impl/AigcServiceImpl.java   # 异步任务调度
└── model/                          # Entity / DTO / Enum

frontend/src/views/aigc/
├── studio/     # 创作工作台（输入 + 轮询 + 展示）
├── gallery/    # 灵感广场（提示词库）
└── assets/     # 我的资产（生成历史）
```

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/aigc/generate` | 智能生成入口 |
| GET | `/api/aigc/task/{taskId}` | 查询任务状态（轮询） |
| GET | `/api/aigc/gallery` | 灵感广场 |
| GET | `/api/aigc/assets` | 我的资产 |

## 许可证

本项目仅供教学和学习使用。
