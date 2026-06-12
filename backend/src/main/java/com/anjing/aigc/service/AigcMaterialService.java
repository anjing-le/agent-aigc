package com.anjing.aigc.service;

import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.response.MaterialUploadResponse;
import com.anjing.aigc.service.storage.LocalAigcStorageService;
import com.anjing.util.IdUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AigcMaterialService {

    private final LocalAigcStorageService localAigcStorageService;

    public MaterialUploadResponse uploadMaterial(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AigcException("MATERIAL_EMPTY", "请上传素材文件");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            throw new AigcException("MATERIAL_TYPE_UNSUPPORTED", "仅支持图片或视频素材");
        }

        String fileName = "material-" + IdUtils.uuid() + "." + resolveExtension(file.getOriginalFilename(), contentType);
        try {
            String url = localAigcStorageService.saveBytes("materials", fileName, file.getBytes());
            return MaterialUploadResponse.builder()
                    .url(url)
                    .fileName(fileName)
                    .contentType(contentType)
                    .size(file.getSize())
                    .build();
        } catch (IOException e) {
            throw new AigcException("MATERIAL_SAVE_FAILED", "素材保存失败: " + e.getMessage());
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (extension.matches("[a-z0-9]{1,8}")) {
                return extension;
            }
        }
        if (contentType.contains("png")) return "png";
        if (contentType.contains("webp")) return "webp";
        if (contentType.contains("gif")) return "gif";
        if (contentType.contains("mp4")) return "mp4";
        if (contentType.contains("quicktime")) return "mov";
        return contentType.startsWith("video/") ? "mp4" : "jpg";
    }
}
