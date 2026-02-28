package com.anjing.aigc.provider;

import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.response.GenerationResult;

import java.util.concurrent.CompletableFuture;

/**
 * 内容生成提供商基础接口
 * 
 * <p>所有类型的内容生成提供商（图片、视频、音频、文本）都需要实现此接口。</p>
 * 
 * @author AI Team
 */
public interface ContentProvider {
    
    /**
     * 获取提供商名称
     * 
     * @return 提供商名称，如 "Google Nano Banana", "OpenAI DALL-E"
     */
    String getProviderName();
    
    /**
     * 检查提供商是否可用
     * 
     * @return true 如果配置完整且服务可用
     */
    boolean isAvailable();
    
    /**
     * 获取提供商类型
     * 
     * @return 提供商类型枚举
     */
    ProviderType getProviderType();
    
    /**
     * 同步执行内容生成
     * 
     * @param task AIGC任务
     * @return 生成结果
     */
    GenerationResult generate(AigcTask task);
    
    /**
     * 异步执行内容生成
     * 
     * @param task AIGC任务
     * @return 生成结果的Future
     */
    CompletableFuture<GenerationResult> generateAsync(AigcTask task);
    
    /**
     * 提供商类型枚举
     */
    enum ProviderType {
        /** Google (Gemini, Veo, Lyria) */
        GOOGLE,
        
        /** OpenAI (DALL-E, Sora) */
        OPENAI,
        
        /** Stability AI (Stable Diffusion) */
        STABILITY,
        
        /** 其他 */
        OTHER
    }
}
