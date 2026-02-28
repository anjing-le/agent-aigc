package com.anjing.aigc.provider;

/**
 * 音频生成提供商接口
 * 
 * @author AI Team
 */
public interface AudioGenerationProvider extends ContentProvider {
    
    /**
     * 是否支持音乐生成
     */
    default boolean supportsMusicGeneration() {
        return false;
    }
    
    /**
     * 是否支持TTS（文字转语音）
     */
    default boolean supportsTTS() {
        return false;
    }
}
