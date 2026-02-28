package com.anjing.aigc.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 图片生成结果 DTO
 * 
 * @author AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageGenerationResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 生成的图片列表
     */
    private List<GeneratedImage> images;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 使用的提示词
     */
    private String prompt;
    
    /**
     * 处理耗时（毫秒）
     */
    private Long processingTimeMs;
    
    /**
     * 错误码
     */
    private String errorCode;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 生成的图片
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedImage {
        
        /**
         * 图片URL（如果已上传到存储）
         */
        private String url;
        
        /**
         * 缩略图URL
         */
        private String thumbnailUrl;
        
        /**
         * Base64编码的图片数据
         */
        private String base64Data;
        
        /**
         * MIME类型
         */
        private String mimeType;
        
        /**
         * 图片宽度
         */
        private Integer width;
        
        /**
         * 图片高度
         */
        private Integer height;
    }
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 创建成功结果
     */
    public static ImageGenerationResult success(String taskId, List<GeneratedImage> images, 
            String model, String prompt, long processingTimeMs) {
        return ImageGenerationResult.builder()
                .success(true)
                .taskId(taskId)
                .images(images)
                .model(model)
                .prompt(prompt)
                .processingTimeMs(processingTimeMs)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static ImageGenerationResult failure(String taskId, String errorCode, String errorMessage) {
        return ImageGenerationResult.builder()
                .success(false)
                .taskId(taskId)
                .images(Collections.emptyList())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * 获取第一张图片（如果有）
     */
    public GeneratedImage getFirstImage() {
        return images != null && !images.isEmpty() ? images.get(0) : null;
    }
    
    /**
     * 获取第一张图片的URL
     */
    public String getFirstImageUrl() {
        GeneratedImage first = getFirstImage();
        return first != null ? first.getUrl() : null;
    }
}
