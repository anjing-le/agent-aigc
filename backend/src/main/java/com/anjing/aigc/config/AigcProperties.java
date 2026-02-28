package com.anjing.aigc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AIGC 模块配置属性
 * 
 * 设计理念：
 * 1. 按内容类型（文字/图片/视频/音频）分为4条路由
 * 2. 每条路由可配置多个提供商，通过开关决定使用哪个
 * 3. Google GenAI SDK 作为主要提供商
 * 4. 其他提供商作为扩展预留
 * 
 * @author AI Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "aigc")
public class AigcProperties {
    
    /**
     * 提供商凭证配置
     */
    private ProvidersConfig providers = new ProvidersConfig();
    
    /**
     * 图片生成路由配置
     */
    private ImageRouteConfig image = new ImageRouteConfig();
    
    /**
     * 视频生成路由配置
     */
    private VideoRouteConfig video = new VideoRouteConfig();
    
    /**
     * 音频生成路由配置
     */
    private AudioRouteConfig audio = new AudioRouteConfig();
    
    // ==================== 提供商凭证配置 ====================
    
    @Data
    public static class ProvidersConfig {
        /**
         * Google GenAI 配置（用于图片/视频/音频生成）
         */
        private GoogleProviderConfig google = new GoogleProviderConfig();
        
        /**
         * OneRouter 配置（用于 Agent 智能路由，便宜的 LLM）
         */
        private OneRouterConfig onerouter = new OneRouterConfig();
    }
    
    @Data
    public static class GoogleProviderConfig {
        private boolean enabled = true;
        private String apiKey;
        
        /**
         * Google API Base URL
         */
        private String baseUrl = "https://generativelanguage.googleapis.com";
        
        /**
         * 代理主机（用于访问 Google API）
         */
        private String proxyHost;
        
        /**
         * 代理端口
         */
        private Integer proxyPort;
    }
    
    @Data
    public static class OneRouterConfig {
        private boolean enabled = true;
        /**
         * OneRouter API Key
         */
        private String apiKey;
        /**
         * OneRouter API 地址（OpenAI 兼容格式）
         */
        private String apiUrl = "https://llm.onerouter.pro/v1";
        /**
         * 用于意图分析的模型（选便宜的）
         * 推荐: gpt-4o-mini ($0.15/$0.60 per 1M tokens)
         */
        private String model = "gpt-4o-mini";
        /**
         * 超时时间（毫秒）
         */
        private int timeout = 30000;
        /**
         * 温度（0-1，越低越稳定）
         */
        private double temperature = 0.1;
    }
    
    // ==================== 图片生成路由配置 ====================
    
    @Data
    public static class ImageRouteConfig {
        /**
         * 当前激活的提供商
         */
        private String activeProvider = "google";
        
        /**
         * Google Nano Banana 配置
         */
        private ImageModelConfig google = new ImageModelConfig();
        
        /**
         * 其他提供商配置（预留）
         */
        private Map<String, ImageModelConfig> others = new HashMap<>();
    }
    
    @Data
    public static class ImageModelConfig {
        private boolean enabled = true;
        
        /**
         * 模型名称
         * - gemini-2.5-flash-image (Nano Banana) - 快速, 1024px
         * - gemini-3-pro-image-preview (Nano Banana Pro) - 高质量, 最高4K
         */
        private String model = "gemini-2.5-flash-image";
        
        private int timeout = 60000;
        
        /**
         * 默认宽高比
         * 可选: 1:1, 2:3, 3:2, 3:4, 4:3, 4:5, 5:4, 9:16, 16:9, 21:9
         */
        private String defaultAspectRatio = "16:9";
        
        /**
         * 默认图片尺寸（仅 gemini-3-pro-image-preview 支持）
         * 可选: 1K, 2K, 4K
         * 注意：必须使用大写K
         */
        private String defaultImageSize = "1K";
        
        /**
         * 是否使用思考模式（gemini-3-pro 默认开启）
         * 模型会生成临时"构思图片"来优化构图
         */
        private boolean enableThinking = true;
    }
    
    // ==================== 视频生成路由配置 ====================
    
    @Data
    public static class VideoRouteConfig {
        /**
         * 当前激活的提供商
         */
        private String activeProvider = "google";
        
        /**
         * Google Veo 配置
         */
        private VideoModelConfig google = new VideoModelConfig();
        
        /**
         * Sora 配置（预留）
         */
        private SoraConfig sora = new SoraConfig();
    }
    
    @Data
    public static class VideoModelConfig {
        private boolean enabled = true;
        /**
         * 模型名称
         * veo-3.1-generate-preview: 最新版本，支持音频
         * veo-3.1-fast-generate-preview: 快速版本
         * veo-3-generate-preview: 稳定版本
         */
        private String model = "veo-3.1-generate-preview";
        private int timeout = 360000; // 6分钟
        /**
         * 默认宽高比: 16:9, 9:16
         */
        private String defaultAspectRatio = "16:9";
        /**
         * 默认分辨率: 720p, 1080p
         */
        private String defaultResolution = "720p";
        /**
         * 默认视频时长（秒）: 4, 6, 8
         */
        private int defaultDuration = 8;
        /**
         * 是否生成音频（Veo 3.1 支持）
         */
        private boolean generateAudio = true;
    }
    
    @Data
    public static class SoraConfig {
        private boolean enabled = false;
        private String apiKey;
        private String model = "sora-2";
        private int timeout = 360000;
    }
    
    // ==================== 音频生成路由配置 ====================
    
    @Data
    public static class AudioRouteConfig {
        /**
         * 当前激活的提供商
         */
        private String activeProvider = "google";
        
        /**
         * Google Lyria 配置
         */
        private AudioModelConfig google = new AudioModelConfig();
        
        /**
         * Suno 配置（预留）
         */
        private SunoConfig suno = new SunoConfig();
    }
    
    @Data
    public static class AudioModelConfig {
        private boolean enabled = true;
        
        /**
         * 模型名称
         * - gemini-2.5-flash-preview-tts: TTS语音合成（推荐）
         * - gemini-2.5-pro-preview-tts: 高质量TTS
         * - lyria-realtime-exp: 实验性实时音乐生成
         */
        private String model = "gemini-2.5-flash-preview-tts";
        private int timeout = 60000;
        
        /**
         * 默认语音
         * 可选: Kore, Aoede, Fenrir, Puck, Charon
         */
        private String defaultVoice = "Kore";
        
        /**
         * 默认 BPM (60-200)，仅音乐生成
         */
        private int defaultBpm = 90;
        
        /**
         * 默认温度 (0.0-3.0)
         */
        private double defaultTemperature = 1.0;
    }
    
    @Data
    public static class SunoConfig {
        private boolean enabled = false;
        private String apiKey;
        private String model = "suno-v3";
        private int timeout = 60000;
    }
    
    // ==================== 存储配置 ====================
    
    /**
     * 存储配置
     */
    private StorageConfig storage = new StorageConfig();
    
    @Data
    public static class StorageConfig {
        /**
         * 本地存储配置
         */
        private LocalStorageConfig local = new LocalStorageConfig();
        
        /**
         * OSS配置
         */
        private OssConfig oss = new OssConfig();
    }
    
    @Data
    public static class LocalStorageConfig {
        private boolean enabled = true;
        private String basePath = "./uploads";
        private String urlPrefix = "http://localhost:10003/files";
    }
    
    @Data
    public static class OssConfig {
        private boolean enabled = false;
        private String provider = "aliyun";
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
        private String cdnDomain;
    }
    
    // ==================== 便捷方法 ====================
    
    /**
     * 检查 Google 提供商是否配置完成
     */
    public boolean isGoogleConfigured() {
        return providers.getGoogle() != null 
            && providers.getGoogle().isEnabled()
            && providers.getGoogle().getApiKey() != null 
            && !providers.getGoogle().getApiKey().isBlank()
            && !providers.getGoogle().getApiKey().startsWith("<");
    }
    
    /**
     * 获取当前激活的图片生成提供商
     */
    public String getActiveImageProvider() {
        return image.getActiveProvider();
    }
    
    /**
     * 获取当前激活的视频生成提供商
     */
    public String getActiveVideoProvider() {
        return video.getActiveProvider();
    }
    
    /**
     * 获取当前激活的音频生成提供商
     */
    public String getActiveAudioProvider() {
        return audio.getActiveProvider();
    }
    
    /**
     * 检查 OneRouter 是否配置完成（用于 Agent 意图分析）
     */
    public boolean isOneRouterConfigured() {
        return providers.getOnerouter() != null
            && providers.getOnerouter().isEnabled()
            && providers.getOnerouter().getApiKey() != null
            && !providers.getOnerouter().getApiKey().isBlank()
            && !providers.getOnerouter().getApiKey().startsWith("<");
    }
}
