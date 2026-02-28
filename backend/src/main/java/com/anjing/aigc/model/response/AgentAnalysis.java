package com.anjing.aigc.model.response;

import com.anjing.aigc.model.dto.AnalyzedIntent;
import com.anjing.aigc.model.enums.ContentType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent分析结果
 * 
 * <p>包含智能路由Agent对用户请求的完整分析结果：</p>
 * <ul>
 *   <li>意图识别结果</li>
 *   <li>模型选择决策</li>
 *   <li>提示词优化过程</li>
 *   <li>提取的技术参数</li>
 * </ul>
 *
 * @author AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentAnalysis {

    /**
     * 识别的意图场景
     * - text_to_image: 文生图
     * - image_to_image: 图生图
     * - text_to_video: 文生视频
     * - image_to_video: 图生视频
     * - text_to_audio: 文生音频
     * - text_generation: 文本生成
     */
    private String intent;

    /**
     * 选择的内容类型
     */
    private ContentType contentType;

    /**
     * 选择的模型
     */
    private String selectedModel;

    /**
     * 用户原始提示词
     */
    private String originalPrompt;
    
    /**
     * 清洗后的提示词（去除技术参数描述）
     */
    private String cleanPrompt;

    /**
     * 优化后的提示词（增强质量）
     */
    private String optimizedPrompt;
    
    /**
     * 完整的意图分析结果（包含提取的技术参数）
     */
    private AnalyzedIntent analyzedIntent;
    
    /**
     * 分析置信度 (0-1)
     */
    private Double confidence;
    
    // ==================== 便捷方法 ====================
    
    /**
     * 获取图片参数（如果是图片类型）
     */
    public AnalyzedIntent.ImageParams getImageParams() {
        return analyzedIntent != null ? analyzedIntent.getEffectiveImageParams() : null;
    }
    
    /**
     * 获取视频参数（如果是视频类型）
     */
    public AnalyzedIntent.VideoParams getVideoParams() {
        return analyzedIntent != null ? analyzedIntent.getEffectiveVideoParams() : null;
    }
    
    /**
     * 获取音频参数（如果是音频类型）
     */
    public AnalyzedIntent.AudioParams getAudioParams() {
        return analyzedIntent != null ? analyzedIntent.getEffectiveAudioParams() : null;
    }
}
