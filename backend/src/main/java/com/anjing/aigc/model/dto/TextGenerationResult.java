package com.anjing.aigc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文字生成结果 DTO
 * 
 * @author AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextGenerationResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 生成的文本
     */
    private String text;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 错误码
     */
    private String errorCode;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 输入token数
     */
    private int inputTokens;
    
    /**
     * 输出token数
     */
    private int outputTokens;
    
    /**
     * 耗时（毫秒）
     */
    private long durationMs;
    
    /**
     * 创建成功结果
     */
    public static TextGenerationResult success(String taskId, String text, String model, int inputTokens, int outputTokens, long durationMs) {
        return TextGenerationResult.builder()
                .success(true)
                .taskId(taskId)
                .text(text)
                .model(model)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .durationMs(durationMs)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static TextGenerationResult failure(String taskId, String errorCode, String errorMessage) {
        return TextGenerationResult.builder()
                .success(false)
                .taskId(taskId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}

