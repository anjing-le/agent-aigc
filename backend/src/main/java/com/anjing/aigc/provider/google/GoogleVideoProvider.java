package com.anjing.aigc.provider.google;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.provider.VideoGenerationProvider;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Google Veo 视频生成提供商 - REST API 实现
 * 
 * <h3>支持的模型</h3>
 * <ul>
 *   <li>veo-3.1-generate-preview - 最新版，含音频，$0.40/秒</li>
 *   <li>veo-3.1-fast-generate-preview - 快速版，含音频，$0.15/秒</li>
 * </ul>
 * 
 * @author AI Team
 * @see <a href="https://ai.google.dev/gemini-api/docs/video">Veo Video Generation</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aigc.providers.google", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GoogleVideoProvider implements VideoGenerationProvider {
    
    private final AigcProperties aigcProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    
    /**
     * 最大轮询次数（每10秒轮询一次，最多5分钟）
     */
    private static final int MAX_POLL_ATTEMPTS = 30;
    private static final long POLL_INTERVAL_MS = 10000;
    
    private OkHttpClient httpClient;
    
    @PostConstruct
    public void init() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)  // 增加连接超时
                .readTimeout(600, TimeUnit.SECONDS)    // 视频生成需要更长时间，10分钟
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
            var config = aigcProperties.getVideo().getGoogle();
            log.info("✅ Google Video Provider (Veo) 初始化成功");
            log.info("   模型: {}", config.getModel());
            log.info("   默认时长: {}秒", config.getDefaultDuration());
        } else {
            log.warn("⚠️ Google Video Provider 未配置或已禁用");
        }
    }
    
    @Override
    public String getProviderName() {
        return "Google Veo";
    }
    
    @Override
    public boolean isAvailable() {
        return aigcProperties.isGoogleConfigured() 
            && aigcProperties.getVideo().getGoogle().isEnabled();
    }
    
    @Override
    public ContentProvider.ProviderType getProviderType() {
        return ContentProvider.ProviderType.GOOGLE;
    }
    
    @Override
    public boolean supportsImageToVideo() {
        return true;
    }
    
    @Override
    public boolean supportsAudioGeneration() {
        String model = aigcProperties.getVideo().getGoogle().getModel();
        return model.contains("veo-3");
    }
    
    @Override
    public GenerationResult generate(AigcTask task) {
        long startTime = System.currentTimeMillis();
        String taskId = task.getTaskId();
        
        log.info("[GoogleVideoProvider] 开始生成视频");
        log.info("   taskId: {}", taskId);
        log.info("   prompt: {}", truncate(task.getOptimizedPrompt(), 100));
        
        try {
            var config = aigcProperties.getVideo().getGoogle();
            String model = task.getModel() != null ? task.getModel() : config.getModel();
            String prompt = task.getOptimizedPrompt();
            String apiKey = aigcProperties.getProviders().getGoogle().getApiKey();
            
            // 构建请求 URL - 视频生成使用 predictLongRunning 端点
            String url = String.format("%s/%s:predictLongRunning?key=%s", GEMINI_API_BASE, model, apiKey);
            
            // 构建请求体（支持参考图片 → 图生视频）
            List<String> referenceImages = task.getReferenceImages();
            String requestBody = buildVideoRequestBody(prompt, config, referenceImages);
            
            boolean hasRef = referenceImages != null && !referenceImages.isEmpty();
            log.info("调用 Veo API: model={}, duration={}s, 图生视频={}", model, config.getDefaultDuration(), hasRef);
            
            // 发送请求
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                    .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Veo API 调用失败: {} - {}", response.code(), errorBody);
                    return GenerationResult.failure(taskId, "API_ERROR", 
                            "API调用失败: " + response.code() + " - " + truncate(errorBody, 200));
                }
                
                String responseBody = response.body().string();
                JsonNode responseJson = objectMapper.readTree(responseBody);
                
                // Veo 返回操作对象，需要轮询等待完成
                String operationName = responseJson.has("name") ? responseJson.get("name").asText() : null;
                
                if (operationName != null) {
                    // 轮询等待视频生成完成
                    String videoUrl = pollForVideoCompletion(operationName, apiKey, taskId);
                    
                    if (videoUrl != null) {
                        long duration = System.currentTimeMillis() - startTime;
                        log.info("[GoogleVideoProvider] ✅ 视频生成完成, taskId={}, 耗时: {}ms", taskId, duration);
                        
                        return GenerationResult.builder()
                                .success(true)
                                .taskId(taskId)
                                .contentType(ContentType.VIDEO)
                                .url(videoUrl)
                                .prompt(task.getPrompt())
                                .model(model)
                                .processingTimeMs(duration)
                                .build();
                    }
                }
                
                // 尝试直接解析响应（某些模型可能直接返回结果）
                String videoUrl = parseDirectVideoResponse(responseJson, taskId);
                if (videoUrl != null) {
                    long duration = System.currentTimeMillis() - startTime;
                    return GenerationResult.builder()
                            .success(true)
                            .taskId(taskId)
                            .contentType(ContentType.VIDEO)
                            .url(videoUrl)
                            .prompt(task.getPrompt())
                            .model(model)
                            .processingTimeMs(duration)
                            .build();
                }
                
                return GenerationResult.failure(taskId, "NO_VIDEO_GENERATED", "未能生成视频");
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[GoogleVideoProvider] ❌ 视频生成失败, taskId={}, 耗时: {}ms", taskId, duration, e);
            return GenerationResult.failure(taskId, "VIDEO_GENERATION_FAILED", e.getMessage());
        }
    }
    
    @Override
    public CompletableFuture<GenerationResult> generateAsync(AigcTask task) {
        return CompletableFuture.supplyAsync(() -> generate(task));
    }
    
    /**
     * 构建视频生成请求体 (predictLongRunning 格式)
     * 
     * <h3>文生视频格式：</h3>
     * <pre>
     * {
     *   "instances": [{ "prompt": "..." }],
     *   "parameters": { "aspectRatio": "16:9", "durationSeconds": 8 }
     * }
     * </pre>
     * 
     * <h3>图生视频格式：</h3>
     * <pre>
     * {
     *   "instances": [{ 
     *     "prompt": "...",
     *     "image": { "bytesBase64Encoded": "...", "mimeType": "image/jpeg" }
     *   }],
     *   "parameters": { "aspectRatio": "16:9", "durationSeconds": 8 }
     * }
     * </pre>
     */
    private String buildVideoRequestBody(String prompt, AigcProperties.VideoModelConfig config,
                                          List<String> referenceImages) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        
        // instances 数组
        ArrayNode instances = root.putArray("instances");
        ObjectNode instance = instances.addObject();
        instance.put("prompt", prompt);
        
        // 如果有参考图片，添加 image 字段（图生视频）
        if (referenceImages != null && !referenceImages.isEmpty()) {
            String imageData = referenceImages.get(0);
            ObjectNode imageNode = createImageNode(imageData);
            if (imageNode != null) {
                instance.set("image", imageNode);
                log.info("图生视频模式: 已添加参考图片");
            }
        }
        
        // parameters 对象
        ObjectNode parameters = root.putObject("parameters");
        parameters.put("aspectRatio", config.getDefaultAspectRatio());
        parameters.put("durationSeconds", config.getDefaultDuration());
        
        if (config.getDefaultResolution() != null) {
            parameters.put("resolution", config.getDefaultResolution());
        }
        
        return objectMapper.writeValueAsString(root);
    }
    
    /**
     * 创建 Veo API 图片节点
     * 
     * @param imageData Base64 图片数据（支持 data:image/... 格式或纯 Base64）
     * @return 图片 JSON 节点
     */
    private ObjectNode createImageNode(String imageData) throws IOException {
        if (imageData == null || imageData.isBlank()) {
            return null;
        }
        
        String mimeType = "image/jpeg";
        String base64Data = null;
        
        // 处理 data:image/jpeg;base64,... 格式
        if (imageData.startsWith("data:image/")) {
            String[] parts = imageData.split(",");
            if (parts.length == 2) {
                mimeType = parts[0].replace("data:", "").replace(";base64", "");
                base64Data = parts[1];
            }
        }
        // 处理纯 Base64
        else if (imageData.matches("^[A-Za-z0-9+/=]+$") && imageData.length() > 100) {
            base64Data = imageData;
        }
        // 处理本地文件路径
        else if (imageData.startsWith("/") || imageData.startsWith("./")) {
            Path path = Path.of(imageData);
            if (Files.exists(path)) {
                byte[] bytes = Files.readAllBytes(path);
                base64Data = Base64.getEncoder().encodeToString(bytes);
                String lower = imageData.toLowerCase();
                if (lower.endsWith(".png")) mimeType = "image/png";
                else if (lower.endsWith(".webp")) mimeType = "image/webp";
            }
        }
        
        if (base64Data == null) {
            log.warn("无法解析参考图片数据");
            return null;
        }
        
        ObjectNode node = objectMapper.createObjectNode();
        node.put("bytesBase64Encoded", base64Data);
        node.put("mimeType", mimeType);
        return node;
    }
    
    /**
     * 轮询等待视频生成完成
     */
    private String pollForVideoCompletion(String operationName, String apiKey, String taskId) 
            throws IOException, InterruptedException {
        
        String pollUrl = String.format("https://generativelanguage.googleapis.com/v1beta/%s?key=%s", 
                operationName, apiKey);
        
        log.info("开始轮询视频生成状态: {}", operationName);
        
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            Request request = new Request.Builder().url(pollUrl).get().build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("轮询请求失败: {}", response.code());
                    continue;
                }
                
                String responseBody = response.body().string();
                JsonNode json = objectMapper.readTree(responseBody);
                
                boolean done = json.has("done") && json.get("done").asBoolean();
                
                if (done) {
                    if (json.has("error")) {
                        String errorMsg = json.get("error").has("message") 
                                ? json.get("error").get("message").asText() 
                                : "Unknown error";
                        throw new RuntimeException("视频生成失败: " + errorMsg);
                    }
                    
                    log.info("视频生成完成, 共轮询 {} 次", attempt + 1);
                    
                    // 解析结果
                    if (json.has("response")) {
                        return parseDirectVideoResponse(json.get("response"), taskId);
                    }
                    return null;
                }
                
                log.debug("视频生成中... 第 {} 次轮询, taskId: {}", attempt + 1, taskId);
            }
            
            Thread.sleep(POLL_INTERVAL_MS);
        }
        
        throw new RuntimeException("视频生成超时，已等待 " + (MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS / 1000) + " 秒");
    }
    
    /**
     * 解析直接返回的视频响应
     * 
     * 响应格式：
     * {
     *   "generateVideoResponse": {
     *     "generatedSamples": [
     *       { "video": { "uri": "https://..." } }
     *     ]
     *   }
     * }
     */
    private String parseDirectVideoResponse(JsonNode response, String taskId) throws IOException {
        // 新格式: generateVideoResponse.generatedSamples[].video.uri
        if (response.has("generateVideoResponse")) {
            JsonNode generateVideoResponse = response.get("generateVideoResponse");
            if (generateVideoResponse.has("generatedSamples")) {
                JsonNode samples = generateVideoResponse.get("generatedSamples");
                if (samples.isArray() && !samples.isEmpty()) {
                    JsonNode firstSample = samples.get(0);
                    if (firstSample.has("video") && firstSample.get("video").has("uri")) {
                        String videoUri = firstSample.get("video").get("uri").asText();
                        log.info("获取到视频 URI: {}", videoUri);
                        // 下载视频并保存到本地
                        return downloadAndSaveVideo(videoUri, taskId);
                    }
                }
            }
        }
        
        // 旧格式兼容: generatedVideos[].uri
        if (response.has("generatedVideos") && response.get("generatedVideos").isArray()) {
            JsonNode videos = response.get("generatedVideos");
            if (!videos.isEmpty()) {
                JsonNode firstVideo = videos.get(0);
                
                if (firstVideo.has("uri")) {
                    return firstVideo.get("uri").asText();
                }
                
                if (firstVideo.has("video") && firstVideo.get("video").has("videoBytes")) {
                    String base64Data = firstVideo.get("video").get("videoBytes").asText();
                    return saveVideoToLocal(base64Data, taskId);
                }
            }
        }
        
        log.warn("无法解析视频响应: {}", response.toString());
        return null;
    }
    
    /**
     * 从 Google API 下载视频并保存到本地
     */
    private String downloadAndSaveVideo(String videoUri, String taskId) throws IOException {
        String apiKey = aigcProperties.getProviders().getGoogle().getApiKey();
        String downloadUrl = videoUri + (videoUri.contains("?") ? "&" : "?") + "key=" + apiKey;
        
        log.info("开始下载视频: {}", videoUri);
        
        Request request = new Request.Builder()
                .url(downloadUrl)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载视频失败: " + response.code());
            }
            
            byte[] videoBytes = response.body().bytes();
            log.info("视频下载完成, 大小: {} bytes", videoBytes.length);
            
            // 保存到本地
            String basePath = aigcProperties.getStorage().getLocal().getBasePath();
            Path dirPath = Paths.get(basePath, "videos");
            Files.createDirectories(dirPath);
            
            String fileName = taskId + ".mp4";
            Path filePath = dirPath.resolve(fileName);
            Files.write(filePath, videoBytes);
            
            log.info("视频保存成功: {}", filePath);
            
            // 返回访问 URL
            return aigcProperties.getStorage().getLocal().getUrlPrefix() + "/videos/" + fileName;
        }
    }
    
    /**
     * 保存视频到本地
     */
    private String saveVideoToLocal(String base64Data, String taskId) throws IOException {
        byte[] videoBytes = Base64.getDecoder().decode(base64Data);
        
        String fileName = taskId + ".mp4";
        Path outputDir = Paths.get("uploads", "videos");
        Files.createDirectories(outputDir);
        Path outputPath = outputDir.resolve(fileName);
        
        Files.write(outputPath, videoBytes);
        log.debug("视频已保存: {}", outputPath);
        
        return "http://localhost:10003/files/videos/" + fileName;
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }
}
