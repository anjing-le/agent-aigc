package com.anjing.aigc.service;

import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.entity.AigcMaterial;
import com.anjing.aigc.model.response.AigcStorageDownloadResource;
import com.anjing.aigc.repository.AigcAssetRepository;
import com.anjing.aigc.repository.AigcMaterialRepository;
import com.anjing.aigc.service.storage.AigcStorageService;
import com.anjing.model.errorcode.AigcErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AigcDownloadService {

    private final AigcAssetRepository assetRepository;
    private final AigcMaterialRepository materialRepository;
    private final AigcStorageService storageService;
    private final AigcOwnershipService ownershipService;
    private final AigcGalleryAuditLogService galleryAuditLogService;

    public ResponseEntity<Resource> downloadAsset(String assetId) {
        AigcAsset asset = assetRepository.findVisibleByAssetId(
                        assetId,
                        ownershipService.currentOwnerId(),
                        ownershipService.currentTenantId())
                .orElseThrow(() -> new AigcException(AigcErrorCode.ASSET_NOT_FOUND));
        return buildStorageResponse(asset.getUrl(), buildAssetFileName(asset), true);
    }

    public ResponseEntity<Resource> previewAsset(String assetId) {
        AigcAsset asset = assetRepository.findVisibleByAssetId(
                        assetId,
                        ownershipService.currentOwnerId(),
                        ownershipService.currentTenantId())
                .orElseThrow(() -> new AigcException(AigcErrorCode.ASSET_NOT_FOUND));
        return buildStorageResponse(asset.getUrl(), buildAssetFileName(asset), false);
    }

    public ResponseEntity<Resource> previewPublishedAsset(String assetId) {
        AigcAsset asset = assetRepository.findByAssetIdAndIsPublishedTrue(assetId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.ASSET_NOT_FOUND));
        return buildStorageResponse(asset.getUrl(), buildAssetFileName(asset), false);
    }

    public ResponseEntity<Resource> downloadPublishedAsset(String assetId) {
        AigcAsset asset = assetRepository.findByAssetIdAndIsPublishedTrue(assetId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.ASSET_NOT_FOUND));
        ResponseEntity<Resource> response = buildStorageResponse(asset.getUrl(), buildAssetFileName(asset), true);
        galleryAuditLogService.recordSuccess(AigcGalleryAuditLogService.ACTION_PUBLIC_DOWNLOAD, asset);
        return response;
    }

    public ResponseEntity<Resource> downloadMaterial(String materialId) {
        AigcMaterial material = materialRepository.findVisibleByMaterialId(
                        materialId,
                        ownershipService.currentOwnerId(),
                        ownershipService.currentTenantId())
                .orElseThrow(() -> new AigcException(AigcErrorCode.MATERIAL_NOT_FOUND));
        return buildStorageResponse(material.getUrl(), buildMaterialFileName(material), true);
    }

    public ResponseEntity<Resource> previewMaterial(String materialId) {
        AigcMaterial material = materialRepository.findVisibleByMaterialId(
                        materialId,
                        ownershipService.currentOwnerId(),
                        ownershipService.currentTenantId())
                .orElseThrow(() -> new AigcException(AigcErrorCode.MATERIAL_NOT_FOUND));
        return buildStorageResponse(material.getUrl(), buildMaterialFileName(material), false);
    }

    private ResponseEntity<Resource> buildStorageResponse(String url, String fileName, boolean attachment) {
        try {
            AigcStorageDownloadResource download = storageService.resolveDownload(url, fileName);
            if (download.isRedirect()) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(download.getRedirectUri())
                        .build();
            }

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(download.getContentType()));
            ContentDisposition.Builder disposition = attachment
                    ? ContentDisposition.attachment()
                    : ContentDisposition.inline();
            builder.header(HttpHeaders.CONTENT_DISPOSITION, disposition
                    .filename(download.getFileName(), StandardCharsets.UTF_8)
                    .build()
                    .toString());
            if (download.getContentLength() != null) {
                builder.contentLength(download.getContentLength());
            }
            return builder.body(download.getResource());
        } catch (IOException e) {
            throw new AigcException(AigcErrorCode.STORAGE_FILE_NOT_FOUND, "文件不存在或不可访问", e);
        }
    }

    private String buildAssetFileName(AigcAsset asset) {
        if (asset.getContentType() == null) {
            return "aigc-" + asset.getAssetId();
        }
        String extension = switch (asset.getContentType()) {
            case IMAGE -> "png";
            case VIDEO -> "mp4";
            case AUDIO -> "mp3";
            case TEXT -> "txt";
        };
        return "aigc-" + asset.getAssetId() + "." + extension;
    }

    private String buildMaterialFileName(AigcMaterial material) {
        if (material.getOriginalFileName() != null && !material.getOriginalFileName().isBlank()) {
            return material.getOriginalFileName();
        }
        return material.getFileName();
    }
}
