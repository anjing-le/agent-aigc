# AIGC 项目 - 面试 Q&A 准备清单

> 使用方法：看着 Q 能说出 A，用自己的话复述，不要背诵。

---

## 一、项目层面

### Q1: 介绍一下你的项目？

**A:** 我做的是一个全模态 AI 创作平台。用户只需要输入一句话，比如"帮我画一只猫在星空下跳舞"，系统会自动理解用户意图、选择合适的 AI 模型、优化提示词，然后生成图片、视频或音频。核心亮点是一个智能路由 Agent，它用便宜的 gpt-4o-mini 做意图分析，然后把任务路由到 Google Gemini 生图、Veo 生视频、或者 TTS 做语音合成。后端用 Spring Boot 3 + Java 17，前端用 Vue 3，支持文生图、图生图、文生视频、图生视频、语音合成五种能力。

### Q2: 为什么选择做 AIGC 创作平台？

**A:** 两个原因。第一，AIGC 是目前 AI 应用最热门的方向之一，Google、OpenAI 都在推多模态生成能力，我想深入理解这些模型怎么用。第二，我发现现有的 AI 生成工具都要求用户自己选模型、调参数、写英文提示词，门槛很高。我想做一个"用户说中文就能用"的平台，核心挑战就是怎么设计 Agent 来自动处理这些事情。

### Q3: 项目的核心功能有哪些？

**A:** 五个生成能力加三个业务功能。生成能力是：文生图、图生图、文生视频、图生视频、语音合成。业务功能是：创作工作台（输入需求 + 展示结果）、灵感广场（提示词库）、我的资产（生成历史管理）。最核心的是智能路由 Agent，它把用户的自然语言自动转化为结构化的 AI 调用请求。

### Q4: 什么是 Agent？你的项目里 Agent 是怎么设计的？

**A:** Agent 可以理解为一个"有决策能力的智能代理"。普通的 API 调用是写死的逻辑，Agent 是用 LLM 来做决策。在我的项目里，Agent 是一个四层处理管道：第一层 IntentAnalyzer 用 gpt-4o-mini 分析用户说的话，提取出内容类型、技术参数；第二层 RoutingAgent 根据分析结果选择最优模型，比如用户要 4K 图片就自动升级到 Gemini 3 Pro；第三层 PromptEnhancer 自动给提示词加上质量修饰词；第四层通过 ProviderRouter 路由到具体的 Provider 执行生成。

### Q5: 意图分析是怎么做的？怎么判断用户想生成图片还是视频？

**A:** 意图分析是通过 IntentAnalyzer 实现的，它调用 OneRouter 的 gpt-4o-mini 模型。我写了一个详细的系统提示词，定义了 LLM 应该从用户输入中提取哪些信息：内容类型是 IMAGE/VIDEO/AUDIO、意图场景是 text_to_image 还是 image_to_video、技术参数比如宽高比、分辨率、时长等。LLM 返回一个结构化的 JSON，我用 Jackson 反序列化成 AnalyzedIntent 对象。比如用户说"帮我生成一个竖屏短视频，一只猫在跳舞"，LLM 会解析出 contentType=VIDEO、aspectRatio=9:16、duration=6。temperature 设为 0.1，保证解析结果的确定性。

### Q6: 智能路由是怎么实现的？怎么选择合适的模型？

**A:** RoutingAgent 里有一个 selectOptimalModel 方法，用 Java 17 的 switch 表达式按内容类型分发。图片方面，如果用户要 2K 或 4K 高分辨率，就选 gemini-3-pro-image-preview，否则用默认的 gemini-2.5-flash-image，快且便宜。视频方面，如果用户说"快一点"就用 veo-3.1-fast-generate-preview（$0.15/秒），说"高质量"就用 veo-3.1-generate-preview（$0.40/秒）。音频固定用 gemini-2.5-flash-preview-tts。这种设计让模型选择是动态的，不是写死的。

### Q7: 文生图是怎么调用的？用的什么模型？

**A:** 文生图用的是 Google Gemini API。在 GoogleImageProvider 里，我用 OkHttp 直接调用 Gemini 的 REST API，请求格式是 `contents.parts` 包含文本提示词，`generationConfig` 设置 responseModalities 为 TEXT 和 IMAGE，还可以配置宽高比。Gemini 返回 Base64 编码的图片数据，我解码后保存到本地的 uploads/images 目录，通过 Spring 的静态资源映射对外提供访问。默认模型是 gemini-2.5-flash-image，生成 1024px 图片大概 10-30 秒。

### Q8: 文生视频是怎么实现的？和文生图有什么区别？

**A:** 最大的区别是视频生成是异步的。图片是同步调用 `generateContent` 接口，直接拿到结果。视频用的是 Veo 的 `predictLongRunning` 接口，发送请求后返回一个 operation name，然后需要每 10 秒轮询一次操作状态，直到 `done: true`。我在 GoogleVideoProvider 里实现了一个轮询循环，最多轮询 30 次（5 分钟），完成后从响应里拿到视频 URI，再下载保存到本地。整个过程可能需要 1-5 分钟。

### Q9: 图生图、图生视频是怎么处理参考图片的？

**A:** 前端把用户上传的图片转成 Base64，放在请求的 referenceImages 数组里。IntentAnalyzer 分析时会感知到有参考图片，自动把 intent 调整为 image_to_image 或 image_to_video。图生图时，Gemini API 的 contents.parts 里除了文本还包含一个 inlineData 节点（Base64 图片数据）。图生视频时，Veo API 的 instances 里多一个 image 字段（bytesBase64Encoded）。PromptEnhancer 也会针对性增强，比如图生图会加上"maintain the original composition and style"。

### Q10: 任务状态轮询是怎么做的？为什么要异步？

**A:** 因为 AI 内容生成耗时很长，图片 10-30 秒，视频 1-5 分钟，不能让 HTTP 请求一直阻塞。所以我用了异步任务 + 前端轮询的方案。后端用 Spring 的 @Async 注解把生成任务放到独立线程池执行，主请求立即返回 taskId。前端拿到 taskId 后每秒调用一次 GET /api/aigc/task/{taskId}，检查任务状态。PENDING → PROCESSING → COMPLETED 或 FAILED。视频还有双重轮询：前端轮询后端任务状态，后端内部轮询 Veo 的 operation 状态。

### Q11: 项目的整体架构是怎样的？

**A:** 分三层。最上面是前端 Vue 3，有创作工作台、灵感广场、我的资产三个页面。中间是后端 Spring Boot 3，核心是两个模块：智能路由 Agent（IntentAnalyzer + RoutingAgent + PromptEnhancer）负责理解和决策，Provider 层（GoogleImageProvider / GoogleVideoProvider / GoogleAudioProvider）负责实际调用 AI 模型。最下面是数据层，MySQL 存任务和资产记录（AigcTask + AigcAsset），Redis 做缓存。前后端通过 REST API 交互，关键接口就两个：POST generate 创建任务、GET task 轮询状态。

### Q12: Provider 抽象是怎么设计的？有什么好处？

**A:** 我定义了一个 ContentProvider 基接口，包含 getProviderName()、isAvailable()、generate() 等方法。然后扩展出 ImageGenerationProvider、VideoGenerationProvider、AudioGenerationProvider 三个子接口，各自定义特有的能力方法，比如图片的 supportsImageToImage()、视频的 supportsAudioGeneration()。ProviderRouter 通过 Spring 的依赖注入自动收集所有实现，根据配置文件的 active-provider 选择激活哪个。好处是符合开闭原则，比如将来要接入 DALL-E，只需要实现 ImageGenerationProvider 接口、加个 @Component 注解，改一下配置就行，完全不用动已有代码。

### Q13: 前后端是怎么交互的？

**A:** 前端用 Axios 封装了 HTTP 请求，Vite 配置了 /api 代理到后端 localhost:10003。交互流程是：用户输入 → 前端 POST /api/aigc/generate → 后端返回 taskId 和 Agent 分析结果 → 前端开始每秒轮询 GET /api/aigc/task/{taskId} → 状态变为 COMPLETED 时拿到生成结果的 URL → 展示图片/视频/音频。前端最多轮询 180 次（3 分钟），超时提示重试。

### Q14: 如果要接入更多模型（如 DALL-E、Midjourney），架构怎么扩展？

**A:** 非常简单，三步。第一步，实现对应的 Provider 接口，比如写一个 DalleImageProvider implements ImageGenerationProvider，在里面调用 OpenAI 的 API。第二步，加上 @Component 注解让 Spring 自动注册。第三步，在配置文件里把 active-provider 改成 openai。ProviderRouter 会自动发现新的 Provider 并路由过去。意图分析和提示词增强完全不需要改，因为它们只关心"生成什么类型的内容"，不关心"谁来生成"。

### Q15: 生成任务很慢，怎么优化用户体验？

**A:** 我做了几个优化。第一，Agent 分析完后立即返回分析结果给前端展示，让用户知道系统已经理解了需求，不是卡住了。第二，任务状态有进度字段（progress），前端可以展示进度条。第三，视频生成用了 fast 模型作为默认（$0.15/秒 vs $0.40/秒），牺牲一点质量换速度。如果继续优化，可以加 WebSocket 替代轮询实现实时推送，或者加缓存对相似提示词直接返回已有结果。

### Q16: 如果生成失败了，怎么处理？

**A:** 分两层处理。Provider 层，每个 Provider 的 generate 方法内部 try-catch，失败时返回 GenerationResult.failure() 带错误码和错误信息，不会抛异常。Service 层，executeGenerationAsync 方法会检查 result.isSuccess()，失败时只更新任务状态为 FAILED 并记录 errorMessage，不会创建 Asset。外层还有一个大的 try-catch 兜底，防止任何未预期的异常导致任务状态卡在 PROCESSING。前端轮询到 FAILED 状态时会展示错误信息提示用户重试。

### Q17: 项目有什么不足？如果继续做会怎么改进？

**A:** 几个方面。第一，轮询方案不够优雅，可以改成 WebSocket 或 SSE 实现服务端推送。第二，文件存储目前是本地磁盘，生产环境应该用 OSS。第三，没有用户体系，所有人共享资产，应该加用户认证和资产隔离。第四，意图分析的降级策略可以更完善，目前 OneRouter 挂了就直接报错，可以加规则引擎做兜底。第五，提示词增强目前是简单的字符串拼接，可以改成用 LLM 做更智能的增强。

### Q18: 为什么意图分析用 gpt-4o-mini 而不直接用 Google 的模型？

**A:** 成本和职责分离两个原因。gpt-4o-mini 的价格是 $0.15/1M tokens，非常便宜，而 Gemini Pro 要 $7/1M tokens，差了 46 倍。意图分析只是提取结构化信息，不需要强大的生成能力，用便宜模型就够了。职责分离方面，OneRouter 走的是 OpenAI 兼容 API 格式，和 Google 的生成 API 完全独立，互不影响。如果 OneRouter 换了 LLM 提供商，生成侧完全不受影响。

---

## 二、大模型应用层面

### Q1: 大模型（LLM）是什么？有什么特点？

**A:** LLM 是 Large Language Model，大型语言模型，比如 GPT-4、Gemini、Claude。它们通过海量文本数据训练，学会了理解和生成自然语言。特点有三个：一是通用性强，一个模型能做翻译、写代码、做问答等多种任务；二是通过 Prompt 就能指导行为，不需要重新训练；三是有涌现能力，模型规模大了之后会出现训练时没有明确教过的能力，比如推理和规划。在我的项目里，我用 gpt-4o-mini 做意图分析，就是利用了它理解自然语言的能力。

### Q2: 什么是多模态（Multimodal）？

**A:** 多模态是指模型能处理和生成多种类型的数据，不只是文本。比如 Gemini 是多模态模型，它既能理解文本输入，也能生成图片。Veo 可以根据文本生成视频，还能根据图片生成视频。在我的项目里，用户输入的是文本（自然语言），输出可以是图片、视频或音频，这就是多模态应用。我的 Agent 的核心工作就是判断用户需要哪种模态的输出，然后路由到对应的模型。

### Q3: Prompt 是什么？怎么写好一个 Prompt？

**A:** Prompt 就是给 AI 模型的指令或输入，它决定了模型输出什么内容。写好 Prompt 有几个要点：要明确角色（你是一个意图解析器）、明确任务（从用户输入中提取结构化信息）、给出格式要求（输出严格 JSON）、提供示例（举例说明怎么解析）。在我的项目里，IntentAnalyzer 的系统提示词就是一个精心设计的 Prompt，它详细定义了内容类型枚举、参数格式、默认值规则，让 LLM 能稳定输出结构化结果。

### Q4: 文生图的 Prompt 和对话的 Prompt 有什么区别？

**A:** 对话的 Prompt 偏逻辑和指令，比如"你是一个翻译助手，把以下内容翻译成英文"。文生图的 Prompt 偏描述和视觉，要描述画面内容、风格、光照、构图等。比如"一只猫在星空下跳舞"是基础描述，但加上"oil painting style, golden hour lighting, high quality, detailed"效果会好很多。这就是为什么我在项目里做了 PromptEnhancer，它会自动给用户的描述加上质量修饰词，比如图片加"high quality, detailed"，视频加"smooth motion, cinematic quality"。

### Q5: 文生图的原理是什么？

**A:** 主流的文生图模型用的是 Diffusion（扩散）模型。简单理解就是两个过程：训练时是"加噪声"，把清晰图片逐步加噪声变成纯噪点；生成时是"去噪声"，从纯噪点开始，在文本 Prompt 的引导下逐步去噪，最终生成和描述匹配的图片。Google 的 Gemini 系列图片生成也是基于类似原理。不过作为应用开发者，我们不需要关心模型内部原理，只需要通过 API 传入 Prompt 和参数就行，这也是我项目中 Provider 层做的事情。

### Q6: 什么是 Agent？和普通的 API 调用有什么区别？

**A:** 普通 API 调用是确定性的，你调什么就返回什么，逻辑是写死的。Agent 是有决策能力的，它能根据输入动态决定下一步做什么。在我的项目里，如果是普通 API 调用方式，用户必须自己选模型、选参数、写英文提示词。但用了 Agent，用户只说"帮我画一只猫"，Agent 用 LLM 理解意图，自动决定用图片模型、选 16:9 宽高比、增强提示词，整个决策过程是动态的。Agent 的核心是"感知 → 决策 → 执行"，而不是写死的 if-else。

### Q7: 什么是 Function Calling / Tool Use？

**A:** Function Calling 是让 LLM 能调用外部工具的能力。比如你问 LLM "今天天气怎么样"，LLM 本身不知道实时天气，但它可以决定调用一个天气 API，拿到结果后再回答你。在我的项目里，虽然没有直接用 Function Calling，但设计理念是类似的：IntentAnalyzer 用 LLM 做"决策"（分析用户意图），然后把决策结果交给"工具"（Provider）去执行。如果要升级，可以把各个 Provider 注册为 LLM 的 Tool，让 LLM 自己选择调用哪个。

### Q8: 多模态模型和单模态模型有什么区别？

**A:** 单模态模型只处理一种数据，比如 GPT-3 只处理文本。多模态模型能处理多种数据类型，比如 Gemini 能同时理解文本和图片输入，也能输出文本和图片。在我的项目里，Gemini 就是多模态的——它接收文本 Prompt 输出图片，接收文本+图片输出新图片（图生图）。Veo 也是多模态的，它能接收文本或文本+图片，输出视频。多模态让 AI 应用的可能性大大增加，但也带来了架构挑战，比如怎么统一处理不同类型的输入输出，这就是我做 Provider 抽象的原因。

### Q9: 大模型应用和传统应用开发有什么区别？

**A:** 三个主要区别。第一，结果不确定性，传统应用输入确定输出就确定，LLM 每次输出可能不同，所以要处理解析失败、结果不符合预期的情况。第二，延迟高，传统 API 毫秒级响应，AI 生成可能要几十秒甚至几分钟，需要异步架构。第三，成本模型不同，传统应用主要是服务器成本，AI 应用还要按 token 或按次数付费给模型提供商。在我的项目里，针对这三点分别做了：意图分析的 JSON 解析容错、异步任务 + 轮询机制、用便宜模型做路由 + 按需选择贵的模型。

### Q10: 你觉得 AIGC 应用的难点在哪里？

**A:** 我觉得有三个难点。第一是不确定性管理，AI 生成的结果不可预测，可能触发安全过滤、可能质量不达标，需要做好重试和降级。第二是成本控制，模型调用按量计费，如果架构设计不好很容易超支，比如我用便宜的 gpt-4o-mini 做意图分析就是一个成本优化。第三是用户体验，AI 生成很慢，怎么让用户不觉得在等待，需要做好进度反馈和异步交互设计。

### Q11: AIGC 有哪些应用场景？未来发展趋势是什么？

**A:** 应用场景很多：电商的商品图自动生成、短视频平台的内容创作、游戏的素材生产、教育领域的可视化教学、广告行业的创意批量产出。未来趋势我觉得有几个：一是生成质量会越来越高，接近专业水平；二是多模态融合，一次输入同时生成文案 + 图片 + 视频；三是实时交互，现在生图要几十秒，未来可能做到实时；四是个性化，模型能学习用户风格偏好，生成更符合个人品味的内容。

### Q12: 你在这个项目中学到了什么？

**A:** 三个方面。技术上，学会了怎么对接 Google 的多模态 API，包括 Gemini 的 generateContent 和 Veo 的 predictLongRunning 两种不同的调用模式。架构上，理解了 Agent 模式的设计思想，怎么把"理解 → 决策 → 执行"分层解耦，以及 Provider 抽象的开闭原则。工程上，掌握了异步任务管理、前后端轮询机制、成本控制策略这些实际落地的经验。最大的收获是意识到 AI 应用开发和传统开发的思维方式很不一样，要拥抱不确定性，在架构上做好弹性设计。
