package com.anjing.aigc.agent;

import com.anjing.aigc.model.enums.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 提示词增强器
 * 
 * <h3>功能</h3>
 * <p>根据内容类型自动增强用户提示词，提高生成质量：</p>
 * <ul>
 *   <li>图片 - 添加质量修饰词、光照、细节描述</li>
 *   <li>视频 - 添加运动流畅度、电影感、帧率相关描述</li>
 *   <li>音频 - 添加音质、乐器、氛围描述</li>
 * </ul>
 * 
 * <h3>设计原则</h3>
 * <ul>
 *   <li>增强但不改变原意</li>
 *   <li>避免重复添加已有的修饰词</li>
 *   <li>保持提示词的自然流畅</li>
 * </ul>
 * 
 * @author AI Team
 */
@Slf4j
@Component
public class PromptEnhancer {
    
    /**
     * 增强提示词
     * 
     * @param cleanPrompt 清洗后的用户提示词
     * @param contentType 内容类型
     * @param hasReference 是否有参考素材
     * @return 增强后的提示词
     */
    public String enhance(String cleanPrompt, ContentType contentType, boolean hasReference) {
        if (cleanPrompt == null || cleanPrompt.isBlank()) {
            return cleanPrompt;
        }
        
        String enhanced = switch (contentType) {
            case TEXT -> enhanceTextPrompt(cleanPrompt);
            case IMAGE -> enhanceImagePrompt(cleanPrompt, hasReference);
            case VIDEO -> enhanceVideoPrompt(cleanPrompt, hasReference);
            case AUDIO -> enhanceAudioPrompt(cleanPrompt);
        };
        
        if (!enhanced.equals(cleanPrompt)) {
            log.debug("提示词增强: {} -> {}", 
                    truncate(cleanPrompt, 50), 
                    truncate(enhanced, 80));
        }
        
        return enhanced;
    }
    
    /**
     * 增强文本提示词
     */
    private String enhanceTextPrompt(String prompt) {
        // 文本生成一般不需要过多增强，保持原样
        return prompt;
    }
    
    /**
     * 增强图片提示词
     */
    private String enhanceImagePrompt(String prompt, boolean hasReference) {
        StringBuilder enhanced = new StringBuilder(prompt.trim());
        String lower = prompt.toLowerCase();
        
        // 检查是否已有质量描述
        boolean hasQuality = containsAny(lower, 
                "高质量", "high quality", "detailed", "精细", "8k", "4k", "hd", "高清");
        
        // 检查是否已有光照描述
        boolean hasLighting = containsAny(lower,
                "光", "light", "照明", "阳光", "sunlight", "golden hour", "柔光");
        
        // 添加质量增强
        if (!hasQuality) {
            enhanced.append(", high quality, detailed");
        }
        
        // 图生图场景：保持原始构图
        if (hasReference) {
            if (!containsAny(lower, "保持", "maintain", "keep", "原始", "original")) {
                enhanced.append(", maintain the original composition and style");
            }
        }
        
        return enhanced.toString();
    }
    
    /**
     * 增强视频提示词
     */
    private String enhanceVideoPrompt(String prompt, boolean hasReference) {
        StringBuilder enhanced = new StringBuilder(prompt.trim());
        String lower = prompt.toLowerCase();
        
        // 检查是否已有运动描述
        boolean hasMotion = containsAny(lower,
                "流畅", "smooth", "自然", "natural", "motion", "运动", "动作");
        
        // 检查是否已有电影感描述
        boolean hasCinematic = containsAny(lower,
                "电影", "cinematic", "film", "影视", "大片");
        
        // 添加视频质量增强
        if (!hasMotion) {
            enhanced.append(", smooth motion");
        }
        
        if (!hasCinematic && !hasMotion) {
            enhanced.append(", cinematic quality");
        }
        
        // 图生视频场景：强调自然过渡
        if (hasReference) {
            if (!containsAny(lower, "动起来", "animate", "动画化", "活起来")) {
                enhanced.append(", natural and seamless animation");
            }
        }
        
        return enhanced.toString();
    }
    
    /**
     * 增强音频提示词
     */
    private String enhanceAudioPrompt(String prompt) {
        StringBuilder enhanced = new StringBuilder(prompt.trim());
        String lower = prompt.toLowerCase();
        
        // 检查是否已有音质描述
        boolean hasQuality = containsAny(lower,
                "高音质", "high quality", "清晰", "clear", "professional");
        
        if (!hasQuality) {
            enhanced.append(", high quality audio");
        }
        
        return enhanced.toString();
    }
    
    /**
     * 检查字符串是否包含任意关键词
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }
}

