package com.anjing.aigc.service;

import com.anjing.aigc.model.dto.AssetDTO;
import com.anjing.aigc.model.dto.GalleryDTO;
import com.anjing.aigc.model.request.GenerateRequest;
import com.anjing.aigc.model.response.GenerateResponse;
import com.anjing.aigc.model.response.ModelListResponse;
import com.anjing.aigc.model.response.TaskStatusResponse;
import com.anjing.model.response.PageResponse;

/**
 * AIGC 服务接口
 * 
 * <p>核心业务逻辑接口，定义AIGC创作工坊的所有功能</p>
 *
 * @author AIGC Team
 */
public interface AigcService {

    /**
     * 智能生成 - Agent核心入口
     *
     * @param request 生成请求
     * @return 生成响应
     */
    GenerateResponse generate(GenerateRequest request);

    /**
     * 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    TaskStatusResponse getTaskStatus(String taskId);

    /**
     * 获取可用模型列表
     *
     * @return 模型列表
     */
    ModelListResponse getAvailableModels();

    /**
     * 获取灵感广场作品列表
     *
     * @param current 当前页
     * @param size 每页大小
     * @param contentType 内容类型
     * @param model 模型
     * @param keyword 关键词
     * @return 分页列表
     */
    PageResponse<GalleryDTO> getGalleryList(Integer current, Integer size, String contentType, String model, String keyword);

    /**
     * 保存作品到灵感广场
     *
     * @param assetId 资产ID
     */
    void saveToGallery(String assetId);

    /**
     * 获取我的资产列表
     *
     * @param current 当前页
     * @param size 每页大小
     * @param contentType 内容类型
     * @return 分页列表
     */
    PageResponse<AssetDTO> getAssetList(Integer current, Integer size, String contentType);

    /**
     * 删除资产
     *
     * @param assetId 资产ID
     */
    void deleteAsset(String assetId);
}

