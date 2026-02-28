package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 通用生成结果
 * 
 * <p>适用于所有类型的内容生成（图片、视频、音频、文本）。</p>
 *
 * @author AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerationResult {

    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 资产ID（持久化后的ID）
     */
    private String assetId;

    /**
     * 内容类型
     */
    private ContentType contentType;

    /**
     * 资源URL
     */
    private String url;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 使用的提示词
     */
    private String prompt;

    /**
     * 使用的模型
     */
    private String model;
    
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
     * 元数据（扩展信息）
     * 
     * 不同类型的内容可能包含不同的元数据：
     * - 图片: width, height, format
     * - 视频: duration, fps, resolution
     * - 音频: duration, sampleRate
     * - 文本: text, charCount, wordCount
     */
    private Map<String, Object> metadata;
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 创建失败结果
     */
    public static GenerationResult failure(String taskId, String errorCode, String errorMessage) {
        return GenerationResult.builder()
                .success(false)
                .taskId(taskId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return success && errorCode == null;
    }
    
    /**
     * 判断是否失败
     */
    public boolean isFailed() {
        return !success || errorCode != null;
    }
}
