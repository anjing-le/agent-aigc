package com.anjing.aigc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 音频生成结果 DTO
 * 
 * @author AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioGenerationResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 生成的音频信息
     */
    private GeneratedAudio audio;
    
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
     * 使用的提示词
     */
    private String usedPrompt;
    
    /**
     * 耗时（毫秒）
     */
    private long durationMs;
    
    /**
     * 生成的音频信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedAudio {
        /**
         * 音频URL
         */
        private String url;
        
        /**
         * 音频Base64数据
         */
        private String base64Data;
        
        /**
         * MIME类型
         */
        private String mimeType;
        
        /**
         * 音频时长（秒）
         */
        private int durationSeconds;
        
        /**
         * 采样率
         */
        private int sampleRate;
        
        /**
         * 声道数
         */
        private int channels;
    }
    
    /**
     * 创建成功结果
     */
    public static AudioGenerationResult success(String taskId, GeneratedAudio audio, String model, String usedPrompt, long durationMs) {
        return AudioGenerationResult.builder()
                .success(true)
                .taskId(taskId)
                .audio(audio)
                .model(model)
                .usedPrompt(usedPrompt)
                .durationMs(durationMs)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static AudioGenerationResult failure(String taskId, String errorCode, String errorMessage) {
        return AudioGenerationResult.builder()
                .success(false)
                .taskId(taskId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}

