package com.anjing.aigc.service;

import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.entity.AigcMaterial;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.response.AigcStorageDownloadResource;
import com.anjing.aigc.repository.AigcAssetRepository;
import com.anjing.aigc.repository.AigcMaterialRepository;
import com.anjing.aigc.service.storage.AigcStorageService;
import com.anjing.model.errorcode.AigcErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AigcDownloadServiceTest {

    private final AigcAssetRepository assetRepository = mock(AigcAssetRepository.class);
    private final AigcMaterialRepository materialRepository = mock(AigcMaterialRepository.class);
    private final AigcStorageService storageService = mock(AigcStorageService.class);
    private final AigcOwnershipService ownershipService = new AigcOwnershipService();
    private final AigcDownloadService downloadService = new AigcDownloadService(
            assetRepository,
            materialRepository,
            storageService,
            ownershipService
    );

    @Test
    void downloadAssetUsesVisibleAssetAndStorageBoundary() throws Exception {
        AigcAsset asset = new AigcAsset();
        asset.setAssetId("asset-1");
        asset.setContentType(ContentType.IMAGE);
        asset.setUrl("http://localhost:10003/files/images/asset-1.png");
        when(assetRepository.findVisibleByAssetId("asset-1", null, null)).thenReturn(Optional.of(asset));
        when(storageService.resolveDownload(asset.getUrl(), "aigc-asset-1.png"))
                .thenReturn(AigcStorageDownloadResource.builder()
                        .resource(new ByteArrayResource(new byte[]{1, 2, 3}))
                        .fileName("aigc-asset-1.png")
                        .contentType("image/png")
                        .contentLength(3L)
                        .build());

        ResponseEntity<?> response = downloadService.downloadAsset("asset-1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(3L, response.getHeaders().getContentLength());
        assertEquals("image/png", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .startsWith("attachment"));
    }

    @Test
    void previewAssetUsesInlineDispositionAfterVisibilityCheck() throws Exception {
        AigcAsset asset = new AigcAsset();
        asset.setAssetId("asset-1");
        asset.setContentType(ContentType.IMAGE);
        asset.setUrl("http://localhost:10003/files/images/asset-1.png");
        when(assetRepository.findVisibleByAssetId("asset-1", null, null)).thenReturn(Optional.of(asset));
        when(storageService.resolveDownload(asset.getUrl(), "aigc-asset-1.png"))
                .thenReturn(AigcStorageDownloadResource.builder()
                        .resource(new ByteArrayResource(new byte[]{1, 2, 3}))
                        .fileName("aigc-asset-1.png")
                        .contentType("image/png")
                        .contentLength(3L)
                        .build());

        ResponseEntity<?> response = downloadService.previewAsset("asset-1");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .startsWith("inline"));
    }

    @Test
    void previewPublishedAssetRequiresPublishedAsset() {
        when(assetRepository.findByAssetIdAndIsPublishedTrue("asset-1")).thenReturn(Optional.empty());

        AigcException error = assertThrows(AigcException.class,
                () -> downloadService.previewPublishedAsset("asset-1"));

        assertEquals(AigcErrorCode.ASSET_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void previewPublishedAssetUsesStorageBoundaryWithoutOwnerContext() throws Exception {
        AigcAsset asset = new AigcAsset();
        asset.setAssetId("asset-1");
        asset.setContentType(ContentType.IMAGE);
        asset.setUrl("http://localhost:10003/files/images/asset-1.png");
        when(assetRepository.findByAssetIdAndIsPublishedTrue("asset-1")).thenReturn(Optional.of(asset));
        when(storageService.resolveDownload(asset.getUrl(), "aigc-asset-1.png"))
                .thenReturn(AigcStorageDownloadResource.builder()
                        .resource(new ByteArrayResource(new byte[]{1, 2, 3}))
                        .fileName("aigc-asset-1.png")
                        .contentType("image/png")
                        .contentLength(3L)
                        .build());

        ResponseEntity<?> response = downloadService.previewPublishedAsset("asset-1");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .startsWith("inline"));
    }

    @Test
    void downloadMaterialRejectsMissingVisibleMaterial() {
        when(materialRepository.findVisibleByMaterialId("missing", null, null)).thenReturn(Optional.empty());

        AigcException error = assertThrows(AigcException.class,
                () -> downloadService.downloadMaterial("missing"));

        assertEquals(AigcErrorCode.MATERIAL_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void downloadMaterialUsesOriginalFileName() throws Exception {
        AigcMaterial material = new AigcMaterial();
        material.setMaterialId("mat-1");
        material.setOriginalFileName("cover.png");
        material.setFileName("material-cover.png");
        material.setUrl("http://localhost:10003/files/materials/material-cover.png");
        when(materialRepository.findVisibleByMaterialId("mat-1", null, null)).thenReturn(Optional.of(material));
        when(storageService.resolveDownload(material.getUrl(), "cover.png"))
                .thenReturn(AigcStorageDownloadResource.builder()
                        .resource(new ByteArrayResource(new byte[]{1}))
                        .fileName("cover.png")
                        .contentType("image/png")
                        .contentLength(1L)
                        .build());

        ResponseEntity<?> response = downloadService.downloadMaterial("mat-1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("image/png", response.getHeaders().getContentType().toString());
    }

    @Test
    void previewMaterialUsesOriginalFileNameWithInlineDisposition() throws Exception {
        AigcMaterial material = new AigcMaterial();
        material.setMaterialId("mat-1");
        material.setOriginalFileName("cover.png");
        material.setFileName("material-cover.png");
        material.setUrl("http://localhost:10003/files/materials/material-cover.png");
        when(materialRepository.findVisibleByMaterialId("mat-1", null, null)).thenReturn(Optional.of(material));
        when(storageService.resolveDownload(material.getUrl(), "cover.png"))
                .thenReturn(AigcStorageDownloadResource.builder()
                        .resource(new ByteArrayResource(new byte[]{1}))
                        .fileName("cover.png")
                        .contentType("image/png")
                        .contentLength(1L)
                        .build());

        ResponseEntity<?> response = downloadService.previewMaterial("mat-1");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .startsWith("inline"));
    }
}
