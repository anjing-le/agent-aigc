package com.anjing.aigc.provider;

import com.anjing.aigc.model.dto.ImageGenerationRequest;
import com.anjing.aigc.model.dto.ImageGenerationResult;

import java.util.concurrent.CompletableFuture;

/**
 * 图片生成提供商接口
 * 
 * @author AI Team
 */
public interface ImageGenerationProvider extends ContentProvider {
    
    /**
     * 是否支持图生图（Image to Image）
     */
    default boolean supportsImageToImage() {
        return false;
    }
    
    /**
     * 是否支持图片编辑（局部重绘、风格迁移等）
     */
    default boolean supportsImageEditing() {
        return false;
    }
    
    /**
     * 使用详细请求参数生成图片
     * 
     * @param request 图片生成请求
     * @return 生成结果
     */
    default ImageGenerationResult generate(ImageGenerationRequest request) {
        throw new UnsupportedOperationException("此提供商不支持 ImageGenerationRequest");
    }
    
    /**
     * 异步生成图片
     * 
     * @param request 图片生成请求
     * @return 生成结果的Future
     */
    default CompletableFuture<ImageGenerationResult> generateAsync(ImageGenerationRequest request) {
        return CompletableFuture.supplyAsync(() -> generate(request));
    }
}
