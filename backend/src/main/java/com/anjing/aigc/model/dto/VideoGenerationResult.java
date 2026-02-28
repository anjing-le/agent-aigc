package com.anjing.aigc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频生成结果 DTO
 * 
 * @author AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenerationResult {
    
    /**
     * 任务状态
     */
    public enum Status {
        /**
         * 等待中
         */
        PENDING,
        /**
         * 生成中
         */
        PROCESSING,
        /**
         * 已完成
         */
        COMPLETED,
        /**
         * 失败
         */
        FAILED,
        /**
         * 已取消
         */
        CANCELLED
    }
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 任务状态
     */
    private Status status;
    
    /**
     * 进度百分比 (0-100)
     */
    private int progress;
    
    /**
     * 生成的视频信息
     */
    private GeneratedVideo video;
    
    /**
     * 错误信息（失败时）
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
     * 生成的视频信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedVideo {
        /**
         * 视频URL
         */
        private String url;
        
        /**
         * 缩略图URL
         */
        private String thumbnailUrl;
        
        /**
         * MIME类型
         */
        private String mimeType;
        
        /**
         * 视频时长（秒）
         */
        private int durationSeconds;
        
        /**
         * 视频宽度
         */
        private int width;
        
        /**
         * 视频高度
         */
        private int height;
        
        /**
         * 是否包含音频
         */
        private boolean hasAudio;
    }
    
    /**
     * 创建进行中结果
     */
    public static VideoGenerationResult processing(String taskId, int progress) {
        return VideoGenerationResult.builder()
                .taskId(taskId)
                .status(Status.PROCESSING)
                .progress(progress)
                .build();
    }
    
    /**
     * 创建成功结果
     */
    public static VideoGenerationResult success(String taskId, GeneratedVideo video, String model, String usedPrompt, long durationMs) {
        return VideoGenerationResult.builder()
                .taskId(taskId)
                .status(Status.COMPLETED)
                .progress(100)
                .video(video)
                .model(model)
                .usedPrompt(usedPrompt)
                .durationMs(durationMs)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static VideoGenerationResult failure(String taskId, String errorCode, String errorMessage) {
        return VideoGenerationResult.builder()
                .taskId(taskId)
                .status(Status.FAILED)
                .progress(0)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * 是否已完成（成功或失败）
     */
    public boolean isDone() {
        return status == Status.COMPLETED || status == Status.FAILED || status == Status.CANCELLED;
    }
}

