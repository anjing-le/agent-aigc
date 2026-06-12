package com.anjing.aigc.service;

import com.anjing.aigc.agent.RoutingAgent;
import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.enums.TaskStatus;
import com.anjing.aigc.model.response.AssetDetailResponse;
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
    private final AigcTaskRepository taskRepository = mock(AigcTaskRepository.class);
    private final AigcAssetRepository assetRepository = mock(AigcAssetRepository.class);
    private final AigcMaterialRepository materialRepository = mock(AigcMaterialRepository.class);
    private final AigcReferenceMaterialPolicy referenceMaterialPolicy = mock(AigcReferenceMaterialPolicy.class);
    private final LocalAigcStorageService storageService = mock(LocalAigcStorageService.class);
    private final AigcServiceImpl aigcService = new AigcServiceImpl(
            routingAgent,
            taskExecutor,
            providerRouter,
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
        task.setStatus(TaskStatus.COMPLETED);
        task.setProgress(100);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        when(taskRepository.findByAssetId("asset-0")).thenReturn(Optional.of(task));

        AssetDetailResponse detail = aigcService.getAssetDetail("asset-0");

        assertEquals("asset-0", detail.getAsset().getId());
        assertEquals("task-0", detail.getTask().getTaskId());
        assertEquals(TaskStatus.COMPLETED, detail.getTask().getStatus());
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
}
