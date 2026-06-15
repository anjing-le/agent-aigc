package com.anjing.aigc.service.storage;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.response.StorageStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                new LocalAigcStorageService(properties),
                new OssAigcStorageService(properties),
                mock(AigcStorageAuditLogService.class)
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
        properties.getStorage().getOss().setRetryCount(2);
        properties.getStorage().getOss().setRetryIntervalMs(100L);
        properties.getStorage().getOss().setSignedUrlEnabled(true);
        AigcStorageService storageService = new AigcStorageService(
                properties,
                new LocalAigcStorageService(properties),
                new OssAigcStorageService(properties),
                mock(AigcStorageAuditLogService.class)
        );

        StorageStatusResponse response = storageService.getStorageStatus();

        assertEquals("OSS", response.getActiveMode());
        assertTrue(response.getOss().getConfigured());
        assertTrue(response.getOss().getAvailable());
        assertEquals(true, response.getOss().getEndpointConfigured());
        assertEquals(true, response.getOss().getBucketConfigured());
        assertEquals(true, response.getOss().getCleanupAuditEnabled());
        assertEquals(true, response.getOss().getSignedUrlEnabled());
        assertEquals(2, response.getOss().getRetryCount());
        assertEquals(100L, response.getOss().getRetryIntervalMs());
        assertEquals("OSS adapter 已就绪", response.getOss().getMessage());
    }

    @Test
    void saveBytesDelegatesThroughStorageAdapterBoundary() throws Exception {
        AigcProperties properties = new AigcProperties();
        properties.getStorage().getLocal().setBasePath(tempDir.toString());
        LocalAigcStorageService localStorageService = mock(LocalAigcStorageService.class);
        OssAigcStorageService ossStorageService = mock(OssAigcStorageService.class);
        AigcStorageAuditLogService auditLogService = mock(AigcStorageAuditLogService.class);
        byte[] bytes = new byte[]{1, 2, 3};
        when(localStorageService.saveBytes("images", "asset.png", bytes))
                .thenReturn("http://localhost:10003/files/images/asset.png");
        AigcStorageService storageService = new AigcStorageService(
                properties,
                localStorageService,
                ossStorageService,
                auditLogService
        );

        String url = storageService.saveBytes("images", "asset.png", bytes);

        assertEquals("http://localhost:10003/files/images/asset.png", url);
        verify(localStorageService).saveBytes("images", "asset.png", bytes);
        verify(auditLogService).recordSuccess(
                AigcStorageAuditLogService.ACTION_UPLOAD,
                "LOCAL",
                "images",
                "asset.png",
                "http://localhost:10003/files/images/asset.png",
                3L
        );
    }

    @Test
    void saveBytesDelegatesToOssWhenOssAdapterIsAvailable() throws Exception {
        AigcProperties properties = new AigcProperties();
        properties.getStorage().getLocal().setBasePath(tempDir.toString());
        properties.getStorage().getOss().setEnabled(true);
        properties.getStorage().getOss().setEndpoint("https://oss.example.com");
        properties.getStorage().getOss().setBucketName("agent-aigc");
        LocalAigcStorageService localStorageService = mock(LocalAigcStorageService.class);
        OssAigcStorageService ossStorageService = mock(OssAigcStorageService.class);
        AigcStorageAuditLogService auditLogService = mock(AigcStorageAuditLogService.class);
        byte[] bytes = new byte[]{1, 2, 3};
        when(ossStorageService.isEnabled()).thenReturn(true);
        when(ossStorageService.isConfigured()).thenReturn(true);
        when(ossStorageService.saveBytes("images", "asset.png", bytes))
                .thenReturn("https://cdn.example.com/aigc/images/asset.png");
        AigcStorageService storageService = new AigcStorageService(
                properties,
                localStorageService,
                ossStorageService,
                auditLogService
        );

        String url = storageService.saveBytes("images", "asset.png", bytes);

        assertEquals("https://cdn.example.com/aigc/images/asset.png", url);
        verify(ossStorageService).saveBytes("images", "asset.png", bytes);
        verify(auditLogService).recordSuccess(
                AigcStorageAuditLogService.ACTION_UPLOAD,
                "OSS",
                "images",
                "asset.png",
                "https://cdn.example.com/aigc/images/asset.png",
                3L
        );
    }

    @Test
    void deleteByUrlFallsBackToLocalWhenOssDoesNotOwnUrl() throws Exception {
        AigcProperties properties = new AigcProperties();
        LocalAigcStorageService localStorageService = mock(LocalAigcStorageService.class);
        OssAigcStorageService ossStorageService = mock(OssAigcStorageService.class);
        AigcStorageAuditLogService auditLogService = mock(AigcStorageAuditLogService.class);
        String localUrl = "http://localhost:10003/files/images/asset.png";
        when(ossStorageService.isConfigured()).thenReturn(true);
        when(ossStorageService.deleteByUrl(localUrl)).thenReturn(false);
        when(localStorageService.deleteByUrl(localUrl)).thenReturn(true);
        AigcStorageService storageService = new AigcStorageService(
                properties,
                localStorageService,
                ossStorageService,
                auditLogService
        );

        boolean deleted = storageService.deleteByUrl(localUrl);

        assertTrue(deleted);
        verify(ossStorageService).deleteByUrl(localUrl);
        verify(localStorageService).deleteByUrl(localUrl);
        verify(auditLogService).recordSuccess(
                AigcStorageAuditLogService.ACTION_DELETE_URL,
                "LOCAL",
                null,
                null,
                localUrl,
                null
        );
    }
}
