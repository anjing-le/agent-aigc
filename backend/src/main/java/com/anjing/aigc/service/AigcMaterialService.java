package com.anjing.aigc.service;

import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.response.MaterialUploadResponse;
import com.anjing.aigc.service.storage.LocalAigcStorageService;
import com.anjing.model.errorcode.AigcErrorCode;
import com.anjing.util.IdUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
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

    private final LocalAigcStorageService localAigcStorageService;

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
            String url = localAigcStorageService.saveBytes("materials", fileName, file.getBytes());
            return MaterialUploadResponse.builder()
                    .url(url)
                    .fileName(fileName)
                    .contentType(contentType)
                    .size(file.getSize())
                    .build();
        } catch (IOException e) {
            throw new AigcException(AigcErrorCode.MATERIAL_SAVE_FAILED, "素材保存失败，请稍后重试", e);
        }
    }
}
