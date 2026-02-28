package com.anjing.aigc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 音频/音乐生成请求 DTO
 * 
 * 支持的模式：
 * 1. 音乐生成 - Lyria RealTime (实时流式音乐)
 * 2. TTS - 文字转语音
 * 3. 音乐生成 - Suno 等
 * 
 * @author AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioGenerationRequest {
    
    /**
     * 生成模式
     */
    public enum Mode {
        /**
         * 音乐生成
         */
        MUSIC,
        /**
         * 文字转语音
         */
        TTS,
        /**
         * 声音效果
         */
        SOUND_EFFECT
    }
    
    /**
     * 生成模式
     */
    @Builder.Default
    private Mode mode = Mode.MUSIC;
    
    /**
     * 用户提示词（描述想要的音乐/音效）
     */
    private String prompt;
    
    /**
     * 优化后的提示词
     */
    private String optimizedPrompt;
    
    // ==================== 音乐生成参数（Lyria） ====================
    
    /**
     * 带权重的提示词列表
     * 格式: [{"text": "Piano", "weight": 2.0}, ...]
     */
    private List<WeightedPrompt> weightedPrompts;
    
    /**
     * 每分钟节拍数 (60-200)
     */
    @Builder.Default
    private int bpm = 90;
    
    /**
     * 温度 (0.0-3.0)
     */
    @Builder.Default
    private double temperature = 1.0;
    
    /**
     * 音符密度 (0.0-1.0)
     */
    @Builder.Default
    private double density = 0.5;
    
    /**
     * 亮度 (0.0-1.0)
     */
    @Builder.Default
    private double brightness = 0.5;
    
    /**
     * 音阶
     */
    private String scale;
    
    /**
     * 是否静音贝斯
     */
    @Builder.Default
    private boolean muteBass = false;
    
    /**
     * 是否静音鼓
     */
    @Builder.Default
    private boolean muteDrums = false;
    
    // ==================== TTS 参数 ====================
    
    /**
     * TTS 文本内容
     */
    private String ttsText;
    
    /**
     * 语音类型/角色
     */
    private String voiceId;
    
    /**
     * 语言代码
     */
    @Builder.Default
    private String language = "zh-CN";
    
    // ==================== 元数据 ====================
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 生成时长（秒）
     */
    @Builder.Default
    private int durationSeconds = 30;
    
    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;
    
    /**
     * 带权重的提示词
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeightedPrompt {
        private String text;
        @Builder.Default
        private double weight = 1.0;
    }
}

