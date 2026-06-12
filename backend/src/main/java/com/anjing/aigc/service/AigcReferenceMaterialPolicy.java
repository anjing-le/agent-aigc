package com.anjing.aigc.service;

import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.entity.AigcMaterial;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.model.errorcode.AigcErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AigcReferenceMaterialPolicy {

    public void validate(ContentType contentType, List<AigcMaterial> materials, List<String> referenceImages) {
        if (isEmpty(materials) && isEmpty(referenceImages)) {
            return;
        }

        switch (contentType) {
            case IMAGE -> validateImageMaterials(materials);
            case VIDEO -> validateVideoMaterials(materials);
            case AUDIO -> rejectAudioReferences();
            case TEXT -> throw unsupported("文本创作不支持参考素材");
        }
    }

    private void validateImageMaterials(List<AigcMaterial> materials) {
        for (AigcMaterial material : emptySafe(materials)) {
            if (!isImage(material.getContentType())) {
                throw unsupported("图片创作仅支持引用图片素材: " + material.getOriginalFileName());
            }
        }
    }

    private void validateVideoMaterials(List<AigcMaterial> materials) {
        for (AigcMaterial material : emptySafe(materials)) {
            if (!isImage(material.getContentType()) && !isVideo(material.getContentType())) {
                throw unsupported("视频创作仅支持引用图片或视频素材: " + material.getOriginalFileName());
            }
        }
    }

    private void rejectAudioReferences() {
        throw unsupported("音频创作暂不支持引用图片或视频素材");
    }

    private boolean isImage(String contentType) {
        return contentType != null && contentType.toLowerCase().startsWith("image/");
    }

    private boolean isVideo(String contentType) {
        return contentType != null && contentType.toLowerCase().startsWith("video/");
    }

    private boolean isEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private List<AigcMaterial> emptySafe(List<AigcMaterial> materials) {
        return materials == null ? List.of() : materials;
    }

    private AigcException unsupported(String message) {
        return new AigcException(AigcErrorCode.MATERIAL_USAGE_UNSUPPORTED, message);
    }
}
