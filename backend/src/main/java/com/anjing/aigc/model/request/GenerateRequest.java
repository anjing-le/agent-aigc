package com.anjing.aigc.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 生成请求
 * 
 * <p>设计理念：用户只需描述需求，系统自动处理一切</p>
 * <ul>
 *   <li>不需要选择模型 - Agent自动选择最优模型</li>
 *   <li>不需要调参 - Agent自动优化参数</li>
 *   <li>不需要关心技术细节 - 只需描述想要什么</li>
 * </ul>
 *
 * @author AIGC Team
 */
@Data
public class GenerateRequest {

    /** 
     * 用户输入的需求描述（必填）
     * 
     * <p>可以是任何形式的需求描述，例如：</p>
     * <ul>
     *   <li>文生图：一只可爱的橘猫在阳光下打盹</li>
     *   <li>文生视频：让樱花随风飘落的短视频</li>
     *   <li>图生图：把这张照片变成水彩画风格</li>
     *   <li>图生视频：让这张图片动起来</li>
     * </ul>
     */
    @NotBlank(message = "请描述你想要创作的内容")
    private String prompt;

    /**
     * 内容类型提示（可选）
     *
     * <p>为空时由 Agent 自动识别；有值时作为用户显式选择，Agent 仍负责 Prompt 优化和模型选择。</p>
     */
    private String contentTypeHint;

    /**
     * 生成参数（可选）
     *
     * <p>用于承载宽高比、尺寸、视频时长、音色等轻量参数。V1 前保持 Map，待真实 provider 链路稳定后再收敛为强类型 DTO。</p>
     */
    private Map<String, Object> generationParams;

    /** 
     * 参考素材URL列表（可选）
     * 
     * <p>支持图片/视频素材，用于：</p>
     * <ul>
     *   <li>图生图：基于图片生成新图片</li>
     *   <li>图生视频：将静态图片变成动态视频</li>
     *   <li>风格迁移：参考某种风格</li>
     * </ul>
     */
    private List<String> referenceImages;
}
