package com.anjing.aigc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 视频生成请求 DTO
 * 
 * 支持的模式：
 * 1. 文生视频 - 仅提供 prompt
 * 2. 图生视频 - 提供 prompt + firstFrameImage
 * 3. 插帧生成 - 提供 prompt + firstFrameImage + lastFrameImage
 * 4. 视频延长 - 提供 prompt + sourceVideoUrl
 * 5. 参考图片 - 提供 prompt + referenceImages (最多3张)
 * 
 * @author AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenerationRequest {
    
    /**
     * 用户原始提示词
     */
    private String prompt;
    
    /**
     * 优化后的提示词（由Agent处理）
     */
    private String optimizedPrompt;
    
    /**
     * 负面提示词
     */
    private String negativePrompt;
    
    // ==================== 图生视频相关 ====================
    
    /**
     * 首帧图片URL（图生视频、插帧生成）
     */
    private String firstFrameImage;
    
    /**
     * 末帧图片URL（插帧生成，Veo 3.1 支持）
     */
    private String lastFrameImage;
    
    /**
     * 参考图片URL列表（Veo 3.1 支持，最多3张）
     * 用于指导视频内容的风格和主题
     */
    private List<String> referenceImages;
    
    // ==================== 视频延长相关 ====================
    
    /**
     * 源视频URL（视频延长）
     */
    private String sourceVideoUrl;
    
    // ==================== 视频参数 ====================
    
    /**
     * 宽高比: 16:9, 9:16
     */
    @Builder.Default
    private String aspectRatio = "16:9";
    
    /**
     * 分辨率: 720p, 1080p
     */
    @Builder.Default
    private String resolution = "720p";
    
    /**
     * 视频时长（秒）: 4, 6, 8
     */
    @Builder.Default
    private int durationSeconds = 8;
    
    /**
     * 是否生成音频（Veo 3.1 支持）
     */
    @Builder.Default
    private boolean generateAudio = true;
    
    /**
     * 人物生成控制
     * allow_all: 允许所有
     * allow_adult: 仅允许成人
     * dont_allow: 不允许
     */
    @Builder.Default
    private String personGeneration = "allow_all";
    
    // ==================== 元数据 ====================
    
    /**
     * 任务ID（用于追踪）
     */
    private String taskId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 额外参数（提供商特定）
     */
    private Map<String, Object> extraParams;
    
    // ==================== 便捷方法 ====================
    
    /**
     * 是否为图生视频模式
     */
    public boolean isImageToVideoMode() {
        return firstFrameImage != null && !firstFrameImage.isBlank();
    }
    
    /**
     * 是否为插帧模式
     */
    public boolean isInterpolationMode() {
        return firstFrameImage != null && lastFrameImage != null;
    }
    
    /**
     * 是否为视频延长模式
     */
    public boolean isExtensionMode() {
        return sourceVideoUrl != null && !sourceVideoUrl.isBlank();
    }
    
    /**
     * 是否有参考图片
     */
    public boolean hasReferenceImages() {
        return referenceImages != null && !referenceImages.isEmpty();
    }
}

