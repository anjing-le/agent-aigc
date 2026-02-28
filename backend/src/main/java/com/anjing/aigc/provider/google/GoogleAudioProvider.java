package com.anjing.aigc.provider.google;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.provider.AudioGenerationProvider;
import com.anjing.aigc.provider.ContentProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Google 音频生成提供商 (TTS) - REST API 实现
 * 
 * <h3>支持的模型</h3>
 * <ul>
 *   <li>gemini-2.5-flash-preview-tts - 文字转语音 (TTS)</li>
 *   <li>gemini-2.5-pro-preview-tts - 高质量TTS</li>
 * </ul>
 * 
 * <h3>TTS 支持的语音</h3>
 * <ul>
 *   <li>Puck - 活泼、温暖的声音</li>
 *   <li>Charon - 沉稳、权威的声音</li>
 *   <li>Kore - 清新、年轻的声音</li>
 *   <li>Fenrir - 深沉、有力的声音</li>
 *   <li>Aoede - 温柔、优雅的声音</li>
 * </ul>
 * 
 * @author AI Team
 * @see <a href="https://ai.google.dev/gemini-api/docs/text-to-speech">Gemini TTS</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aigc.providers.google", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GoogleAudioProvider implements AudioGenerationProvider {
    
    private final AigcProperties aigcProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    
    private static final String DEFAULT_VOICE = "Kore";
    
    private static final Map<String, String> VOICE_MAP = Map.of(
            "puck", "Puck",
            "charon", "Charon",
            "kore", "Kore",
            "fenrir", "Fenrir",
            "aoede", "Aoede"
    );
    
    private OkHttpClient httpClient;
    
    @PostConstruct
    public void init() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)  // 音频生成需要更长时间
                .writeTimeout(60, TimeUnit.SECONDS);
        
        // 配置代理 (访问 Google API 需要代理)
        var googleConfig = aigcProperties.getProviders().getGoogle();
        if (googleConfig.getProxyHost() != null && googleConfig.getProxyPort() != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, 
                    new InetSocketAddress(googleConfig.getProxyHost(), googleConfig.getProxyPort()));
            builder.proxy(proxy);
            log.info("   使用代理: {}:{}", googleConfig.getProxyHost(), googleConfig.getProxyPort());
        }
        
        httpClient = builder.build();
        
        if (isAvailable()) {
            var config = aigcProperties.getAudio().getGoogle();
            log.info("✅ Google Audio Provider (TTS) 初始化成功");
            log.info("   模型: {}", config.getModel());
        } else {
            log.warn("⚠️ Google Audio Provider 未配置或已禁用");
        }
    }
    
    @Override
    public String getProviderName() {
        return "Google TTS";
    }
    
    @Override
    public boolean isAvailable() {
        return aigcProperties.isGoogleConfigured() 
            && aigcProperties.getAudio().getGoogle().isEnabled();
    }
    
    @Override
    public ContentProvider.ProviderType getProviderType() {
        return ContentProvider.ProviderType.GOOGLE;
    }
    
    @Override
    public boolean supportsMusicGeneration() {
        return false; // Lyria 是实时流式API，暂不支持
    }
    
    @Override
    public boolean supportsTTS() {
        return true;
    }
    
    @Override
    public GenerationResult generate(AigcTask task) {
        long startTime = System.currentTimeMillis();
        String taskId = task.getTaskId();
        
        log.info("[GoogleAudioProvider] 开始生成音频");
        log.info("   taskId: {}", taskId);
        log.info("   prompt: {}", truncate(task.getOptimizedPrompt(), 100));
        
        String prompt = task.getOptimizedPrompt();
        
        // 判断是 TTS 还是音乐生成
        if (isMusicRequest(prompt)) {
            // 音乐生成暂不支持
            return GenerationResult.builder()
                    .success(false)
                    .taskId(taskId)
                    .contentType(ContentType.AUDIO)
                    .errorCode("MUSIC_NOT_SUPPORTED")
                    .errorMessage("音乐生成功能(Lyria RealTime)是实时流式API，需要WebSocket支持。当前版本建议使用TTS功能。")
                    .prompt(task.getPrompt())
                    .model("lyria-realtime-exp")
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
        
        return generateTTS(task, startTime);
    }
    
    /**
     * 生成 TTS 语音
     */
    private GenerationResult generateTTS(AigcTask task, long startTime) {
        String taskId = task.getTaskId();
        log.info("模式: 文字转语音 (TTS)");
        
        try {
            String model = "gemini-2.5-flash-preview-tts";
            String prompt = task.getOptimizedPrompt();
            String voiceName = extractVoice(prompt);
            String apiKey = aigcProperties.getProviders().getGoogle().getApiKey();
            
            // 构建请求 URL
            String url = String.format("%s/%s:generateContent?key=%s", GEMINI_API_BASE, model, apiKey);
            
            // 构建请求体
            String requestBody = buildTTSRequestBody(prompt, voiceName);
            
            log.debug("调用 TTS API: model={}, voice={}", model, voiceName);
            
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                    .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("TTS API 调用失败: {} - {}", response.code(), errorBody);
                    return GenerationResult.failure(taskId, "API_ERROR", 
                            "API调用失败: " + response.code() + " - " + truncate(errorBody, 200));
                }
                
                String responseBody = response.body().string();
                String audioUrl = parseAudioResponse(responseBody, taskId);
                
                if (audioUrl == null) {
                    return GenerationResult.failure(taskId, "NO_AUDIO_GENERATED", "未能生成音频");
                }
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("[GoogleAudioProvider] ✅ TTS生成完成, taskId={}, voice={}, 耗时: {}ms", 
                        taskId, voiceName, duration);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", "tts");
                metadata.put("voice", voiceName);
                
                return GenerationResult.builder()
                        .success(true)
                        .taskId(taskId)
                        .contentType(ContentType.AUDIO)
                        .url(audioUrl)
                        .prompt(task.getPrompt())
                        .model(model)
                        .processingTimeMs(duration)
                        .metadata(metadata)
                        .build();
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[GoogleAudioProvider] ❌ TTS生成失败, taskId={}, 耗时: {}ms", taskId, duration, e);
            return GenerationResult.failure(taskId, "TTS_GENERATION_FAILED", e.getMessage());
        }
    }
    
    /**
     * 构建 TTS 请求体
     */
    private String buildTTSRequestBody(String prompt, String voiceName) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        
        // contents
        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        ObjectNode textPart = parts.addObject();
        textPart.put("text", prompt);
        
        // generationConfig
        ObjectNode generationConfig = root.putObject("generationConfig");
        
        // responseModalities = ["AUDIO"]
        ArrayNode modalities = generationConfig.putArray("responseModalities");
        modalities.add("AUDIO");
        
        // speechConfig
        ObjectNode speechConfig = generationConfig.putObject("speechConfig");
        ObjectNode voiceConfig = speechConfig.putObject("voiceConfig");
        ObjectNode prebuiltVoiceConfig = voiceConfig.putObject("prebuiltVoiceConfig");
        prebuiltVoiceConfig.put("voiceName", voiceName);
        
        return objectMapper.writeValueAsString(root);
    }
    
    /**
     * 解析音频响应
     */
    private String parseAudioResponse(String responseBody, String taskId) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.get("candidates");
        
        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
            log.warn("API 响应中没有候选结果");
            return null;
        }
        
        JsonNode content = candidates.get(0).get("content");
        if (content == null) return null;
        
        JsonNode parts = content.get("parts");
        if (parts == null || !parts.isArray()) return null;
        
        for (JsonNode part : parts) {
            if (part.has("inlineData")) {
                JsonNode inlineData = part.get("inlineData");
                String mimeType = inlineData.has("mimeType") ? inlineData.get("mimeType").asText() : "audio/mp3";
                String data = inlineData.has("data") ? inlineData.get("data").asText() : null;
                
                if (data != null && !data.isBlank()) {
                    // 保存音频文件
                    return saveAudioToLocal(data, mimeType, taskId);
                }
            }
        }
        
        return null;
    }
    
    /**
     * 保存音频到本地
     */
    private String saveAudioToLocal(String base64Data, String mimeType, String taskId) throws IOException {
        byte[] audioBytes = Base64.getDecoder().decode(base64Data);
        
        String extension = getExtensionFromMimeType(mimeType);
        String fileName = taskId + "_tts." + extension;
        
        Path outputDir = Paths.get("uploads", "audio");
        Files.createDirectories(outputDir);
        Path outputPath = outputDir.resolve(fileName);
        
        Files.write(outputPath, audioBytes);
        log.debug("音频已保存: {}, 大小: {} bytes", outputPath, audioBytes.length);
        
        return "http://localhost:10003/files/audio/" + fileName;
    }
    
    /**
     * 判断是否为音乐请求
     */
    private boolean isMusicRequest(String prompt) {
        String lower = prompt.toLowerCase();
        return lower.contains("音乐") || lower.contains("music") || 
               lower.contains("歌曲") || lower.contains("song") ||
               lower.contains("旋律") || lower.contains("melody") ||
               lower.contains("节拍") || lower.contains("beat");
    }
    
    /**
     * 从提示词中提取语音选择
     */
    private String extractVoice(String prompt) {
        String lower = prompt.toLowerCase();
        
        for (Map.Entry<String, String> entry : VOICE_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        if (lower.contains("男声") || lower.contains("male") || lower.contains("深沉")) {
            return "Fenrir";
        }
        if (lower.contains("女声") || lower.contains("female") || lower.contains("温柔")) {
            return "Aoede";
        }
        if (lower.contains("活泼") || lower.contains("energetic")) {
            return "Puck";
        }
        if (lower.contains("权威") || lower.contains("authoritative")) {
            return "Charon";
        }
        
        return DEFAULT_VOICE;
    }
    
    private String getExtensionFromMimeType(String mimeType) {
        return switch (mimeType) {
            case "audio/mp3", "audio/mpeg" -> "mp3";
            case "audio/wav", "audio/x-wav" -> "wav";
            case "audio/ogg" -> "ogg";
            case "audio/flac" -> "flac";
            case "audio/aac" -> "aac";
            default -> "mp3";
        };
    }
    
    @Override
    public CompletableFuture<GenerationResult> generateAsync(AigcTask task) {
        return CompletableFuture.supplyAsync(() -> generate(task));
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }
}
