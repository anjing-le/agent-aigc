package com.anjing.aigc.agent;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.dto.AnalyzedIntent;
import com.anjing.aigc.model.enums.ContentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 智能意图分析器
 * 
 * <h3>核心功能</h3>
 * <p>使用 OneRouter 的 gpt-4o-mini（便宜的 LLM）解析用户自然语言，提取：</p>
 * <ul>
 *   <li>内容类型 - 图片/视频/音频</li>
 *   <li>技术参数 - 分辨率、宽高比、时长等</li>
 *   <li>风格偏好 - 质量、速度、艺术风格</li>
 *   <li>清洗后的提示词 - 去除技术参数后的纯创意描述</li>
 * </ul>
 * 
 * <h3>成本优化</h3>
 * <p>使用 gpt-4o-mini（$0.15/$0.60 per 1M tokens）进行意图解析，
 * 昂贵的 Google GenAI 模型只用于实际内容创作。</p>
 * 
 * @author AI Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentAnalyzer {
    
    private final AigcProperties aigcProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    /**
     * 系统提示词 - 定义LLM如何解析用户意图
     */
    private static final String SYSTEM_PROMPT = """
        你是一个AIGC意图解析器。你的任务是分析用户的自然语言请求，提取结构化信息。
        
        ## 你需要识别的信息：
        
        ### 1. 内容类型 (contentType) - 注意：我们只支持 IMAGE/VIDEO/AUDIO
        - IMAGE: 图片生成、图片编辑、风格转换
        - VIDEO: 视频生成、图片变视频
        - AUDIO: 音乐生成、语音合成
        
        ### 2. 意图场景 (intent)
        - text_to_image: 文字生成图片
        - image_to_image: 图片编辑/风格转换（用户提供了参考图片）
        - text_to_video: 文字生成视频
        - image_to_video: 图片变视频/动态化
        - text_to_audio: 文字生成音乐/语音
        
        ### 3. 图片参数 (imageParams) - 仅当 contentType=IMAGE 时填写
        - aspectRatio: 宽高比
          * "1:1" - 正方形、头像
          * "16:9" - 横屏、风景、桌面壁纸
          * "9:16" - 竖屏、手机壁纸、海报
          * "4:3" - 传统照片比例
          * "3:4" - 竖版照片
          * "21:9" - 超宽屏、电影感
        - imageSize: 图片尺寸
          * "1K" - 标准(1024px)，默认
          * "2K" - 高清(2048px)
          * "4K" - 超高清(4096px)
        - style: 艺术风格（如果用户提及）
          * photorealistic - 写实/照片级
          * anime - 动漫/二次元
          * oil_painting - 油画风格
          * watercolor - 水彩风格
          * sketch - 素描/线稿
          * 3d_render - 3D渲染
          * pixel_art - 像素风
          * 其他用户描述的风格
        
        ### 4. 视频参数 (videoParams) - 仅当 contentType=VIDEO 时填写
        - aspectRatio: 宽高比
          * "16:9" - 横屏（默认）
          * "9:16" - 竖屏/短视频
        - resolution: 分辨率
          * "720p" - 标准（默认）
          * "1080p" - 高清
        - duration: 时长（秒）
          * 用户说"短一点" → 4
          * 用户说"几秒" → 取最接近的 4/6/8
          * 默认 8
        - quality: 质量偏好
          * "fast" - 用户说"快一点"、"先看看效果"
          * "standard" - 用户说"高质量"、"精细"、"流畅"
        - withAudio: 是否需要音频
          * true - 默认，或用户说"带声音"、"配乐"
          * false - 用户说"静音"、"不要声音"
        
        ### 5. 音频参数 (audioParams) - 仅当 contentType=AUDIO 时填写
        - type: 音频类型
          * "tts" - 语音合成（朗读、配音、旁白）← 默认
          * "music" - 音乐生成（歌曲、旋律、节拍）← 实验性
        - voice: TTS语音选择（仅 type=tts 时）
          * "Kore" - 清新、年轻（默认）
          * "Aoede" - 温柔、优雅（女声）
          * "Fenrir" - 深沉、有力（男声）
          * "Puck" - 活泼、温暖
          * "Charon" - 沉稳、权威
        - bpm: 节拍（60-200，仅 type=music 时）
          * 用户说"快节奏"→120+
          * 用户说"慢节奏"→80-
        - mood: 情绪（happy, sad, energetic, calm, epic, romantic...）
        
        ### 6. 清洗后的提示词 (cleanPrompt)
        从原始输入中移除所有技术参数描述，只保留创意内容。
        例如："帮我生成一个4K竖屏视频，一只猫在跳舞" → "一只猫在跳舞"
        
        ## 输出格式（严格JSON）：
        ```json
        {
          "contentType": "IMAGE|VIDEO|AUDIO",
          "intent": "text_to_image|image_to_image|text_to_video|...",
          "cleanPrompt": "去除技术参数后的纯创意描述",
          "hasReferenceImage": false,
          "imageParams": {
            "aspectRatio": "16:9",
            "imageSize": "1K",
            "style": null
          },
          "videoParams": {
            "aspectRatio": "16:9",
            "resolution": "720p",
            "duration": 8,
            "quality": "standard",
            "withAudio": true
          },
          "audioParams": {
            "type": "tts",
            "voice": "Kore",
            "bpm": null,
            "mood": null
          },
          "confidence": 0.95
        }
        ```
        
        ## 注意事项：
        1. 只输出JSON，不要有其他文字
        2. 未明确提及的参数使用null或默认值
        3. confidence表示你对解析结果的置信度(0-1)
        4. 如果用户意图模糊，优先解析为图片生成
        5. 我们不支持纯文本生成，所以不要返回 contentType=TEXT
        """;
    
    /**
     * 分析用户意图
     * 
     * @param userInput 用户原始输入
     * @param hasReferenceImages 是否有参考图片
     * @return 结构化的意图分析结果
     * @throws RuntimeException 如果 OneRouter 未配置或调用失败
     */
    public AnalyzedIntent analyze(String userInput, boolean hasReferenceImages) {
        long startTime = System.currentTimeMillis();
        log.info("[IntentAnalyzer] 开始分析用户意图: {}", truncate(userInput, 100));
        
        // 检查 OneRouter 是否配置 - 不降级，直接报错
        if (!aigcProperties.isOneRouterConfigured()) {
            throw new RuntimeException("[IntentAnalyzer] OneRouter 未配置！请在 application-local.yml 中配置 aigc.providers.onerouter");
        }
        
        var oneRouterConfig = aigcProperties.getProviders().getOnerouter();
        log.info("[IntentAnalyzer] 调用 OneRouter: model={}, url={}", 
                oneRouterConfig.getModel(), oneRouterConfig.getApiUrl());
        
        // 构建 OpenAI 兼容的请求体
        Map<String, Object> requestBody = Map.of(
            "model", oneRouterConfig.getModel(),
            "temperature", oneRouterConfig.getTemperature(),
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", buildUserMessage(userInput, hasReferenceImages))
            )
        );
        
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(oneRouterConfig.getApiKey());
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // 发送请求到 OneRouter - 不 try-catch，让异常直接抛出
        String apiUrl = oneRouterConfig.getApiUrl() + "/chat/completions";
        ResponseEntity<Map> response = restTemplate.exchange(
            apiUrl,
            HttpMethod.POST,
            entity,
            Map.class
        );
        
        // 解析响应
        String jsonResponse = extractJsonFromResponse(response.getBody());
        AnalyzedIntent intent;
        try {
            intent = objectMapper.readValue(jsonResponse, AnalyzedIntent.class);
        } catch (Exception e) {
            throw new RuntimeException("[IntentAnalyzer] JSON 解析失败: " + jsonResponse, e);
        }
        
        // 补充上下文信息
        intent.setOriginalPrompt(userInput);
        intent.setHasReferenceImage(hasReferenceImages);
        
        // 根据是否有参考图片调整意图
        if (hasReferenceImages) {
            adjustIntentForReferenceImages(intent);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("[IntentAnalyzer] ✅ 意图分析完成, 耗时: {}ms, 模型: {}", duration, oneRouterConfig.getModel());
        log.info("   contentType: {}", intent.getContentType());
        log.info("   intent: {}", intent.getIntent());
        log.info("   cleanPrompt: {}", truncate(intent.getCleanPrompt(), 50));
        log.info("   confidence: {}", intent.getConfidence());
        
        return intent;
    }
    
    /**
     * 构建发送给LLM的用户消息
     */
    private String buildUserMessage(String userInput, boolean hasReferenceImages) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户输入: ").append(userInput);
        if (hasReferenceImages) {
            sb.append("\n\n[上下文: 用户上传了参考图片]");
        }
        return sb.toString();
    }
    
    /**
     * 从 OpenAI 格式响应中提取 JSON
     */
    @SuppressWarnings("unchecked")
    private String extractJsonFromResponse(Map<String, Object> response) {
        if (response == null) {
            throw new RuntimeException("OneRouter 返回空响应");
        }
        
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("OneRouter 返回无效响应");
        }
        
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");
        
        if (content == null || content.isBlank()) {
            throw new RuntimeException("OneRouter 返回空内容");
        }
        
        // 提取JSON（处理可能的markdown代码块）
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        
        return content.trim();
    }
    
    /**
     * 根据参考图片调整意图
     */
    private void adjustIntentForReferenceImages(AnalyzedIntent intent) {
        // 如果有参考图片，调整意图类型
        if (intent.getContentType() == ContentType.IMAGE) {
            intent.setIntent("image_to_image");
        } else if (intent.getContentType() == ContentType.VIDEO) {
            intent.setIntent("image_to_video");
        }
    }
    
    /**
     * 降级策略：当 OneRouter 调用失败时使用规则引擎
     */
    private AnalyzedIntent createFallbackIntent(String userInput, boolean hasReferenceImages) {
        log.warn("[IntentAnalyzer] 使用降级策略进行意图解析");
        
        AnalyzedIntent intent = new AnalyzedIntent();
        intent.setOriginalPrompt(userInput);
        intent.setCleanPrompt(userInput); // 降级时不清洗
        intent.setHasReferenceImage(hasReferenceImages);
        intent.setConfidence(0.5); // 低置信度
        
        String lower = userInput.toLowerCase();
        
        // 简单规则判断内容类型
        if (lower.contains("视频") || lower.contains("video") || lower.contains("动起来") || lower.contains("动画")) {
            intent.setContentType(ContentType.VIDEO);
            intent.setIntent(hasReferenceImages ? "image_to_video" : "text_to_video");
            intent.setVideoParams(AnalyzedIntent.VideoParams.createDefault());
        } else if (lower.contains("音乐") || lower.contains("music") || lower.contains("歌曲") || 
                   lower.contains("配乐") || lower.contains("朗读") || lower.contains("语音")) {
            intent.setContentType(ContentType.AUDIO);
            intent.setIntent("text_to_audio");
            intent.setAudioParams(AnalyzedIntent.AudioParams.createDefault());
        } else {
            // 默认为图片生成
            intent.setContentType(ContentType.IMAGE);
            intent.setIntent(hasReferenceImages ? "image_to_image" : "text_to_image");
            intent.setImageParams(AnalyzedIntent.ImageParams.createDefault());
        }
        
        return intent;
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }
}
