package com.anjing.aigc.model.dto;

import com.anjing.aigc.model.enums.ContentType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 意图分析结果 DTO
 * 
 * <p>由 IntentAnalyzer 使用LLM解析用户自然语言后生成的结构化数据。</p>
 * 
 * <h3>设计理念</h3>
 * <p>用户只需要用自然语言描述需求，系统自动提取：</p>
 * <ul>
 *   <li>内容类型（图片/视频/音频/文本）</li>
 *   <li>技术参数（分辨率、宽高比、时长等）</li>
 *   <li>风格偏好（艺术风格、质量要求）</li>
 * </ul>
 * 
 * <h3>示例</h3>
 * <pre>
 * 输入: "帮我生成一个4K的竖屏视频，一只猫在跳舞，要流畅一点，大概5秒就行"
 * 
 * 输出:
 * {
 *   "contentType": "VIDEO",
 *   "intent": "text_to_video",
 *   "cleanPrompt": "一只猫在跳舞",
 *   "videoParams": {
 *     "aspectRatio": "9:16",
 *     "resolution": "1080p",
 *     "duration": 6,
 *     "quality": "standard"
 *   }
 * }
 * </pre>
 * 
 * @author AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyzedIntent {
    
    /**
     * 内容类型
     */
    private ContentType contentType;
    
    /**
     * 意图场景
     * - text_generation: 文本生成
     * - text_to_image: 文生图
     * - image_to_image: 图生图
     * - text_to_video: 文生视频
     * - image_to_video: 图生视频
     * - text_to_audio: 文生音频
     */
    private String intent;
    
    /**
     * 用户原始输入
     */
    private String originalPrompt;
    
    /**
     * 清洗后的提示词（去除技术参数描述）
     */
    private String cleanPrompt;
    
    /**
     * 是否有参考图片
     */
    private boolean hasReferenceImage;
    
    /**
     * 图片生成参数
     */
    private ImageParams imageParams;
    
    /**
     * 视频生成参数
     */
    private VideoParams videoParams;
    
    /**
     * 音频生成参数
     */
    private AudioParams audioParams;
    
    /**
     * 质量增强建议（可选）
     */
    private List<String> enhancementSuggestions;
    
    /**
     * 解析置信度 (0-1)
     */
    private Double confidence;
    
    // ==================== 嵌套参数类 ====================
    
    /**
     * 图片参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageParams {
        
        /**
         * 宽高比
         * 可选: 1:1, 16:9, 9:16, 4:3, 3:4, 2:3, 3:2, 4:5, 5:4, 21:9
         */
        @Builder.Default
        private String aspectRatio = "16:9";
        
        /**
         * 图片尺寸
         * 可选: 1K, 2K, 4K (仅 Gemini 3 Pro 支持高于1K)
         */
        @Builder.Default
        private String imageSize = "1K";
        
        /**
         * 艺术风格
         * 如: photorealistic, anime, oil_painting, watercolor, sketch, 3d_render, pixel_art
         */
        private String style;
        
        /**
         * 是否需要透明背景（适用于贴纸、图标等）
         */
        private Boolean transparentBackground;
        
        /**
         * 创建默认参数
         */
        public static ImageParams createDefault() {
            return ImageParams.builder()
                    .aspectRatio("16:9")
                    .imageSize("1K")
                    .build();
        }
    }
    
    /**
     * 视频参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoParams {
        
        /**
         * 宽高比
         * 可选: 16:9 (横屏), 9:16 (竖屏)
         */
        @Builder.Default
        private String aspectRatio = "16:9";
        
        /**
         * 分辨率
         * 可选: 720p, 1080p
         */
        @Builder.Default
        private String resolution = "720p";
        
        /**
         * 视频时长（秒）
         * 可选: 4, 6, 8
         */
        @Builder.Default
        private Integer duration = 8;
        
        /**
         * 质量偏好
         * - fast: 快速生成，用于预览
         * - standard: 标准质量，用于最终输出
         */
        @Builder.Default
        private String quality = "standard";
        
        /**
         * 是否包含音频
         */
        @Builder.Default
        private Boolean withAudio = true;
        
        /**
         * 创建默认参数
         */
        public static VideoParams createDefault() {
            return VideoParams.builder()
                    .aspectRatio("16:9")
                    .resolution("720p")
                    .duration(8)
                    .quality("standard")
                    .withAudio(true)
                    .build();
        }
        
        /**
         * 根据质量偏好获取推荐的模型后缀
         * @return "fast" 或 ""
         */
        public String getModelSuffix() {
            return "fast".equals(quality) ? "-fast" : "";
        }
        
        /**
         * 规范化时长（就近取值）
         * Veo支持: 4, 6, 8 秒
         */
        public Integer getNormalizedDuration() {
            if (duration == null) return 8;
            if (duration <= 5) return 4;
            if (duration <= 7) return 6;
            return 8;
        }
    }
    
    /**
     * 音频参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AudioParams {
        
        /**
         * 音频类型
         * - tts: 语音合成（默认）
         * - music: 音乐生成（实验性）
         */
        @Builder.Default
        private String type = "tts";
        
        /**
         * TTS语音选择
         * - Kore: 清新、年轻（默认）
         * - Aoede: 温柔、优雅（女声）
         * - Fenrir: 深沉、有力（男声）
         * - Puck: 活泼、温暖
         * - Charon: 沉稳、权威
         */
        @Builder.Default
        private String voice = "Kore";
        
        /**
         * 节拍 (BPM, 60-200)，仅音乐生成时使用
         */
        @Builder.Default
        private Integer bpm = 90;
        
        /**
         * 情绪/风格
         * 如: happy, sad, energetic, calm, epic, romantic
         */
        private String mood;
        
        /**
         * 时长（秒）
         */
        private Integer duration;
        
        /**
         * 创建默认参数（TTS）
         */
        public static AudioParams createDefault() {
            return AudioParams.builder()
                    .type("tts")
                    .voice("Kore")
                    .build();
        }
        
        /**
         * 创建音乐生成参数
         */
        public static AudioParams createMusicParams(int bpm, String mood) {
            return AudioParams.builder()
                    .type("music")
                    .bpm(bpm)
                    .mood(mood)
                    .build();
        }
        
        /**
         * 是否为TTS类型
         */
        public boolean isTTS() {
            return "tts".equals(type);
        }
        
        /**
         * 是否为音乐类型
         */
        public boolean isMusic() {
            return "music".equals(type);
        }
    }
    
    // ==================== 便捷方法 ====================
    
    /**
     * 是否为图片相关意图
     */
    public boolean isImageIntent() {
        return contentType == ContentType.IMAGE;
    }
    
    /**
     * 是否为视频相关意图
     */
    public boolean isVideoIntent() {
        return contentType == ContentType.VIDEO;
    }
    
    /**
     * 是否为音频相关意图
     */
    public boolean isAudioIntent() {
        return contentType == ContentType.AUDIO;
    }
    
    /**
     * 是否为图生X（需要参考图片）
     */
    public boolean requiresReferenceImage() {
        return "image_to_image".equals(intent) || "image_to_video".equals(intent);
    }
    
    /**
     * 获取有效的图片参数（确保非空）
     */
    public ImageParams getEffectiveImageParams() {
        return imageParams != null ? imageParams : ImageParams.createDefault();
    }
    
    /**
     * 获取有效的视频参数（确保非空）
     */
    public VideoParams getEffectiveVideoParams() {
        return videoParams != null ? videoParams : VideoParams.createDefault();
    }
    
    /**
     * 获取有效的音频参数（确保非空）
     */
    public AudioParams getEffectiveAudioParams() {
        return audioParams != null ? audioParams : AudioParams.createDefault();
    }
}

