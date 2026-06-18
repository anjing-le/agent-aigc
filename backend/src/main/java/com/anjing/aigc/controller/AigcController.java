package com.anjing.aigc.controller;

import com.anjing.aigc.model.dto.MaterialDTO;
import com.anjing.aigc.model.request.GalleryCurationRuleUpdateRequest;
import com.anjing.aigc.model.request.GenerateRequest;
import com.anjing.aigc.model.request.ProviderCredentialUpdateRequest;
import com.anjing.aigc.model.request.ProviderParamUpdateRequest;
import com.anjing.aigc.model.request.ProviderProbeRequest;
import com.anjing.aigc.model.request.ProviderRouteUpdateRequest;
import com.anjing.aigc.model.request.ProviderSmokeTestRequest;
import com.anjing.aigc.model.request.SaveToGalleryRequest;
import com.anjing.aigc.model.request.OwnershipBackfillRequest;
import com.anjing.aigc.model.response.GenerateResponse;
import com.anjing.aigc.model.response.AssetDetailResponse;
import com.anjing.aigc.model.response.GalleryAuditLogResponse;
import com.anjing.aigc.model.response.GalleryAuthorProfileResponse;
import com.anjing.aigc.model.response.GalleryCollectionsResponse;
import com.anjing.aigc.model.response.GalleryCreatorRankingResponse;
import com.anjing.aigc.model.response.GalleryCurationRulesResponse;
import com.anjing.aigc.model.response.GalleryInteractionReportResponse;
import com.anjing.aigc.model.response.GalleryShareResponse;
import com.anjing.aigc.model.response.GalleryTopicsResponse;
import com.anjing.aigc.model.response.MaterialUploadResponse;
import com.anjing.aigc.model.response.ModelListResponse;
import com.anjing.aigc.model.response.OwnershipBackfillResponse;
import com.anjing.aigc.model.response.ProviderAuditLogResponse;
import com.anjing.aigc.model.response.ProviderCredentialUpdateResponse;
import com.anjing.aigc.model.response.ProviderExecutionReportResponse;
import com.anjing.aigc.model.response.ProviderParamUpdateResponse;
import com.anjing.aigc.model.response.ProviderProbeResponse;
import com.anjing.aigc.model.response.ProviderRouteUpdateResponse;
import com.anjing.aigc.model.response.ProviderSmokeTestResponse;
import com.anjing.aigc.model.response.StorageAuditLogResponse;
import com.anjing.aigc.model.response.StorageStatusResponse;
import com.anjing.aigc.model.response.TaskStatusResponse;
import com.anjing.aigc.service.AigcDownloadService;
import com.anjing.aigc.service.AigcGalleryAuditLogService;
import com.anjing.aigc.service.AigcMaterialService;
import com.anjing.aigc.service.AigcOwnershipBackfillService;
import com.anjing.aigc.service.AigcService;
import com.anjing.aigc.service.storage.AigcStorageAuditLogService;
import com.anjing.aigc.service.storage.AigcStorageService;
import com.anjing.model.constants.ApiConstants;
import com.anjing.model.response.APIResponse;
import com.anjing.model.response.PageResult;
import com.anjing.aigc.model.dto.AssetDTO;
import com.anjing.aigc.model.dto.GalleryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
@RequestMapping(ApiConstants.Aigc.BASE)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AIGC Creation", description = "AIGC 创作、任务、资产和灵感广场接口")
public class AigcController {

    private final AigcService aigcService;
    private final AigcMaterialService aigcMaterialService;
    private final AigcDownloadService aigcDownloadService;
    private final AigcStorageService aigcStorageService;
    private final AigcStorageAuditLogService aigcStorageAuditLogService;
    private final AigcGalleryAuditLogService aigcGalleryAuditLogService;
    private final AigcOwnershipBackfillService aigcOwnershipBackfillService;

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
    @PostMapping(ApiConstants.Aigc.GENERATE)
    @Operation(summary = "创建 AIGC 生成任务")
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
    @GetMapping(ApiConstants.Aigc.TASK_STATUS)
    @Operation(summary = "查询 AIGC 任务状态")
    public APIResponse<TaskStatusResponse> getTaskStatus(@PathVariable String taskId) {
        TaskStatusResponse status = aigcService.getTaskStatus(taskId);
        return APIResponse.success(status);
    }

    @PostMapping(ApiConstants.Aigc.TASK_RETRY)
    @Operation(summary = "基于历史任务重新创建 AIGC 生成任务")
    public APIResponse<GenerateResponse> retryTask(@PathVariable String taskId) {
        GenerateResponse response = aigcService.retryTask(taskId);
        return APIResponse.success(response);
    }

    /**
     * 获取可用模型列表
     *
     * @return 模型列表，按类型分组
     */
    @GetMapping(ApiConstants.Aigc.MODELS)
    @Operation(summary = "获取可用模型列表")
    public APIResponse<ModelListResponse> getModels() {
        ModelListResponse models = aigcService.getAvailableModels();
        return APIResponse.success(models);
    }

    @PostMapping(ApiConstants.Aigc.MODEL_PROBE)
    @Operation(summary = "探测 AIGC Provider 配置和当前路由")
    public APIResponse<ProviderProbeResponse> probeModel(@Valid @RequestBody ProviderProbeRequest request) {
        ProviderProbeResponse response = aigcService.probeProvider(request);
        return APIResponse.success(response);
    }

    @PostMapping(ApiConstants.Aigc.MODEL_ACTIVE_PROVIDER)
    @Operation(summary = "运行时切换 AIGC active Provider")
    public APIResponse<ProviderRouteUpdateResponse> updateActiveProvider(
            @Valid @RequestBody ProviderRouteUpdateRequest request) {
        ProviderRouteUpdateResponse response = aigcService.updateActiveProvider(request);
        return APIResponse.success(response);
    }

    @PostMapping(ApiConstants.Aigc.MODEL_PROVIDER_CREDENTIAL)
    @Operation(summary = "只写式更新 AIGC Provider 凭证")
    public APIResponse<ProviderCredentialUpdateResponse> updateProviderCredential(
            @Valid @RequestBody ProviderCredentialUpdateRequest request) {
        ProviderCredentialUpdateResponse response = aigcService.updateProviderCredential(request);
        return APIResponse.success(response);
    }

    @PostMapping(ApiConstants.Aigc.MODEL_PROVIDER_PARAMS)
    @Operation(summary = "更新 AIGC Provider 默认参数模板")
    public APIResponse<ProviderParamUpdateResponse> updateProviderParams(
            @Valid @RequestBody ProviderParamUpdateRequest request) {
        ProviderParamUpdateResponse response = aigcService.updateProviderParams(request);
        return APIResponse.success(response);
    }

    @PostMapping(ApiConstants.Aigc.MODEL_PROVIDER_SMOKE_TEST)
    @Operation(summary = "显式运行 AIGC Provider smoke test")
    public APIResponse<ProviderSmokeTestResponse> smokeTestProvider(
            @Valid @RequestBody ProviderSmokeTestRequest request) {
        ProviderSmokeTestResponse response = aigcService.smokeTestProvider(request);
        return APIResponse.success(response);
    }

    @GetMapping(ApiConstants.Aigc.MODEL_PROVIDER_AUDITS)
    @Operation(summary = "获取 AIGC Provider 管理审计日志")
    public APIResponse<PageResult<ProviderAuditLogResponse>> getProviderAuditLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String action) {
        PageResult<ProviderAuditLogResponse> logs = aigcService.getProviderAuditLogs(
                current, size, contentType, action);
        return APIResponse.success(logs);
    }

    @GetMapping(ApiConstants.Aigc.MODEL_PROVIDER_EXECUTION_REPORT)
    @Operation(summary = "获取 AIGC Provider 执行报表")
    public APIResponse<ProviderExecutionReportResponse> getProviderExecutionReport(
            @RequestParam(defaultValue = "30") Integer days,
            @RequestParam(required = false) String contentType) {
        ProviderExecutionReportResponse report = aigcService.getProviderExecutionReport(days, contentType);
        return APIResponse.success(report);
    }

    @PostMapping(ApiConstants.Aigc.MATERIAL_UPLOAD)
    @Operation(summary = "上传 AIGC 参考素材")
    public APIResponse<MaterialUploadResponse> uploadMaterial(@RequestPart("file") MultipartFile file) {
        MaterialUploadResponse response = aigcMaterialService.uploadMaterial(file);
        return APIResponse.success(response);
    }

    @GetMapping(ApiConstants.Aigc.MATERIALS)
    @Operation(summary = "获取 AIGC 参考素材列表")
    public APIResponse<PageResult<MaterialDTO>> getMaterialList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String contentType) {
        PageResult<MaterialDTO> materials = aigcMaterialService.getMaterialList(current, size, contentType);
        return APIResponse.success(materials);
    }

    @GetMapping(ApiConstants.Aigc.STORAGE_STATUS)
    @Operation(summary = "获取 AIGC 资产存储状态")
    public APIResponse<StorageStatusResponse> getStorageStatus() {
        StorageStatusResponse response = aigcStorageService.getStorageStatus();
        return APIResponse.success(response);
    }

    @GetMapping(ApiConstants.Aigc.STORAGE_AUDITS)
    @Operation(summary = "获取 AIGC 存储审计日志")
    public APIResponse<PageResult<StorageAuditLogResponse>> getStorageAuditLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String backend,
            @RequestParam(required = false) Boolean success) {
        PageResult<StorageAuditLogResponse> logs = aigcStorageAuditLogService.getAuditLogs(
                current, size, action, backend, success);
        return APIResponse.success(logs);
    }

    @PostMapping(ApiConstants.Aigc.OWNERSHIP_BACKFILL)
    @Operation(summary = "回填历史 AIGC 数据归属")
    public APIResponse<OwnershipBackfillResponse> backfillOwnership(
            @Valid @RequestBody OwnershipBackfillRequest request) {
        OwnershipBackfillResponse response = aigcOwnershipBackfillService.backfill(request);
        return APIResponse.success(response);
    }

    @GetMapping(ApiConstants.Aigc.GALLERY_AUDITS)
    @Operation(summary = "获取 AIGC 广场发布和互动审计日志")
    public APIResponse<PageResult<GalleryAuditLogResponse>> getGalleryAuditLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String assetId,
            @RequestParam(required = false) Boolean success) {
        PageResult<GalleryAuditLogResponse> logs = aigcGalleryAuditLogService.getAuditLogs(
                current, size, action, assetId, success);
        return APIResponse.success(logs);
    }

    @GetMapping(ApiConstants.Aigc.GALLERY_INTERACTION_REPORT)
    @Operation(summary = "获取 AIGC 广场互动报表")
    public APIResponse<GalleryInteractionReportResponse> getGalleryInteractionReport(
            @RequestParam(defaultValue = "30") Integer days,
            @RequestParam(required = false) String contentType) {
        GalleryInteractionReportResponse report = aigcGalleryAuditLogService.getInteractionReport(
                days, contentType);
        return APIResponse.success(report);
    }

    @DeleteMapping(ApiConstants.Aigc.MATERIAL_DETAIL)
    @Operation(summary = "删除 AIGC 参考素材")
    public APIResponse<Void> deleteMaterial(@PathVariable String materialId) {
        aigcMaterialService.deleteMaterial(materialId);
        return APIResponse.success(null);
    }

    @GetMapping(ApiConstants.Aigc.MATERIAL_TASKS)
    @Operation(summary = "按素材反查引用它的 AIGC 任务")
    public APIResponse<PageResult<TaskStatusResponse>> getMaterialTasks(
            @PathVariable String materialId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        PageResult<TaskStatusResponse> tasks = aigcService.getTasksByMaterial(materialId, current, size);
        return APIResponse.success(tasks);
    }

    @GetMapping(ApiConstants.Aigc.MATERIAL_DOWNLOAD)
    @Operation(summary = "授权下载 AIGC 参考素材")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable String materialId) {
        return aigcDownloadService.downloadMaterial(materialId);
    }

    @GetMapping(ApiConstants.Aigc.MATERIAL_PREVIEW)
    @Operation(summary = "授权预览 AIGC 参考素材")
    public ResponseEntity<Resource> previewMaterial(@PathVariable String materialId) {
        return aigcDownloadService.previewMaterial(materialId);
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
    @GetMapping(ApiConstants.Aigc.GALLERY)
    @Operation(summary = "获取灵感广场作品列表")
    public APIResponse<PageResult<GalleryDTO>> getGalleryList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String keyword) {
        PageResult<GalleryDTO> gallery = aigcService.getGalleryList(current, size, contentType, model, keyword);
        return APIResponse.success(gallery);
    }

    @GetMapping(ApiConstants.Aigc.GALLERY_RANKING)
    @Operation(summary = "获取灵感广场全局热门榜单")
    public APIResponse<PageResult<GalleryDTO>> getGalleryRanking(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String keyword) {
        PageResult<GalleryDTO> gallery = aigcService.getGalleryRanking(
                current, size, contentType, model, keyword);
        return APIResponse.success(gallery);
    }

    @GetMapping(ApiConstants.Aigc.GALLERY_COLLECTIONS)
    @Operation(summary = "获取灵感广场动态作品合集")
    public APIResponse<GalleryCollectionsResponse> getGalleryCollections(
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "4") Integer size) {
        GalleryCollectionsResponse collections = aigcService.getGalleryCollections(contentType, keyword, size);
        return APIResponse.success(collections);
    }

    @GetMapping(ApiConstants.Aigc.GALLERY_TOPICS)
    @Operation(summary = "获取灵感广场人工运营专题")
    public APIResponse<GalleryTopicsResponse> getGalleryTopics(
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "4") Integer size) {
        GalleryTopicsResponse topics = aigcService.getGalleryTopics(contentType, keyword, size);
        return APIResponse.success(topics);
    }

    @GetMapping(ApiConstants.Aigc.GALLERY_CREATOR_RANKING)
    @Operation(summary = "获取灵感广场公开创作者榜单")
    public APIResponse<GalleryCreatorRankingResponse> getGalleryCreatorRanking(
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "5") Integer size) {
        GalleryCreatorRankingResponse ranking = aigcService.getGalleryCreatorRanking(
                contentType, keyword, size);
        return APIResponse.success(ranking);
    }

    @GetMapping(ApiConstants.Aigc.GALLERY_CURATION_RULES)
    @Operation(summary = "获取灵感广场运营规则说明")
    public APIResponse<GalleryCurationRulesResponse> getGalleryCurationRules() {
        GalleryCurationRulesResponse rules = aigcService.getGalleryCurationRules();
        return APIResponse.success(rules);
    }

    @PostMapping(ApiConstants.Aigc.GALLERY_CURATION_RULE_CONFIG)
    @Operation(summary = "更新灵感广场运营规则配置")
    public APIResponse<GalleryCurationRulesResponse> updateGalleryCurationRule(
            @Valid @RequestBody GalleryCurationRuleUpdateRequest request) {
        GalleryCurationRulesResponse rules = aigcService.updateGalleryCurationRule(request);
        return APIResponse.success(rules);
    }

    @GetMapping(ApiConstants.Aigc.GALLERY_FAVORITES)
    @Operation(summary = "获取当前用户收藏的灵感广场作品")
    public APIResponse<PageResult<GalleryDTO>> getFavoriteGalleryList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        PageResult<GalleryDTO> gallery = aigcService.getMyFavoriteGalleryList(current, size);
        return APIResponse.success(gallery);
    }

    @GetMapping(ApiConstants.Aigc.GALLERY_SHARE)
    @Operation(summary = "获取公开分享页所需的已发布 AIGC 广场作品")
    public APIResponse<GalleryShareResponse> getGalleryShare(@PathVariable String assetId) {
        GalleryShareResponse response = aigcService.getGalleryShare(assetId);
        return APIResponse.success(response);
    }

    @PostMapping(ApiConstants.Aigc.GALLERY_SHARE_REUSE)
    @Operation(
            summary = "记录公开分享页 Prompt 复用",
            operationId = "recordGallerySharePromptReuse"
    )
    public APIResponse<Void> recordGallerySharePromptReuse(@PathVariable String assetId) {
        aigcService.recordGallerySharePromptReuse(assetId);
        return APIResponse.success(null);
    }

    @GetMapping(ApiConstants.Aigc.GALLERY_AUTHOR_PROFILE)
    @Operation(summary = "获取灵感广场公开作者主页")
    public APIResponse<GalleryAuthorProfileResponse> getGalleryAuthorProfile(
            @PathVariable String authorId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String contentType) {
        GalleryAuthorProfileResponse response = aigcService.getGalleryAuthorProfile(
                authorId, current, size, contentType);
        return APIResponse.success(response);
    }

    @GetMapping(ApiConstants.Aigc.GALLERY_ASSET_PREVIEW)
    @Operation(summary = "公开预览已发布 AIGC 广场作品")
    public ResponseEntity<Resource> previewGalleryAsset(@PathVariable String assetId) {
        return aigcDownloadService.previewPublishedAsset(assetId);
    }

    @GetMapping(ApiConstants.Aigc.GALLERY_ASSET_DOWNLOAD)
    @Operation(summary = "公开下载已发布 AIGC 广场作品")
    public ResponseEntity<Resource> downloadGalleryAsset(@PathVariable String assetId) {
        return aigcDownloadService.downloadPublishedAsset(assetId);
    }

    /**
     * 保存作品到灵感广场
     *
     * @param body 请求体，包含 assetId
     * @return 操作结果
     */
    @PostMapping(ApiConstants.Aigc.GALLERY_SAVE)
    @Operation(summary = "保存作品到灵感广场")
    public APIResponse<Void> saveToGallery(@Valid @RequestBody SaveToGalleryRequest request) {
        aigcService.saveToGallery(request.getAssetId());
        return APIResponse.success(null);
    }

    @DeleteMapping(ApiConstants.Aigc.GALLERY_PUBLICATION)
    @Operation(summary = "从灵感广场撤回作品")
    public APIResponse<Void> removeFromGallery(@PathVariable String assetId) {
        aigcService.removeFromGallery(assetId);
        return APIResponse.success(null);
    }

    @PostMapping(ApiConstants.Aigc.GALLERY_LIKE)
    @Operation(summary = "点赞灵感广场作品")
    public APIResponse<GalleryDTO> likeGalleryAsset(@PathVariable String assetId) {
        GalleryDTO response = aigcService.likeGalleryAsset(assetId);
        return APIResponse.success(response);
    }

    @DeleteMapping(ApiConstants.Aigc.GALLERY_LIKE)
    @Operation(summary = "取消点赞灵感广场作品")
    public APIResponse<GalleryDTO> unlikeGalleryAsset(@PathVariable String assetId) {
        GalleryDTO response = aigcService.unlikeGalleryAsset(assetId);
        return APIResponse.success(response);
    }

    @PostMapping(ApiConstants.Aigc.GALLERY_FAVORITE)
    @Operation(summary = "收藏灵感广场作品")
    public APIResponse<GalleryDTO> favoriteGalleryAsset(@PathVariable String assetId) {
        GalleryDTO response = aigcService.favoriteGalleryAsset(assetId);
        return APIResponse.success(response);
    }

    @DeleteMapping(ApiConstants.Aigc.GALLERY_FAVORITE)
    @Operation(summary = "取消收藏灵感广场作品")
    public APIResponse<GalleryDTO> unfavoriteGalleryAsset(@PathVariable String assetId) {
        GalleryDTO response = aigcService.unfavoriteGalleryAsset(assetId);
        return APIResponse.success(response);
    }

    /**
     * 获取我的资产列表
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param contentType 内容类型筛选
     * @return 资产分页列表
     */
    @GetMapping(ApiConstants.Aigc.ASSETS)
    @Operation(summary = "获取我的资产列表")
    public APIResponse<PageResult<AssetDTO>> getAssetList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String contentType) {
        PageResult<AssetDTO> assets = aigcService.getAssetList(current, size, contentType);
        return APIResponse.success(assets);
    }

    @GetMapping(ApiConstants.Aigc.ASSET_DETAIL)
    @Operation(summary = "获取资产详情和来源任务")
    public APIResponse<AssetDetailResponse> getAssetDetail(@PathVariable String assetId) {
        AssetDetailResponse detail = aigcService.getAssetDetail(assetId);
        return APIResponse.success(detail);
    }

    @GetMapping(ApiConstants.Aigc.ASSET_DOWNLOAD)
    @Operation(summary = "授权下载 AIGC 资产文件")
    public ResponseEntity<Resource> downloadAsset(@PathVariable String assetId) {
        return aigcDownloadService.downloadAsset(assetId);
    }

    @GetMapping(ApiConstants.Aigc.ASSET_PREVIEW)
    @Operation(summary = "授权预览 AIGC 资产文件")
    public ResponseEntity<Resource> previewAsset(@PathVariable String assetId) {
        return aigcDownloadService.previewAsset(assetId);
    }

    /**
     * 删除资产
     *
     * @param assetId 资产ID
     * @return 操作结果
     */
    @DeleteMapping(ApiConstants.Aigc.ASSET_DETAIL)
    @Operation(summary = "删除资产")
    public APIResponse<Void> deleteAsset(@PathVariable String assetId) {
        aigcService.deleteAsset(assetId);
        return APIResponse.success(null);
    }
}
