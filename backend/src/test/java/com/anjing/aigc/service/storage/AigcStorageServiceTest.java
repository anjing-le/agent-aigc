package com.anjing.aigc.service.storage;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.response.StorageStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AigcStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void getStorageStatusReportsWritableLocalStorage() {
        AigcProperties properties = new AigcProperties();
        properties.getStorage().getLocal().setBasePath(tempDir.toString());
        properties.getStorage().getLocal().setUrlPrefix("http://localhost:10003/files");
        AigcStorageService storageService = new AigcStorageService(
                properties,
                new LocalAigcStorageService(properties)
        );

        StorageStatusResponse response = storageService.getStorageStatus();

        assertEquals("LOCAL", response.getActiveMode());
        assertTrue(response.getLocal().getAvailable());
        assertTrue(response.getLocal().getWritable());
        assertTrue(response.getAssetCleanupSupported());
        assertEquals("OSS 未启用", response.getOss().getMessage());
    }

    @Test
    void getStorageStatusKeepsOssCredentialsOutOfResponse() {
        AigcProperties properties = new AigcProperties();
        properties.getStorage().getLocal().setBasePath(tempDir.toString());
        properties.getStorage().getOss().setEnabled(true);
        properties.getStorage().getOss().setEndpoint("https://oss.example.com");
        properties.getStorage().getOss().setBucketName("agent-aigc");
        properties.getStorage().getOss().setAccessKeyId("access-key-id");
        properties.getStorage().getOss().setAccessKeySecret("secret-value");
        AigcStorageService storageService = new AigcStorageService(
                properties,
                new LocalAigcStorageService(properties)
        );

        StorageStatusResponse response = storageService.getStorageStatus();

        assertEquals("LOCAL", response.getActiveMode());
        assertTrue(response.getOss().getConfigured());
        assertFalse(response.getOss().getAvailable());
        assertEquals(true, response.getOss().getEndpointConfigured());
        assertEquals(true, response.getOss().getBucketConfigured());
        assertEquals("OSS 配置已就绪，adapter 待接入", response.getOss().getMessage());
    }

    @Test
    void saveBytesDelegatesThroughStorageAdapterBoundary() throws Exception {
        AigcProperties properties = new AigcProperties();
        properties.getStorage().getLocal().setBasePath(tempDir.toString());
        LocalAigcStorageService localStorageService = mock(LocalAigcStorageService.class);
        byte[] bytes = new byte[]{1, 2, 3};
        when(localStorageService.saveBytes("images", "asset.png", bytes))
                .thenReturn("http://localhost:10003/files/images/asset.png");
        AigcStorageService storageService = new AigcStorageService(properties, localStorageService);

        String url = storageService.saveBytes("images", "asset.png", bytes);

        assertEquals("http://localhost:10003/files/images/asset.png", url);
        verify(localStorageService).saveBytes("images", "asset.png", bytes);
    }
}
