package com.anjing.aigc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 图片生成请求 DTO
 * 
 * <h3>支持的模式</h3>
 * <ul>
 *   <li>文生图 (Text to Image) - 仅提供 prompt</li>
 *   <li>图生图 (Image to Image) - 提供 prompt + referenceImages</li>
 *   <li>多图合成 - 提供 prompt + 多张 referenceImages (最多14张)</li>
 *   <li>风格迁移 - 提供原图 + 风格描述</li>
 *   <li>局部重绘 - 提供原图 + 区域描述</li>
 * </ul>
 * 
 * <h3>支持的模型</h3>
 * <ul>
 *   <li>gemini-2.5-flash-image (Nano Banana) - 快速, 1024px</li>
 *   <li>gemini-3-pro-image-preview (Nano Banana Pro) - 高质量, 最高4K</li>
 * </ul>
 * 
 * @author AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationRequest {
    
    /**
     * 用户原始提示词
     */
    private String prompt;
    
    /**
     * 优化后的提示词（由Agent处理）
     */
    private String optimizedPrompt;
    
    /**
     * 负面提示词（不希望出现的内容）
     * 建议使用正面描述而非"没有xxx"
     */
    private String negativePrompt;
    
    /**
     * 参考图片列表（支持Base64或URL）
     * 
     * 格式支持：
     * - Base64: "data:image/jpeg;base64,..."
     * - 纯Base64: "..."
     * - 本地路径: "/path/to/image.jpg"
     * 
     * 数量限制：
     * - gemini-2.5-flash-image: 最多3张
     * - gemini-3-pro-image-preview: 最多14张（6张高保真物体 + 5张人物）
     */
    private List<String> referenceImages;
    
    /**
     * 宽高比
     * 
     * 可选值: 1:1, 2:3, 3:2, 3:4, 4:3, 4:5, 5:4, 9:16, 16:9, 21:9
     */
    @Builder.Default
    private String aspectRatio = "16:9";
    
    /**
     * 图片尺寸/分辨率（仅 Gemini 3 Pro 支持）
     * 
     * 可选值: 1K, 2K, 4K
     * 注意：必须使用大写K
     */
    @Builder.Default
    private String imageSize = "1K";
    
    /**
     * 生成数量
     * 注意：当前API一次只能生成1张图片
     */
    @Builder.Default
    private int numberOfImages = 1;
    
    /**
     * 响应模式
     * - TEXT_AND_IMAGE: 返回文本和图片（默认）
     * - IMAGE_ONLY: 仅返回图片
     */
    @Builder.Default
    private ResponseModality responseModality = ResponseModality.TEXT_AND_IMAGE;
    
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
    
    /**
     * 响应模式枚举
     */
    public enum ResponseModality {
        /**
         * 返回文本和图片
         */
        TEXT_AND_IMAGE,
        
        /**
         * 仅返回图片
         */
        IMAGE_ONLY
    }
    
    // ==================== 便捷方法 ====================
    
    /**
     * 是否为图生图模式
     */
    public boolean isImageToImageMode() {
        return referenceImages != null && !referenceImages.isEmpty();
    }
    
    /**
     * 是否为多图合成模式
     */
    public boolean isMultiImageMode() {
        return referenceImages != null && referenceImages.size() > 1;
    }
    
    /**
     * 获取参考图片数量
     */
    public int getReferenceImageCount() {
        return referenceImages != null ? referenceImages.size() : 0;
    }
}
