package com.anjing.aigc.agent;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.dto.AnalyzedIntent;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.request.GenerateRequest;
import com.anjing.aigc.model.response.AgentAnalysis;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.provider.ProviderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * 智能路由Agent - AIGC核心
 * 
 * <h3>核心职责</h3>
 * <p>用户只需描述需求，Agent自动处理一切：</p>
 * <ol>
 *   <li><b>意图理解</b> - 使用 OneRouter gpt-4o-mini 解析用户自然语言，提取技术参数</li>
 *   <li><b>模型选择</b> - 根据内容类型和质量要求选择最优的 Google 模型</li>
 *   <li><b>提示词优化</b> - 自动增强提示词质量</li>
 *   <li><b>任务执行</b> - 调用对应的 Google Provider 完成生成</li>
 * </ol>
 * 
 * <h3>架构说明</h3>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                         1️⃣ 智能路由 Agent (核心)                             │
 * │  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐         │
 * │  │  IntentAnalyzer │ -> │  RoutingAgent   │ -> │ PromptEnhancer  │         │
 * │  │   (意图分析)     │    │   (路由决策)     │    │  (提示词优化)    │         │
 * │  └─────────────────┘    └─────────────────┘    └─────────────────┘         │
 * │         ↑                                                                   │
 * │         └── 使用 OneRouter gpt-4o-mini ($0.15/$0.60 per 1M tokens)         │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *                                    ↓
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                         2️⃣ 内容生成 Providers                               │
 * │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
 * │  │  IMAGE Provider  │  │  VIDEO Provider  │  │  AUDIO Provider  │          │
 * │  │  (Google Nano 🍌) │  │  (Google Veo)    │  │  (Google Lyria)  │          │
 * │  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h3>智能参数提取示例</h3>
 * <pre>
 * 用户输入: "帮我生成一个4K的竖屏视频，一只猫在跳舞，要流畅一点，大概5秒就行"
 * 
 * Agent解析:
 * - contentType: VIDEO
 * - resolution: 4K → 1080p (最高支持)
 * - aspectRatio: 9:16 (竖屏)
 * - duration: 6秒 (就近取值)
 * - quality: standard (流畅 → 标准质量)
 * - cleanPrompt: "一只猫在跳舞"
 * </pre>
 * 
 * @author AI Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingAgent {
    
    private final IntentAnalyzer intentAnalyzer;
    private final PromptEnhancer promptEnhancer;
    private final ProviderRouter providerRouter;
    private final AigcProperties aigcProperties;
    
    /**
     * 分析用户请求 - Agent核心决策
     * 
     * <p>流程：意图解析 → 参数提取 → 模型选择 → 提示词优化</p>
     * 
     * @param request 用户请求（只包含描述和可选素材）
     * @return Agent分析决策结果
     */
    public AgentAnalysis analyze(GenerateRequest request) {
        String prompt = request.getPrompt();
        List<String> referenceImages = request.getReferenceImages();
        boolean hasReference = !CollectionUtils.isEmpty(referenceImages);
        
        log.info("========== Agent开始分析 ==========");
        log.info("原始输入: {}", truncate(prompt, 100));
        log.info("参考素材: {} 张", hasReference ? referenceImages.size() : 0);
        
        // 1. 使用 OneRouter gpt-4o-mini 进行智能意图解析
        AnalyzedIntent analyzedIntent = intentAnalyzer.analyze(prompt, hasReference);
        applyUserOverrides(analyzedIntent, request);
        
        // 2. 根据解析结果选择最优的 Google 模型
        String selectedModel = selectOptimalModel(analyzedIntent);
        
        // 3. 智能优化提示词
        String optimizedPrompt = promptEnhancer.enhance(
                analyzedIntent.getCleanPrompt(), 
                analyzedIntent.getContentType(),
                hasReference
        );
        
        log.info("========== Agent决策完成 ==========");
        log.info("内容类型: {}", analyzedIntent.getContentType());
        log.info("意图场景: {}", analyzedIntent.getIntent());
        log.info("选定模型: {}", selectedModel);
        log.info("置信度: {}", analyzedIntent.getConfidence());
        
        return AgentAnalysis.builder()
                .intent(analyzedIntent.getIntent())
                .contentType(analyzedIntent.getContentType())
                .selectedModel(selectedModel)
                .originalPrompt(prompt)
                .cleanPrompt(analyzedIntent.getCleanPrompt())
                .optimizedPrompt(optimizedPrompt)
                .analyzedIntent(analyzedIntent)
                .confidence(analyzedIntent.getConfidence())
                .build();
    }
    
    /**
     * 执行生成任务
     * 
     * @param task 任务信息（包含分析结果）
     * @return 生成结果
     */
    public GenerationResult executeGeneration(AigcTask task) {
        log.info("[RoutingAgent] 执行生成任务: taskId={}, contentType={}, model={}",
                task.getTaskId(), task.getContentType(), task.getModel());
        
        try {
            // 我们只支持 IMAGE / VIDEO / AUDIO
            GenerationResult result = switch (task.getContentType()) {
                case IMAGE -> providerRouter.getImageProvider().generate(task);
                case VIDEO -> providerRouter.getVideoProvider().generate(task);
                case AUDIO -> providerRouter.getAudioProvider().generate(task);
                case TEXT -> throw new UnsupportedOperationException("不支持纯文本生成，请使用图片/视频/音频功能");
            };
            
            log.info("[RoutingAgent] ✅ 生成完成: taskId={}", task.getTaskId());
            return result;
            
        } catch (Exception e) {
            log.error("[RoutingAgent] ❌ 生成失败: taskId={}", task.getTaskId(), e);
            return GenerationResult.failure(task.getTaskId(), "GENERATION_FAILED", e.getMessage());
        }
    }
    
    /**
     * 根据意图分析结果选择最优模型
     */
    private String selectOptimalModel(AnalyzedIntent intent) {
        return switch (intent.getContentType()) {
            case IMAGE -> selectImageModel(intent);
            case VIDEO -> selectVideoModel(intent);
            case AUDIO -> selectAudioModel(intent);
            case TEXT -> throw new UnsupportedOperationException("不支持纯文本生成");
        };
    }
    
    /**
     * 选择图片模型
     * 
     * 决策逻辑：
     * - 需要4K/高质量 → gemini-3-pro-image-preview
     * - 其他情况 → gemini-2.5-flash-image (快速便宜)
     */
    private String selectImageModel(AnalyzedIntent intent) {
        if (isActiveProvider("mock", aigcProperties.getActiveImageProvider())) {
            return "mock-image-preview";
        }

        var imageParams = intent.getEffectiveImageParams();
        
        // 如果需要高分辨率(2K/4K)，使用 Gemini 3 Pro
        if ("2K".equals(imageParams.getImageSize()) || "4K".equals(imageParams.getImageSize())) {
            log.info("检测到高分辨率需求({}), 选择 Gemini 3 Pro Image", imageParams.getImageSize());
            return "gemini-3-pro-image-preview";
        }
        
        // 默认使用配置的模型
        return aigcProperties.getImage().getGoogle().getModel();
    }
    
    /**
     * 选择视频模型
     * 
     * 决策逻辑：
     * - quality=fast → veo-3.1-fast-generate-preview
     * - quality=standard → veo-3.1-generate-preview
     */
    private String selectVideoModel(AnalyzedIntent intent) {
        if (isActiveProvider("mock", aigcProperties.getActiveVideoProvider())) {
            return "mock-video-preview";
        }

        var videoParams = intent.getEffectiveVideoParams();
        
        String baseModel = aigcProperties.getVideo().getGoogle().getModel();
        
        // 根据质量偏好调整模型
        if ("fast".equals(videoParams.getQuality())) {
            // 用户想要快速预览
            if (!baseModel.contains("fast")) {
                String fastModel = baseModel.replace("-generate-", "-fast-generate-");
                log.info("用户偏好快速生成, 切换到: {}", fastModel);
                return fastModel;
            }
        } else if ("standard".equals(videoParams.getQuality())) {
            // 用户想要高质量
            if (baseModel.contains("fast")) {
                String standardModel = baseModel.replace("-fast-generate-", "-generate-");
                log.info("用户偏好高质量, 切换到: {}", standardModel);
                return standardModel;
            }
        }
        
        return baseModel;
    }
    
    /**
     * 选择音频模型
     */
    private String selectAudioModel(AnalyzedIntent intent) {
        if (isActiveProvider("mock", aigcProperties.getActiveAudioProvider())) {
            return "mock-audio-preview";
        }

        var audioParams = intent.getEffectiveAudioParams();
        
        // 根据音频类型选择模型
        if ("tts".equals(audioParams.getType())) {
            return "gemini-2.5-flash-preview-tts";
        }
        
        return aigcProperties.getAudio().getGoogle().getModel();
    }

    private boolean isActiveProvider(String expected, String actual) {
        return actual != null && expected.equalsIgnoreCase(actual.trim());
    }

    private void applyUserOverrides(AnalyzedIntent intent, GenerateRequest request) {
        applyContentTypeHint(intent, request.getContentTypeHint(), !CollectionUtils.isEmpty(request.getReferenceImages()));
        applyGenerationParams(intent, request.getGenerationParams());
    }

    private void applyContentTypeHint(AnalyzedIntent intent, String contentTypeHint, boolean hasReference) {
        if (contentTypeHint == null || contentTypeHint.isBlank()) {
            return;
        }

        ContentType contentType = ContentType.valueOf(contentTypeHint.trim().toUpperCase());
        intent.setContentType(contentType);
        switch (contentType) {
            case IMAGE -> {
                intent.setIntent(hasReference ? "image_to_image" : "text_to_image");
                if (intent.getImageParams() == null) {
                    intent.setImageParams(AnalyzedIntent.ImageParams.createDefault());
                }
            }
            case VIDEO -> {
                intent.setIntent(hasReference ? "image_to_video" : "text_to_video");
                if (intent.getVideoParams() == null) {
                    intent.setVideoParams(AnalyzedIntent.VideoParams.createDefault());
                }
            }
            case AUDIO -> {
                intent.setIntent("text_to_audio");
                if (intent.getAudioParams() == null) {
                    intent.setAudioParams(AnalyzedIntent.AudioParams.createDefault());
                }
            }
            case TEXT -> throw new UnsupportedOperationException("不支持纯文本生成");
        }
        intent.setConfidence(1.0);
    }

    private void applyGenerationParams(AnalyzedIntent intent, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return;
        }

        switch (intent.getContentType()) {
            case IMAGE -> applyImageParams(intent, params);
            case VIDEO -> applyVideoParams(intent, params);
            case AUDIO -> applyAudioParams(intent, params);
            case TEXT -> throw new UnsupportedOperationException("不支持纯文本生成");
        }
    }

    private void applyImageParams(AnalyzedIntent intent, Map<String, Object> params) {
        var imageParams = intent.getEffectiveImageParams();
        getString(params, "aspectRatio").ifPresent(imageParams::setAspectRatio);
        getString(params, "imageSize").ifPresent(imageParams::setImageSize);
        getString(params, "style").ifPresent(imageParams::setStyle);
        intent.setImageParams(imageParams);
    }

    private void applyVideoParams(AnalyzedIntent intent, Map<String, Object> params) {
        var videoParams = intent.getEffectiveVideoParams();
        getString(params, "aspectRatio").ifPresent(videoParams::setAspectRatio);
        getString(params, "resolution").ifPresent(videoParams::setResolution);
        getInteger(params, "duration").ifPresent(videoParams::setDuration);
        getString(params, "quality").ifPresent(videoParams::setQuality);
        intent.setVideoParams(videoParams);
    }

    private void applyAudioParams(AnalyzedIntent intent, Map<String, Object> params) {
        var audioParams = intent.getEffectiveAudioParams();
        getString(params, "audioType").ifPresent(audioParams::setType);
        getString(params, "voice").ifPresent(audioParams::setVoice);
        getInteger(params, "bpm").ifPresent(audioParams::setBpm);
        getString(params, "mood").ifPresent(audioParams::setMood);
        intent.setAudioParams(audioParams);
    }

    private java.util.Optional<String> getString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null || value.toString().isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(value.toString().trim());
    }

    private java.util.Optional<Integer> getInteger(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null || value.toString().isBlank()) {
            return java.util.Optional.empty();
        }
        if (value instanceof Number number) {
            return java.util.Optional.of(number.intValue());
        }
        try {
            return java.util.Optional.of(Integer.parseInt(value.toString()));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }
}
