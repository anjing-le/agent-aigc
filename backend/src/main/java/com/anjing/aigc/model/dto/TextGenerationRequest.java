package com.anjing.aigc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 文字生成请求 DTO
 * 
 * 主要用途：
 * 1. 提示词优化
 * 2. 意图识别
 * 3. 对话生成
 * 
 * @author AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextGenerationRequest {
    
    /**
     * 用途类型
     */
    public enum Purpose {
        /**
         * 提示词优化
         */
        PROMPT_OPTIMIZATION,
        /**
         * 意图识别
         */
        INTENT_RECOGNITION,
        /**
         * 对话生成
         */
        CONVERSATION,
        /**
         * 通用生成
         */
        GENERAL
    }
    
    /**
     * 用途
     */
    @Builder.Default
    private Purpose purpose = Purpose.GENERAL;
    
    /**
     * 用户输入
     */
    private String prompt;
    
    /**
     * 系统提示（角色设定）
     */
    private String systemPrompt;
    
    /**
     * 对话历史
     */
    private List<Message> conversationHistory;
    
    /**
     * 温度 (0.0-2.0)
     */
    @Builder.Default
    private double temperature = 0.7;
    
    /**
     * 最大token数
     */
    @Builder.Default
    private int maxTokens = 4096;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;
    
    /**
     * 对话消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /**
         * 角色: user, assistant, system
         */
        private String role;
        
        /**
         * 内容
         */
        private String content;
    }
}

