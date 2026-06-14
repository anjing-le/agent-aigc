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
import com.anjing.aigc.model.response.AgentAnalysis;
import com.anjing.aigc.model.response.AssetDetailResponse;
import com.anjing.aigc.model.response.ModelListResponse;
import com.anjing.aigc.model.response.ProviderProbeResponse;
import com.anjing.aigc.model.response.ProviderRouteUpdateResponse;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.provider.ImageGenerationProvider;
import com.anjing.aigc.provider.ProviderRouter;
import com.anjing.aigc.repository.AigcAssetRepository;
import com.anjing.aigc.repository.AigcMaterialRepository;
import com.anjing.aigc.repository.AigcTaskRepository;
import com.anjing.aigc.service.impl.AigcServiceImpl;
import com.anjing.aigc.service.storage.LocalAigcStorageService;
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
    private final AigcTaskRepository taskRepository = mock(AigcTaskRepository.class);
    private final AigcAssetRepository assetRepository = mock(AigcAssetRepository.class);
    private final AigcMaterialRepository materialRepository = mock(AigcMaterialRepository.class);
    private final AigcReferenceMaterialPolicy referenceMaterialPolicy = mock(AigcReferenceMaterialPolicy.class);
    private final LocalAigcStorageService storageService = mock(LocalAigcStorageService.class);
    private final AigcServiceImpl aigcService = new AigcServiceImpl(
            routingAgent,
            taskExecutor,
            providerRouter,
            aigcProperties,
            taskRepository,
            assetRepository,
            materialRepository,
            referenceMaterialPolicy,
            storageService
    );

    @Test
    void deleteAssetRemovesLocalFilesAndRecord() throws Exception {
        AigcAsset asset = asset("asset-1");
        asset.setUrl("http://localhost:10003/files/images/asset-1.png");
        asset.setThumbnailUrl("http://localhost:10003/files/images/asset-1-thumb.png");
        when(assetRepository.findByAssetId("asset-1")).thenReturn(Optional.of(asset));

        aigcService.deleteAsset("asset-1");

        verify(storageService).deleteByUrl(asset.getUrl());
        verify(storageService).deleteByUrl(asset.getThumbnailUrl());
        verify(assetRepository).deleteByAssetId("asset-1");
    }

    @Test
    void getAssetDetailReturnsSourceTaskWhenExists() {
        AigcAsset asset = asset("asset-0");
        asset.setUrl("http://localhost:10003/files/images/asset-0.png");
        when(assetRepository.findByAssetId("asset-0")).thenReturn(Optional.of(asset));

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
        when(taskRepository.findByAssetId("asset-0")).thenReturn(Optional.of(task));

        AssetDetailResponse detail = aigcService.getAssetDetail("asset-0");

        assertEquals("asset-0", detail.getAsset().getId());
        assertEquals("task-0", detail.getTask().getTaskId());
        assertEquals(TaskStatus.COMPLETED, detail.getTask().getStatus());
        assertEquals("clean prompt", detail.getTask().getAgentAnalysis().getCleanPrompt());
        assertEquals(0.8, detail.getTask().getAgentAnalysis().getConfidence());
        assertEquals("Mock Image Provider", detail.getTask().getProviderExecution().getProviderName());
        assertEquals("MOCK_FREE", detail.getTask().getProviderExecution().getCostStatus());
    }

    @Test
    void getAvailableModelsReturnsConfigurationStatus() {
        givenImageProviders();
        when(providerRouter.getVideoProviders()).thenReturn(List.of());
        when(providerRouter.getAudioProviders()).thenReturn(List.of());

        ModelListResponse models = aigcService.getAvailableModels();

        assertEquals(2, models.getImageModels().size());
        assertEquals("缺少 aigc.providers.google.api-key", models.getImageModels().get(0).getMissingConfig());
        assertEquals(true, models.getImageModels().get(0).getActive());
        assertEquals("mock-image-preview", models.getImageModels().get(1).getConfiguredModel());
        assertEquals("local-demo", models.getImageModels().get(1).getDefaultParams().get("mode"));
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
        assertEquals("缺少 aigc.providers.google.api-key", response.getMissingConfig());
        assertEquals("探测未通过：配置不完整", response.getMessage());
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
        assertEquals(true, response.getRoutable());
        assertEquals("Mock Image Provider", aigcProperties.getImage().getActiveProvider());
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
    void deleteAssetContinuesWhenLocalFileDeleteFails() throws Exception {
        AigcAsset asset = asset("asset-2");
        asset.setUrl("http://localhost:10003/files/images/asset-2.png");
        when(assetRepository.findByAssetId("asset-2")).thenReturn(Optional.of(asset));
        when(storageService.deleteByUrl(asset.getUrl())).thenThrow(new IOException("delete failed"));

        aigcService.deleteAsset("asset-2");

        verify(assetRepository).deleteByAssetId("asset-2");
    }

    @Test
    void deleteAssetRejectsMissingAsset() {
        when(assetRepository.findByAssetId("missing")).thenReturn(Optional.empty());

        AigcException error = assertThrows(AigcException.class, () -> aigcService.deleteAsset("missing"));

        assertEquals(AigcErrorCode.ASSET_NOT_FOUND, error.getErrorCode());
    }

    private AigcAsset asset(String assetId) {
        AigcAsset asset = new AigcAsset();
        asset.setAssetId(assetId);
        asset.setContentType(ContentType.IMAGE);
        asset.setPrompt("prompt");
        asset.setModel("mock-image-preview");
        asset.setIsPublished(false);
        return asset;
    }

    private void givenImageProviders() {
        aigcProperties.getImage().setActiveProvider("google");
        aigcProperties.getProviders().getGoogle().setApiKey(null);

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
}
