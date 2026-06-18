package com.anjing.aigc.service;

import com.anjing.aigc.model.dto.AssetDTO;
import com.anjing.aigc.model.dto.GalleryDTO;
import com.anjing.aigc.model.request.GenerateRequest;
import com.anjing.aigc.model.request.ProviderCredentialUpdateRequest;
import com.anjing.aigc.model.request.ProviderParamUpdateRequest;
import com.anjing.aigc.model.request.ProviderProbeRequest;
import com.anjing.aigc.model.request.ProviderRouteUpdateRequest;
import com.anjing.aigc.model.request.ProviderSmokeTestRequest;
import com.anjing.aigc.model.response.AssetDetailResponse;
import com.anjing.aigc.model.response.GalleryAuthorProfileResponse;
import com.anjing.aigc.model.response.GalleryCollectionsResponse;
import com.anjing.aigc.model.response.GalleryShareResponse;
import com.anjing.aigc.model.response.GalleryTopicsResponse;
import com.anjing.aigc.model.response.GenerateResponse;
import com.anjing.aigc.model.response.ModelListResponse;
import com.anjing.aigc.model.response.ProviderAuditLogResponse;
import com.anjing.aigc.model.response.ProviderCredentialUpdateResponse;
import com.anjing.aigc.model.response.ProviderExecutionReportResponse;
import com.anjing.aigc.model.response.ProviderParamUpdateResponse;
import com.anjing.aigc.model.response.ProviderProbeResponse;
import com.anjing.aigc.model.response.ProviderRouteUpdateResponse;
import com.anjing.aigc.model.response.ProviderSmokeTestResponse;
import com.anjing.aigc.model.response.TaskStatusResponse;
import com.anjing.model.response.PageResult;

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
     * 基于历史任务重新创建生成任务
     *
     * @param taskId 原任务ID
     * @return 新生成任务响应
     */
    GenerateResponse retryTask(String taskId);

    /**
     * 按素材反查引用它的任务
     *
     * @param materialId 素材ID
     * @param current 当前页
     * @param size 每页大小
     * @return 任务分页列表
     */
    PageResult<TaskStatusResponse> getTasksByMaterial(String materialId, Integer current, Integer size);

    /**
     * 获取可用模型列表
     *
     * @return 模型列表
     */
    ModelListResponse getAvailableModels();

    /**
     * 探测 Provider 配置和当前路由是否可运行。
     *
     * @param request 探测请求
     * @return 探测结果
     */
    ProviderProbeResponse probeProvider(ProviderProbeRequest request);

    /**
     * 运行时切换指定内容类型的 active provider。
     *
     * @param request 切换请求
     * @return 切换后的路由摘要
     */
    ProviderRouteUpdateResponse updateActiveProvider(ProviderRouteUpdateRequest request);

    /**
     * 只写式更新 Provider 凭证。
     *
     * @param request 凭证更新请求
     * @return 更新后的非敏感配置摘要
     */
    ProviderCredentialUpdateResponse updateProviderCredential(ProviderCredentialUpdateRequest request);

    /**
     * 更新 Provider 默认参数模板。
     *
     * @param request 参数模板更新请求
     * @return 更新后的参数模板摘要
     */
    ProviderParamUpdateResponse updateProviderParams(ProviderParamUpdateRequest request);

    /**
     * 显式运行 Provider smoke test。
     *
     * @param request smoke test 请求
     * @return smoke test 结果
     */
    ProviderSmokeTestResponse smokeTestProvider(ProviderSmokeTestRequest request);

    /**
     * 获取 Provider 管理审计日志。
     *
     * @param current 当前页
     * @param size 每页大小
     * @param contentType 内容类型
     * @param action 审计动作
     * @return 审计分页列表
     */
    PageResult<ProviderAuditLogResponse> getProviderAuditLogs(
            Integer current, Integer size, String contentType, String action);

    /**
     * 获取 Provider 执行报表。
     *
     * @param days 时间窗口
     * @param contentType 内容类型
     * @return 执行报表
     */
    ProviderExecutionReportResponse getProviderExecutionReport(Integer days, String contentType);

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
    PageResult<GalleryDTO> getGalleryList(Integer current, Integer size, String contentType, String model, String keyword);

    /**
     * 获取灵感广场全局热门榜单。
     *
     * @param current 当前页
     * @param size 每页大小
     * @param contentType 内容类型
     * @param model 模型
     * @param keyword 关键词
     * @return 分页榜单
     */
    PageResult<GalleryDTO> getGalleryRanking(
            Integer current, Integer size, String contentType, String model, String keyword);

    /**
     * 获取灵感广场动态作品合集。
     *
     * @param contentType 内容类型
     * @param keyword 关键词
     * @param size 每个合集的作品数量
     * @return 动态合集
     */
    GalleryCollectionsResponse getGalleryCollections(String contentType, String keyword, Integer size);

    /**
     * 获取灵感广场人工运营专题。
     *
     * @param contentType 内容类型
     * @param keyword 关键词
     * @param size 每个专题的作品数量
     * @return 运营专题
     */
    GalleryTopicsResponse getGalleryTopics(String contentType, String keyword, Integer size);

    /**
     * 获取当前用户/会话收藏的灵感广场作品。
     *
     * @param current 当前页
     * @param size 每页大小
     * @return 分页列表
     */
    PageResult<GalleryDTO> getMyFavoriteGalleryList(Integer current, Integer size);

    /**
     * 获取公开分享页所需的已发布作品信息。
     *
     * @param assetId 资产ID
     * @return 分享页响应
     */
    GalleryShareResponse getGalleryShare(String assetId);

    /**
     * 记录公开分享页 Prompt 复用行为。
     *
     * @param assetId 资产ID
     */
    void recordGallerySharePromptReuse(String assetId);

    /**
     * 获取公开作者主页。
     *
     * @param authorId 公开作者标识
     * @param current 当前页
     * @param size 每页大小
     * @param contentType 内容类型
     * @return 作者主页响应
     */
    GalleryAuthorProfileResponse getGalleryAuthorProfile(
            String authorId, Integer current, Integer size, String contentType);

    /**
     * 保存作品到灵感广场
     *
     * @param assetId 资产ID
     */
    void saveToGallery(String assetId);

    /**
     * 从灵感广场撤回作品
     *
     * @param assetId 资产ID
     */
    void removeFromGallery(String assetId);

    /**
     * 点赞灵感广场作品
     *
     * @param assetId 资产ID
     * @return 更新后的广场作品
     */
    GalleryDTO likeGalleryAsset(String assetId);

    /**
     * 取消点赞灵感广场作品
     *
     * @param assetId 资产ID
     * @return 更新后的广场作品
     */
    GalleryDTO unlikeGalleryAsset(String assetId);

    /**
     * 收藏灵感广场作品
     *
     * @param assetId 资产ID
     * @return 更新后的广场作品
     */
    GalleryDTO favoriteGalleryAsset(String assetId);

    /**
     * 取消收藏灵感广场作品
     *
     * @param assetId 资产ID
     * @return 更新后的广场作品
     */
    GalleryDTO unfavoriteGalleryAsset(String assetId);

    /**
     * 获取我的资产列表
     *
     * @param current 当前页
     * @param size 每页大小
     * @param contentType 内容类型
     * @return 分页列表
     */
    PageResult<AssetDTO> getAssetList(Integer current, Integer size, String contentType);

    /**
     * 获取资产详情及来源任务
     *
     * @param assetId 资产ID
     * @return 资产详情
     */
    AssetDetailResponse getAssetDetail(String assetId);

    /**
     * 删除资产
     *
     * @param assetId 资产ID
     */
    void deleteAsset(String assetId);
}
