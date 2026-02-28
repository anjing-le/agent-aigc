package com.anjing.aigc.controller;

import com.anjing.aigc.model.request.GenerateRequest;
import com.anjing.aigc.model.response.GenerateResponse;
import com.anjing.aigc.model.response.ModelListResponse;
import com.anjing.aigc.model.response.TaskStatusResponse;
import com.anjing.aigc.service.AigcService;
import com.anjing.model.response.APIResponse;
import com.anjing.model.response.PageResponse;
import com.anjing.aigc.model.dto.AssetDTO;
import com.anjing.aigc.model.dto.GalleryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * AIGC 创作工坊控制器
 * 
 * <p>核心接口入口，负责处理所有AIGC相关请求</p>
 * 
 * <h3>功能模块：</h3>
 * <ul>
 *   <li>智能路由Agent - 意图识别、模型选择、参数优化</li>
 *   <li>图片生成 - Nano Banana模型</li>
 *   <li>视频生成 - Sora-2模型</li>
 *   <li>音频生成 - TODO: 预留扩展</li>
 * </ul>
 *
 * @author AIGC Team
 */
@RestController
@RequestMapping("/api/aigc")
@RequiredArgsConstructor
@Slf4j
public class AigcController {

    private final AigcService aigcService;

    /**
     * 智能生成接口 - Agent核心入口
     * 
     * <p>通过智能路由Agent自动完成：</p>
     * <ul>
     *   <li>意图识别 - 分析用户输入，理解创作需求</li>
     *   <li>模型选择 - 根据任务类型选择最优模型</li>
     *   <li>参数优化 - 自动补全和优化提示词</li>
     *   <li>任务调度 - 异步执行生成任务</li>
     * </ul>
     *
     * @param request 生成请求
     * @return 生成响应，包含任务ID和Agent分析结果
     */
    @PostMapping("/generate")
    public APIResponse<GenerateResponse> generate(@Valid @RequestBody GenerateRequest request) {
        log.info("接收到生成请求: prompt={}", request.getPrompt());
        GenerateResponse response = aigcService.generate(request);
        return APIResponse.success(response);
    }

    /**
     * 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    @GetMapping("/task/{taskId}")
    public APIResponse<TaskStatusResponse> getTaskStatus(@PathVariable String taskId) {
        TaskStatusResponse status = aigcService.getTaskStatus(taskId);
        return APIResponse.success(status);
    }

    /**
     * 获取可用模型列表
     *
     * @return 模型列表，按类型分组
     */
    @GetMapping("/models")
    public APIResponse<ModelListResponse> getModels() {
        ModelListResponse models = aigcService.getAvailableModels();
        return APIResponse.success(models);
    }

    /**
     * 获取灵感广场作品列表
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param contentType 内容类型筛选
     * @param model 模型筛选
     * @param keyword 关键词搜索
     * @return 作品分页列表
     */
    @GetMapping("/gallery")
    public APIResponse<PageResponse<GalleryDTO>> getGalleryList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String keyword) {
        PageResponse<GalleryDTO> gallery = aigcService.getGalleryList(current, size, contentType, model, keyword);
        return APIResponse.success(gallery);
    }

    /**
     * 保存作品到灵感广场
     *
     * @param body 请求体，包含 assetId
     * @return 操作结果
     */
    @PostMapping("/gallery/save")
    public APIResponse<Void> saveToGallery(@RequestBody java.util.Map<String, String> body) {
        String assetId = body.get("assetId");
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId 不能为空");
        }
        aigcService.saveToGallery(assetId);
        return APIResponse.success(null);
    }

    /**
     * 获取我的资产列表
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param contentType 内容类型筛选
     * @return 资产分页列表
     */
    @GetMapping("/assets")
    public APIResponse<PageResponse<AssetDTO>> getAssetList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String contentType) {
        PageResponse<AssetDTO> assets = aigcService.getAssetList(current, size, contentType);
        return APIResponse.success(assets);
    }

    /**
     * 删除资产
     *
     * @param assetId 资产ID
     * @return 操作结果
     */
    @DeleteMapping("/assets/{assetId}")
    public APIResponse<Void> deleteAsset(@PathVariable String assetId) {
        aigcService.deleteAsset(assetId);
        return APIResponse.success(null);
    }
}

