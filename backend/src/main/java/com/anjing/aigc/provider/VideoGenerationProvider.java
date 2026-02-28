package com.anjing.aigc.provider;

/**
 * 视频生成提供商接口
 * 
 * @author AI Team
 */
public interface VideoGenerationProvider extends ContentProvider {
    
    /**
     * 是否支持图生视频（Image to Video）
     */
    default boolean supportsImageToVideo() {
        return false;
    }
    
    /**
     * 是否支持生成带音频的视频
     */
    default boolean supportsAudioGeneration() {
        return false;
    }
}
