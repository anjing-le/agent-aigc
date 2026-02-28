package com.anjing.aigc.provider.google;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.dto.ImageGenerationRequest;
import com.anjing.aigc.model.dto.ImageGenerationResult;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.provider.ImageGenerationProvider;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Google Gemini 图片生成提供商 (Nano Banana) - REST API 实现
 * 
 * <h3>支持的模型</h3>
 * <ul>
 *   <li>gemini-2.5-flash-image - 快速高效，1024px，适合大批量低延迟任务</li>
 *   <li>gemini-3-pro-image-preview - 高质量，支持1K/2K/4K分辨率</li>
 * </ul>
 * 
 * @author AI Team
 * @see <a href="https://ai.google.dev/gemini-api/docs/image-generation">Gemini Image Generation</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aigc.providers.google", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GoogleImageProvider implements ImageGenerationProvider {
    
    private final AigcProperties aigcProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // API 端点会根据配置动态选择（直连 or 中转）
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    
    private OkHttpClient httpClient;
    
    @PostConstruct
    public void init() {
        // 初始化 HTTP 客户端
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);
        
        // 应用代理配置
        var googleConfig = aigcProperties.getProviders().getGoogle();
        if (googleConfig.getProxyHost() != null && googleConfig.getProxyPort() != null) {
            builder.proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, 
                    new java.net.InetSocketAddress(googleConfig.getProxyHost(), googleConfig.getProxyPort())));
            log.info("Google Image Provider 配置了代理: {}:{}", 
                    googleConfig.getProxyHost(), googleConfig.getProxyPort());
        }
        httpClient = builder.build();
        
        if (isAvailable()) {
            log.info("✅ Google Image Provider (Nano Banana) 初始化成功");
            log.info("   模型: {}", aigcProperties.getImage().getGoogle().getModel());
            log.info("   默认宽高比: {}", aigcProperties.getImage().getGoogle().getDefaultAspectRatio());
            log.info("   API Base: {}", googleConfig.getBaseUrl());
        } else {
            log.warn("⚠️ Google Image Provider 未配置或已禁用");
        }
    }
    
    @Override
    public String getProviderName() {
        return "Google Nano Banana";
    }
    
    @Override
    public boolean isAvailable() {
        boolean googleConfigured = aigcProperties.isGoogleConfigured();
        boolean imageEnabled = aigcProperties.getImage().getGoogle().isEnabled();
        
        if (!googleConfigured) {
            log.debug("Google 未配置 - apiKey: {}", 
                    aigcProperties.getProviders().getGoogle().getApiKey() != null 
                            ? "已设置(长度:" + aigcProperties.getProviders().getGoogle().getApiKey().length() + ")" 
                            : "null");
        }
        if (!imageEnabled) {
            log.debug("Image provider 未启用");
        }
        
        return googleConfigured && imageEnabled;
    }
    
    @Override
    public ProviderType getProviderType() {
        return ContentProvider.ProviderType.GOOGLE;
    }
    
    @Override
    public boolean supportsImageToImage() {
        return true;
    }
    
    @Override
    public boolean supportsImageEditing() {
        return true;
    }
    
    @Override
    public ImageGenerationResult generate(ImageGenerationRequest request) {
        long startTime = System.currentTimeMillis();
        String taskId = request.getTaskId() != null ? request.getTaskId() : UUID.randomUUID().toString();
        
        log.info("[GoogleImageProvider] 开始生成图片");
        log.info("   taskId: {}", taskId);
        log.info("   prompt: {}", truncate(request.getPrompt(), 100));
        
        try {
            var googleConfig = aigcProperties.getProviders().getGoogle();
            String model = aigcProperties.getImage().getGoogle().getModel();
            String prompt = request.getOptimizedPrompt() != null 
                    ? request.getOptimizedPrompt() 
                    : request.getPrompt();
            String aspectRatio = request.getAspectRatio() != null 
                    ? request.getAspectRatio() 
                    : aigcProperties.getImage().getGoogle().getDefaultAspectRatio();
            
            // 构建请求 URL（直连 Google API）
            String apiKey = googleConfig.getApiKey();
            String url = googleConfig.getBaseUrl() + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
            log.debug("调用 Google API: {}", url);
            
            // 构建请求体
            String requestBody = buildRequestBody(prompt, aspectRatio, model, request.getReferenceImages());
            
            log.debug("调用 Gemini API: model={}, aspectRatio={}", model, aspectRatio);
            
            // 构建请求
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                    .addHeader("User-Agent", "AIGC-Platform/1.0")
                    .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Gemini API 调用失败: {} - {}", response.code(), errorBody);
                    return ImageGenerationResult.failure(taskId, "API_ERROR", 
                            "API调用失败: " + response.code() + " - " + truncate(errorBody, 200));
                }
                
                String responseBody = response.body().string();
                List<ImageGenerationResult.GeneratedImage> images = parseResponse(responseBody);
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("[GoogleImageProvider] ✅ 图片生成完成, taskId: {}, 生成 {} 张图片, 耗时: {}ms", 
                        taskId, images.size(), duration);
                
                if (images.isEmpty()) {
                    return ImageGenerationResult.failure(taskId, "NO_IMAGE_GENERATED", 
                            "API返回成功但未生成图片，可能触发了安全过滤");
                }
                
                return ImageGenerationResult.success(taskId, images, model, prompt, duration);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[GoogleImageProvider] ❌ 图片生成失败, taskId: {}, 耗时: {}ms", taskId, duration, e);
            return ImageGenerationResult.failure(taskId, "GENERATION_ERROR", e.getMessage());
        }
    }
    
    @Override
    public CompletableFuture<ImageGenerationResult> generateAsync(ImageGenerationRequest request) {
        return CompletableFuture.supplyAsync(() -> generate(request));
    }
    
    /**
     * 实现 ContentProvider 接口的 generate 方法
     */
    @Override
    public GenerationResult generate(AigcTask task) {
        ImageGenerationRequest request = ImageGenerationRequest.builder()
                .taskId(task.getTaskId())
                .prompt(task.getPrompt())
                .optimizedPrompt(task.getOptimizedPrompt())
                .referenceImages(task.getReferenceImages())
                .build();
        
        ImageGenerationResult imageResult = generate(request);
        
        if (!imageResult.isSuccess()) {
            return GenerationResult.failure(task.getTaskId(), 
                    imageResult.getErrorCode(), imageResult.getErrorMessage());
        }
        
        // 保存第一张图片并返回 URL
        if (imageResult.getImages() != null && !imageResult.getImages().isEmpty()) {
            ImageGenerationResult.GeneratedImage firstImage = imageResult.getImages().get(0);
            String url = saveImageToLocal(firstImage.getBase64Data(), firstImage.getMimeType(), task.getTaskId());
            
            return GenerationResult.builder()
                    .success(true)
                    .taskId(task.getTaskId())
                    .contentType(ContentType.IMAGE)
                    .url(url)
                    .thumbnailUrl(url) // TODO: 生成缩略图
                    .prompt(task.getPrompt())
                    .model(imageResult.getModel())
                    .processingTimeMs(imageResult.getProcessingTimeMs())
                    .build();
        }
        
        return GenerationResult.failure(task.getTaskId(), "NO_IMAGE", "未生成图片");
    }
    
    /**
     * 保存图片到本地
     */
    private String saveImageToLocal(String base64Data, String mimeType, String taskId) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            String extension = mimeType.contains("png") ? "png" : "jpg";
            String fileName = taskId + "." + extension;
            
            Path outputDir = Path.of("uploads", "images");
            Files.createDirectories(outputDir);
            Path outputPath = outputDir.resolve(fileName);
            
            Files.write(outputPath, imageBytes);
            log.debug("图片已保存: {}", outputPath);
            
            return "http://localhost:10003/files/images/" + fileName;
        } catch (Exception e) {
            log.error("保存图片失败", e);
            return null;
        }
    }
    
    @Override
    public CompletableFuture<GenerationResult> generateAsync(AigcTask task) {
        return CompletableFuture.supplyAsync(() -> generate(task));
    }
    
    /**
     * 构建 Gemini API 请求体
     */
    private String buildRequestBody(String prompt, String aspectRatio, String model, 
                                     List<String> referenceImages) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        
        // contents
        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        
        // 添加文本提示
        ObjectNode textPart = parts.addObject();
        textPart.put("text", prompt);
        
        // 添加参考图片（如果有）
        if (referenceImages != null) {
            for (String imageData : referenceImages) {
                ObjectNode imagePart = createImagePart(imageData);
                if (imagePart != null) {
                    parts.add(imagePart);
                }
            }
        }
        
        // generationConfig
        ObjectNode generationConfig = root.putObject("generationConfig");
        
        // responseModalities
        ArrayNode modalities = generationConfig.putArray("responseModalities");
        modalities.add("TEXT");
        modalities.add("IMAGE");
        
        // imageConfig (for aspectRatio)
        if (aspectRatio != null && !aspectRatio.isBlank()) {
            ObjectNode imageConfig = generationConfig.putObject("imageConfig");
            imageConfig.put("aspectRatio", aspectRatio);
            
            // Gemini 3 Pro 支持 imageSize
            if (model.contains("gemini-3-pro")) {
                String imageSize = aigcProperties.getImage().getGoogle().getDefaultImageSize();
                if (imageSize != null && !imageSize.isBlank()) {
                    imageConfig.put("imageSize", imageSize.toUpperCase());
                }
            }
        }
        
        return objectMapper.writeValueAsString(root);
    }
    
    /**
     * 创建图片 Part
     */
    private ObjectNode createImagePart(String imageData) throws IOException {
        if (imageData == null || imageData.isBlank()) {
            return null;
        }
        
        String mimeType = "image/jpeg";
        String base64Data = null;
        
        // 处理 Base64 格式 (data:image/jpeg;base64,...)
        if (imageData.startsWith("data:image/")) {
            String[] parts = imageData.split(",");
            if (parts.length == 2) {
                mimeType = parts[0].replace("data:", "").replace(";base64", "");
                base64Data = parts[1];
            }
        }
        // 处理纯 Base64
        else if (isBase64(imageData)) {
            base64Data = imageData;
        }
        // 处理本地文件路径
        else if (imageData.startsWith("/") || imageData.startsWith("./")) {
            Path path = Path.of(imageData);
            if (Files.exists(path)) {
                byte[] bytes = Files.readAllBytes(path);
                base64Data = Base64.getEncoder().encodeToString(bytes);
                mimeType = determineMimeType(imageData);
            }
        }
        
        if (base64Data == null) {
            return null;
        }
        
        ObjectNode part = objectMapper.createObjectNode();
        ObjectNode inlineData = part.putObject("inlineData");
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64Data);
        
        return part;
    }
    
    /**
     * 解析 API 响应
     */
    private List<ImageGenerationResult.GeneratedImage> parseResponse(String responseBody) throws IOException {
        List<ImageGenerationResult.GeneratedImage> images = new ArrayList<>();
        
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.get("candidates");
        
        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
            log.warn("API 响应中没有候选结果");
            return images;
        }
        
        JsonNode content = candidates.get(0).get("content");
        if (content == null) {
            log.warn("API 响应中没有内容");
            return images;
        }
        
        JsonNode parts = content.get("parts");
        if (parts == null || !parts.isArray()) {
            log.warn("API 响应中没有 parts");
            return images;
        }
        
        for (JsonNode part : parts) {
            // 处理文本
            if (part.has("text")) {
                String text = part.get("text").asText();
                if (!text.isBlank()) {
                    log.debug("模型返回文本: {}", truncate(text, 200));
                }
            }
            
            // 处理图片
            if (part.has("inlineData")) {
                // 跳过思考过程中的临时图片
                if (part.has("thought") && part.get("thought").asBoolean()) {
                    log.debug("跳过思考过程中的临时图片");
                    continue;
                }
                
                JsonNode inlineData = part.get("inlineData");
                String mimeType = inlineData.has("mimeType") ? inlineData.get("mimeType").asText() : "image/png";
                String data = inlineData.has("data") ? inlineData.get("data").asText() : null;
                
                if (data != null && !data.isBlank()) {
                    images.add(ImageGenerationResult.GeneratedImage.builder()
                            .base64Data(data)
                            .mimeType(mimeType)
                            .build());
                    
                    log.debug("提取图片: mimeType={}", mimeType);
                }
            }
        }
        
        return images;
    }
    
    private boolean isBase64(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        try {
            return str.matches("^[A-Za-z0-9+/=]+$") && str.length() % 4 == 0 && str.length() > 100;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String determineMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }
}
