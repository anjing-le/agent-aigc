package com.anjing.aigc.service;

import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.entity.AigcMaterial;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.model.errorcode.AigcErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AigcReferenceMaterialPolicyTest {

    private final AigcReferenceMaterialPolicy policy = new AigcReferenceMaterialPolicy();

    @Test
    void imageCreationAcceptsImageMaterial() {
        assertDoesNotThrow(() -> policy.validate(
                ContentType.IMAGE,
                List.of(material("cover.png", "image/png")),
                List.of()
        ));
    }

    @Test
    void imageCreationRejectsVideoMaterial() {
        AigcException error = assertThrows(AigcException.class, () -> policy.validate(
                ContentType.IMAGE,
                List.of(material("clip.mp4", "video/mp4")),
                List.of()
        ));

        assertEquals(AigcErrorCode.MATERIAL_USAGE_UNSUPPORTED, error.getErrorCode());
    }

    @Test
    void videoCreationAcceptsImageAndVideoMaterials() {
        assertDoesNotThrow(() -> policy.validate(
                ContentType.VIDEO,
                List.of(
                        material("cover.png", "image/png"),
                        material("clip.mp4", "video/mp4")
                ),
                List.of()
        ));
    }

    @Test
    void audioCreationRejectsVisualReferences() {
        AigcException error = assertThrows(AigcException.class, () -> policy.validate(
                ContentType.AUDIO,
                List.of(material("cover.png", "image/png")),
                List.of()
        ));

        assertEquals(AigcErrorCode.MATERIAL_USAGE_UNSUPPORTED, error.getErrorCode());
    }

    @Test
    void audioCreationRejectsRawReferenceUrls() {
        AigcException error = assertThrows(AigcException.class, () -> policy.validate(
                ContentType.AUDIO,
                List.of(),
                List.of("/uploads/aigc/materials/cover.png")
        ));

        assertEquals(AigcErrorCode.MATERIAL_USAGE_UNSUPPORTED, error.getErrorCode());
    }

    private AigcMaterial material(String fileName, String contentType) {
        AigcMaterial material = new AigcMaterial();
        material.setMaterialId("mat-" + fileName);
        material.setOriginalFileName(fileName);
        material.setFileName(fileName);
        material.setContentType(contentType);
        material.setUrl("/uploads/aigc/materials/" + fileName);
        material.setSize(1024L);
        return material;
    }
}
