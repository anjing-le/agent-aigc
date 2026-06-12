package com.anjing.aigc.service;

import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.entity.AigcMaterial;
import com.anjing.aigc.model.response.MaterialUploadResponse;
import com.anjing.aigc.repository.AigcMaterialRepository;
import com.anjing.aigc.service.storage.LocalAigcStorageService;
import com.anjing.model.errorcode.AigcErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AigcMaterialServiceTest {

    private final LocalAigcStorageService storageService = mock(LocalAigcStorageService.class);
    private final AigcMaterialRepository materialRepository = mock(AigcMaterialRepository.class);
    private final AigcMaterialService materialService = new AigcMaterialService(storageService, materialRepository);

    @Test
    void uploadMaterialStoresSupportedImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        when(storageService.saveBytes(eq("materials"), any(String.class), eq(file.getBytes())))
                .thenReturn("/uploads/aigc/materials/material-demo.png");

        MaterialUploadResponse response = materialService.uploadMaterial(file);

        assertEquals("/uploads/aigc/materials/material-demo.png", response.getUrl());
        assertEquals("cover.png", response.getOriginalFileName());
        assertEquals("image/png", response.getContentType());
        assertEquals(3, response.getSize());
        verify(storageService).saveBytes(eq("materials"), any(String.class), eq(file.getBytes()));
        verify(materialRepository).save(any(AigcMaterial.class));
    }

    @Test
    void uploadMaterialRejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                new byte[]{1}
        );

        AigcException error = assertThrows(AigcException.class, () -> materialService.uploadMaterial(file));

        assertEquals(AigcErrorCode.MATERIAL_TYPE_UNSUPPORTED, error.getErrorCode());
    }

    @Test
    void uploadMaterialRejectsOversizedImage() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(20L * 1024 * 1024 + 1);

        AigcException error = assertThrows(AigcException.class, () -> materialService.uploadMaterial(file));

        assertEquals(AigcErrorCode.MATERIAL_SIZE_EXCEEDED, error.getErrorCode());
    }

    @Test
    void deleteMaterialRemovesExistingRecord() {
        AigcMaterial material = new AigcMaterial();
        material.setMaterialId("mat-1");
        when(materialRepository.findByMaterialId("mat-1")).thenReturn(Optional.of(material));

        materialService.deleteMaterial("mat-1");

        verify(materialRepository).deleteByMaterialId("mat-1");
    }

    @Test
    void deleteMaterialRejectsMissingRecord() {
        when(materialRepository.findByMaterialId("missing")).thenReturn(Optional.empty());

        AigcException error = assertThrows(AigcException.class, () -> materialService.deleteMaterial("missing"));

        assertEquals(AigcErrorCode.MATERIAL_NOT_FOUND, error.getErrorCode());
    }
}
