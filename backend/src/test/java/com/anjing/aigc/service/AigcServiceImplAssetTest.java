package com.anjing.aigc.service;

import com.anjing.aigc.agent.RoutingAgent;
import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.enums.TaskStatus;
import com.anjing.aigc.model.request.ProviderProbeRequest;
import com.anjing.aigc.model.request.ProviderRouteUpdateRequest;
import com.anjing.aigc.model.request.ProviderSmokeTestRequest;
import com.anjing.aigc.model.response.AgentAnalysis;
import com.anjing.aigc.model.response.AssetDetailResponse;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.model.response.ModelListResponse;
import com.anjing.aigc.model.response.ProviderProbeResponse;
import com.anjing.aigc.model.response.ProviderRouteUpdateResponse;
import com.anjing.aigc.model.response.ProviderSmokeTestResponse;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.provider.ImageGenerationProvider;
import com.anjing.aigc.provider.ProviderRouter;
import com.anjing.aigc.repository.AigcAssetRepository;
import com.anjing.aigc.repository.AigcMaterialRepository;
import com.anjing.aigc.repository.AigcProviderCredentialConfigRepository;
import com.anjing.aigc.repository.AigcProviderParamConfigRepository;
import com.anjing.aigc.repository.AigcProviderRouteConfigRepository;
import com.anjing.aigc.repository.AigcTaskRepository;
import com.anjing.aigc.service.impl.AigcServiceImpl;
import com.anjing.aigc.service.storage.AigcStorageService;
import com.anjing.model.errorcode.AigcErrorCode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AigcServiceImplAssetTest {

    private final RoutingAgent routingAgent = mock(RoutingAgent.class);
    private final AigcTaskExecutor taskExecutor = mock(AigcTaskExecutor.class);
    private final ProviderRouter providerRouter = mock(ProviderRouter.class);
    private final AigcProperties aigcProperties = new AigcProperties();
    private final AigcProviderAuditLogService auditLogService = mock(AigcProviderAuditLogService.class);
    private final AigcProviderCostEstimator costEstimator = new AigcProviderCostEstimator(aigcProperties);
    private final AigcProviderManagementPermissionService permissionService =
            mock(AigcProviderManagementPermissionService.class);
    private final AigcProviderCredentialConfigRepository credentialConfigRepository =
            mock(AigcProviderCredentialConfigRepository.class);
    private final AigcProviderCredentialCodec credentialCodec =
            new AigcProviderCredentialCodec(aigcProperties);
    private final AigcProviderCredentialConfigService credentialConfigService =
            new AigcProviderCredentialConfigService(aigcProperties, credentialConfigRepository, credentialCodec);
    private final AigcProviderParamConfigRepository paramConfigRepository =
            mock(AigcProviderParamConfigRepository.class);
    private final AigcProviderParamConfigService paramConfigService =
            new AigcProviderParamConfigService(aigcProperties, paramConfigRepository);
    private final AigcProviderRouteConfigRepository routeConfigRepository =
            mock(AigcProviderRouteConfigRepository.class);
    private final AigcProviderRouteConfigService routeConfigService =
            new AigcProviderRouteConfigService(aigcProperties, routeConfigRepository);
    private final AigcTaskRepository taskRepository = mock(AigcTaskRepository.class);
    private final AigcAssetRepository assetRepository = mock(AigcAssetRepository.class);
    private final AigcMaterialRepository materialRepository = mock(AigcMaterialRepository.class);
    private final AigcReferenceMaterialPolicy referenceMaterialPolicy = mock(AigcReferenceMaterialPolicy.class);
    private final AigcStorageService storageService = mock(AigcStorageService.class);
    private final AigcOwnershipService ownershipService = new AigcOwnershipService();
    private final AigcServiceImpl aigcService = new AigcServiceImpl(
            routingAgent,
            taskExecutor,
            providerRouter,
            aigcProperties,
            auditLogService,
            costEstimator,
            permissionService,
            credentialConfigService,
            paramConfigService,
            routeConfigService,
            taskRepository,
            assetRepository,
            materialRepository,
            referenceMaterialPolicy,
            storageService,
            ownershipService
    );

    @Test
    void deleteAssetRemovesLocalFilesAndRecord() throws Exception {
        AigcAsset asset = asset("asset-1");
        asset.setUrl("http://localhost:10003/files/images/asset-1.png");
        asset.setThumbnailUrl("http://localhost:10003/files/images/asset-1-thumb.png");
        when(assetRepository.findVisibleByAssetId("asset-1", null, null)).thenReturn(Optional.of(asset));

        aigcService.deleteAsset("asset-1");

        verify(storageService).deleteByUrl(asset.getUrl());
        verify(storageService).deleteByUrl(asset.getThumbnailUrl());
        verify(assetRepository).deleteByAssetId("asset-1");
    }

    @Test
    void getAssetDetailReturnsSourceTaskWhenExists() {
        AigcAsset asset = asset("asset-0");
        asset.setUrl("http://localhost:10003/files/images/asset-0.png");
        when(assetRepository.findVisibleByAssetId("asset-0", null, null)).thenReturn(Optional.of(asset));

        AigcTask task = new AigcTask();
        task.setTaskId("task-0");
        task.setAssetId("asset-0");
        task.setPrompt("prompt");
        task.setOptimizedPrompt("optimized prompt");
        task.setContentType(ContentType.IMAGE);
        task.setProviderName("Mock Image Provider");
        task.setProviderType("OTHER");
        task.setStatus(TaskStatus.COMPLETED);
        task.setProgress(100);
        task.setDurationMs(1200L);
        task.setAgentAnalysis(AgentAnalysis.builder()
                .intent("text_to_image")
                .contentType(ContentType.IMAGE)
                .selectedModel("mock-image-preview")
                .cleanPrompt("clean prompt")
                .optimizedPrompt("optimized prompt")
                .confidence(0.8)
                .build());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        when(taskRepository.findVisibleByAssetId("asset-0", null, null)).thenReturn(Optional.of(task));

        AssetDetailResponse detail = aigcService.getAssetDetail("asset-0");

        assertEquals("asset-0", detail.getAsset().getId());
        assertEquals("task-0", detail.getTask().getTaskId());
        assertEquals(TaskStatus.COMPLETED, detail.getTask().getStatus());
        assertEquals("clean prompt", detail.getTask().getAgentAnalysis().getCleanPrompt());
        assertEquals(0.8, detail.getTask().getAgentAnalysis().getConfidence());
        assertEquals("Mock Image Provider", detail.getTask().getProviderExecution().getProviderName());
        assertEquals("MOCK_FREE", detail.getTask().getProviderExecution().getCostStatus());
        assertEquals(0, detail.getTask().getProviderExecution().getEstimatedCostAmount().signum());
        assertEquals("USD", detail.getTask().getProviderExecution().getEstimatedCostCurrency());
    }

    @Test
    void getAvailableModelsReturnsConfigurationStatus() {
        givenImageProviders();
        when(providerRouter.getVideoProviders()).thenReturn(List.of());
        when(providerRouter.getAudioProviders()).thenReturn(List.of());

        ModelListResponse models = aigcService.getAvailableModels();

        assertEquals(2, models.getImageModels().size());
        assertEquals("缺少 Google Provider 凭证", models.getImageModels().get(0).getMissingConfig());
        assertEquals("missing", models.getImageModels().get(0).getCredentialSource());
        assertEquals("configuration", models.getImageModels().get(0).getParamConfigSource());
        assertEquals(true, models.getImageModels().get(0).getActive());
        assertEquals("configuration", models.getImageModels().get(0).getRouteConfigSource());
        assertEquals("ESTIMATE_NOT_CONFIGURED", models.getImageModels().get(0).getCostStatus());
        assertEquals(false, models.getImageModels().get(0).getCostEstimateConfigured());
        assertEquals("registered", models.getImageModels().get(0).getChecks().get(0).getId());
        assertEquals("mock-image-preview", models.getImageModels().get(1).getConfiguredModel());
        assertEquals("not-required", models.getImageModels().get(1).getCredentialSource());
        assertEquals("not-required", models.getImageModels().get(1).getParamConfigSource());
        assertEquals("local-demo", models.getImageModels().get(1).getDefaultParams().get("mode"));
        assertEquals("MOCK_FREE", models.getImageModels().get(1).getCostStatus());
        assertEquals(true, models.getImageModels().get(1).getCostEstimateConfigured());
    }

    @Test
    void probeProviderReportsMissingGoogleConfiguration() {
        givenImageProviders();

        ProviderProbeRequest request = new ProviderProbeRequest();
        request.setContentType(ContentType.IMAGE);
        request.setProvider("GOOGLE");
        request.setProviderName("Google Nano Banana");

        ProviderProbeResponse response = aigcService.probeProvider(request);

        assertEquals(false, response.getRoutable());
        assertEquals(true, response.getActive());
        assertEquals("缺少 Google Provider 凭证", response.getMissingConfig());
        assertEquals("探测未通过：配置不完整", response.getMessage());
        assertEquals("ESTIMATE_NOT_CONFIGURED", response.getCostStatus());
        assertEquals(false, response.getCostEstimateConfigured());
        assertEquals("credential", response.getChecks().get(2).getId());
        assertEquals("FAIL", response.getChecks().get(2).getStatus());
    }

    @Test
    void probeProviderReportsRegisteredButInactiveProvider() {
        givenImageProviders();

        ProviderProbeRequest request = new ProviderProbeRequest();
        request.setContentType(ContentType.IMAGE);
        request.setProvider("OTHER");
        request.setProviderName("Mock Image Provider");

        ProviderProbeResponse response = aigcService.probeProvider(request);

        assertEquals(true, response.getRegistered());
        assertEquals(false, response.getActive());
        assertEquals(true, response.getAvailable());
        assertEquals(false, response.getRoutable());
        assertEquals("探测通过：Provider 已注册，但不是当前路由", response.getMessage());
        assertEquals("MOCK_FREE", response.getCostStatus());
        assertEquals(true, response.getCostEstimateConfigured());
        assertEquals("WARN", response.getChecks().get(1).getStatus());
    }

    @Test
    void updateActiveProviderSwitchesRuntimeRouteToMockProvider() {
        givenImageProviders();

        ProviderRouteUpdateRequest request = new ProviderRouteUpdateRequest();
        request.setContentType(ContentType.IMAGE);
        request.setProvider("OTHER");
        request.setProviderName("Mock Image Provider");

        ProviderRouteUpdateResponse response = aigcService.updateActiveProvider(request);

        assertEquals("Mock Image Provider", response.getActiveProvider());
        assertEquals("Mock Image Provider", response.getProviderName());
        assertEquals("database", response.getRouteConfigSource());
        assertEquals(true, response.getRoutable());
        verify(permissionService).assertCanManageProvider("active-provider", ContentType.IMAGE, "OTHER");
        verify(routeConfigRepository).save(org.mockito.ArgumentMatchers.argThat(config ->
                config.getContentType() == ContentType.IMAGE
                        && "Mock Image Provider".equals(config.getActiveProvider())
                        && "Mock Image Provider".equals(config.getProviderName())
                        && "OTHER".equals(config.getProviderType())));
    }

    @Test
    void updateActiveProviderRejectsUnregisteredProvider() {
        givenImageProviders();

        ProviderRouteUpdateRequest request = new ProviderRouteUpdateRequest();
        request.setContentType(ContentType.IMAGE);
        request.setProvider("missing-provider");

        AigcException error = assertThrows(AigcException.class, () -> aigcService.updateActiveProvider(request));

        assertEquals(AigcErrorCode.PROVIDER_UNAVAILABLE, error.getErrorCode());
    }

    @Test
    void smokeTestProviderRunsMockImageProviderAndSavesAsset() {
        ImageGenerationProvider mockProvider = givenSingleMockImageProvider();
        when(mockProvider.generate(org.mockito.ArgumentMatchers.any(AigcTask.class)))
                .thenReturn(GenerationResult.builder()
                        .success(true)
                        .taskId("task-smoke")
                        .contentType(ContentType.IMAGE)
                        .url("data:image/svg+xml,test")
                        .thumbnailUrl("data:image/svg+xml,test")
                        .prompt("prompt")
                        .model("mock-image-preview")
                        .build());

        ProviderSmokeTestRequest request = new ProviderSmokeTestRequest();
        request.setContentType(ContentType.IMAGE);
        request.setProvider("OTHER");
        request.setProviderName("Mock Image Provider");
        request.setPrompt("smoke prompt");

        ProviderSmokeTestResponse response = aigcService.smokeTestProvider(request);

        assertEquals(true, response.getSuccess());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("mock-image-preview", response.getModel());
        assertEquals("MOCK_FREE", response.getProviderExecution().getCostStatus());
        verify(assetRepository).save(org.mockito.ArgumentMatchers.argThat(asset ->
                asset.getContentType() == ContentType.IMAGE
                        && "mock-image-preview".equals(asset.getModel())
                        && "smoke prompt".equals(asset.getPrompt())));
        verify(taskRepository, org.mockito.Mockito.atLeastOnce()).save(org.mockito.ArgumentMatchers.argThat(task ->
                task.getContentType() == ContentType.IMAGE
                        && "provider_smoke_test".equals(task.getIntent())));
        verify(auditLogService).record(
                org.mockito.ArgumentMatchers.eq("smoke-test"),
                org.mockito.ArgumentMatchers.eq(ContentType.IMAGE),
                org.mockito.ArgumentMatchers.eq("OTHER"),
                org.mockito.ArgumentMatchers.eq("Mock Image Provider"),
                org.mockito.ArgumentMatchers.eq("OTHER"),
                org.mockito.ArgumentMatchers.anyMap(),
                org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    void smokeTestProviderRequiresExplicitExternalConfirmationForGoogle() {
        givenImageProviders();

        ProviderSmokeTestRequest request = new ProviderSmokeTestRequest();
        request.setContentType(ContentType.IMAGE);
        request.setProvider("GOOGLE");
        request.setProviderName("Google Nano Banana");

        ProviderSmokeTestResponse response = aigcService.smokeTestProvider(request);

        assertEquals(false, response.getSuccess());
        assertEquals("SKIPPED", response.getStatus());
        assertEquals("Google smoke test 会触发外部调用，请显式确认", response.getMessage());
    }

    @Test
    void deleteAssetContinuesWhenLocalFileDeleteFails() throws Exception {
        AigcAsset asset = asset("asset-2");
        asset.setUrl("http://localhost:10003/files/images/asset-2.png");
        when(assetRepository.findVisibleByAssetId("asset-2", null, null)).thenReturn(Optional.of(asset));
        when(storageService.deleteByUrl(asset.getUrl())).thenThrow(new IOException("delete failed"));

        aigcService.deleteAsset("asset-2");

        verify(assetRepository).deleteByAssetId("asset-2");
    }

    @Test
    void deleteAssetRejectsMissingAsset() {
        when(assetRepository.findVisibleByAssetId("missing", null, null)).thenReturn(Optional.empty());

        AigcException error = assertThrows(AigcException.class, () -> aigcService.deleteAsset("missing"));

        assertEquals(AigcErrorCode.ASSET_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void removeFromGalleryUnpublishesVisibleAsset() {
        AigcAsset asset = asset("asset-published");
        asset.setIsPublished(true);
        when(assetRepository.findVisibleByAssetId("asset-published", null, null)).thenReturn(Optional.of(asset));

        aigcService.removeFromGallery("asset-published");

        assertEquals(false, asset.getIsPublished());
        verify(assetRepository).save(asset);
    }

    @Test
    void removeFromGalleryRejectsMissingAsset() {
        when(assetRepository.findVisibleByAssetId("missing", null, null)).thenReturn(Optional.empty());

        AigcException error = assertThrows(AigcException.class, () -> aigcService.removeFromGallery("missing"));

        assertEquals(AigcErrorCode.ASSET_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void likeGalleryAssetIncrementsPublishedAssetLikeCount() {
        AigcAsset asset = asset("asset-liked");
        asset.setIsPublished(true);
        asset.setLikeCount(2);
        when(assetRepository.findByAssetIdAndIsPublishedTrue("asset-liked")).thenReturn(Optional.of(asset));
        when(assetRepository.save(asset)).thenReturn(asset);

        assertEquals(3, aigcService.likeGalleryAsset("asset-liked").getLikeCount());
        assertEquals(3, asset.getLikeCount());
        verify(assetRepository).save(asset);
    }

    @Test
    void unlikeGalleryAssetDoesNotGoBelowZero() {
        AigcAsset asset = asset("asset-unliked");
        asset.setIsPublished(true);
        asset.setLikeCount(0);
        when(assetRepository.findByAssetIdAndIsPublishedTrue("asset-unliked")).thenReturn(Optional.of(asset));
        when(assetRepository.save(asset)).thenReturn(asset);

        assertEquals(0, aigcService.unlikeGalleryAsset("asset-unliked").getLikeCount());
        assertEquals(0, asset.getLikeCount());
        verify(assetRepository).save(asset);
    }

    @Test
    void likeGalleryAssetRequiresPublishedAsset() {
        when(assetRepository.findByAssetIdAndIsPublishedTrue("draft")).thenReturn(Optional.empty());

        AigcException error = assertThrows(AigcException.class, () -> aigcService.likeGalleryAsset("draft"));

        assertEquals(AigcErrorCode.ASSET_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void favoriteGalleryAssetIncrementsPublishedAssetFavoriteCount() {
        AigcAsset asset = asset("asset-favorited");
        asset.setIsPublished(true);
        asset.setFavoriteCount(4);
        when(assetRepository.findByAssetIdAndIsPublishedTrue("asset-favorited")).thenReturn(Optional.of(asset));
        when(assetRepository.save(asset)).thenReturn(asset);

        assertEquals(5, aigcService.favoriteGalleryAsset("asset-favorited").getFavoriteCount());
        assertEquals(5, asset.getFavoriteCount());
        verify(assetRepository).save(asset);
    }

    @Test
    void unfavoriteGalleryAssetDoesNotGoBelowZero() {
        AigcAsset asset = asset("asset-unfavorited");
        asset.setIsPublished(true);
        asset.setFavoriteCount(0);
        when(assetRepository.findByAssetIdAndIsPublishedTrue("asset-unfavorited")).thenReturn(Optional.of(asset));
        when(assetRepository.save(asset)).thenReturn(asset);

        assertEquals(0, aigcService.unfavoriteGalleryAsset("asset-unfavorited").getFavoriteCount());
        assertEquals(0, asset.getFavoriteCount());
        verify(assetRepository).save(asset);
    }

    @Test
    void favoriteGalleryAssetRequiresPublishedAsset() {
        when(assetRepository.findByAssetIdAndIsPublishedTrue("draft")).thenReturn(Optional.empty());

        AigcException error = assertThrows(AigcException.class, () -> aigcService.favoriteGalleryAsset("draft"));

        assertEquals(AigcErrorCode.ASSET_NOT_FOUND, error.getErrorCode());
    }

    private AigcAsset asset(String assetId) {
        AigcAsset asset = new AigcAsset();
        asset.setAssetId(assetId);
        asset.setContentType(ContentType.IMAGE);
        asset.setPrompt("prompt");
        asset.setModel("mock-image-preview");
        asset.setIsPublished(false);
        asset.setLikeCount(0);
        asset.setFavoriteCount(0);
        return asset;
    }

    private void givenImageProviders() {
        aigcProperties.getImage().setActiveProvider("google");
        aigcProperties.getProviders().getGoogle().setApiKey(null);
        when(credentialConfigRepository.findByProviderKey("google")).thenReturn(Optional.empty());
        when(paramConfigRepository.findByContentTypeAndProviderKey(ContentType.IMAGE, "google"))
                .thenReturn(Optional.empty());
        when(routeConfigRepository.findByContentType(ContentType.IMAGE)).thenReturn(Optional.empty());

        ImageGenerationProvider googleProvider = mock(ImageGenerationProvider.class);
        when(googleProvider.getProviderName()).thenReturn("Google Nano Banana");
        when(googleProvider.getProviderType()).thenReturn(ContentProvider.ProviderType.GOOGLE);
        when(googleProvider.isAvailable()).thenReturn(false);

        ImageGenerationProvider mockProvider = mock(ImageGenerationProvider.class);
        when(mockProvider.getProviderName()).thenReturn("Mock Image Provider");
        when(mockProvider.getProviderType()).thenReturn(ContentProvider.ProviderType.OTHER);
        when(mockProvider.isAvailable()).thenReturn(true);

        when(providerRouter.getImageProviders()).thenReturn(List.of(googleProvider, mockProvider));
    }

    private ImageGenerationProvider givenSingleMockImageProvider() {
        aigcProperties.getImage().setActiveProvider("mock");
        when(routeConfigRepository.findByContentType(ContentType.IMAGE)).thenReturn(Optional.empty());
        ImageGenerationProvider mockProvider = mock(ImageGenerationProvider.class);
        when(mockProvider.getProviderName()).thenReturn("Mock Image Provider");
        when(mockProvider.getProviderType()).thenReturn(ContentProvider.ProviderType.OTHER);
        when(mockProvider.isAvailable()).thenReturn(true);
        when(providerRouter.getImageProviders()).thenReturn(List.of(mockProvider));
        return mockProvider;
    }
}
