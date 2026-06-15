package com.anjing.aigc.service;

import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.dto.MaterialDTO;
import com.anjing.aigc.model.entity.AigcMaterial;
import com.anjing.aigc.model.response.MaterialUploadResponse;
import com.anjing.aigc.repository.AigcMaterialRepository;
import com.anjing.aigc.service.storage.AigcStorageService;
import com.anjing.model.errorcode.AigcErrorCode;
import com.anjing.util.DateUtils;
import com.anjing.util.IdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.anjing.model.response.PageResult;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AigcMaterialService {

    private static final long IMAGE_MAX_BYTES = 20L * 1024 * 1024;
    private static final long VIDEO_MAX_BYTES = 100L * 1024 * 1024;
    private static final Map<String, String> SUPPORTED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "video/mp4", "mp4",
            "video/quicktime", "mov",
            "video/webm", "webm"
    );

    private final AigcStorageService aigcStorageService;
    private final AigcMaterialRepository materialRepository;

    public PageResult<MaterialDTO> getMaterialList(Integer current, Integer size, String contentType) {
        int pageNumber = current != null && current > 0 ? current - 1 : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, 100) : 20;
        PageRequest pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AigcMaterial> page;
        if (contentType == null || contentType.isBlank()) {
            page = materialRepository.findAll(pageable);
        } else {
            page = materialRepository.findByContentTypeStartingWith(contentType.trim().toLowerCase() + "/", pageable);
        }

        return PageResult.of(
                page.getContent().stream().map(this::toDTO).toList(),
                page.getTotalElements(),
                current != null && current > 0 ? current : 1,
                pageSize
        );
    }

    @Transactional
    public void deleteMaterial(String materialId) {
        AigcMaterial material = materialRepository.findByMaterialId(materialId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.MATERIAL_NOT_FOUND));
        try {
            aigcStorageService.deleteByUrl(material.getUrl());
        } catch (IOException e) {
            log.warn("素材文件删除失败，继续删除素材记录: materialId={}, url={}",
                    material.getMaterialId(), material.getUrl(), e);
        }
        materialRepository.deleteByMaterialId(material.getMaterialId());
    }

    public MaterialUploadResponse uploadMaterial(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AigcException(AigcErrorCode.MATERIAL_EMPTY);
        }

        String contentType = file.getContentType();
        String extension = contentType == null ? null : SUPPORTED_CONTENT_TYPES.get(contentType.toLowerCase());
        if (extension == null) {
            throw new AigcException(AigcErrorCode.MATERIAL_TYPE_UNSUPPORTED);
        }

        long maxBytes = contentType.startsWith("video/") ? VIDEO_MAX_BYTES : IMAGE_MAX_BYTES;
        if (file.getSize() > maxBytes) {
            throw new AigcException(
                    AigcErrorCode.MATERIAL_SIZE_EXCEEDED,
                    "素材文件过大，图片不超过 20MB，视频不超过 100MB"
            );
        }

        String fileName = "material-" + IdUtils.uuid() + "." + extension;
        try {
            String url = aigcStorageService.saveBytes("materials", fileName, file.getBytes());
            AigcMaterial material = new AigcMaterial();
            material.setMaterialId(IdUtils.uuid());
            material.setFileName(fileName);
            material.setOriginalFileName(file.getOriginalFilename());
            material.setContentType(contentType);
            material.setSize(file.getSize());
            material.setUrl(url);
            material.setCreatedAt(DateUtils.nowLocalDateTime());
            materialRepository.save(material);

            return MaterialUploadResponse.builder()
                    .materialId(material.getMaterialId())
                    .url(url)
                    .fileName(fileName)
                    .originalFileName(material.getOriginalFileName())
                    .contentType(contentType)
                    .size(file.getSize())
                    .createdAt(material.getCreatedAt().toString())
                    .build();
        } catch (IOException e) {
            throw new AigcException(AigcErrorCode.MATERIAL_SAVE_FAILED, "素材保存失败，请稍后重试", e);
        }
    }

    private MaterialDTO toDTO(AigcMaterial material) {
        return MaterialDTO.builder()
                .id(material.getMaterialId())
                .url(material.getUrl())
                .fileName(material.getFileName())
                .originalFileName(material.getOriginalFileName())
                .contentType(material.getContentType())
                .size(material.getSize())
                .createdAt(material.getCreatedAt().toString())
                .build();
    }
}
