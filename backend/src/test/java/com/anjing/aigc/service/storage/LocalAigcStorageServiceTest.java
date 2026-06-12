package com.anjing.aigc.service.storage;

import com.anjing.aigc.config.AigcProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalAigcStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void deleteByUrlRemovesLocalFileUnderConfiguredPrefix() throws Exception {
        AigcProperties properties = new AigcProperties();
        properties.getStorage().getLocal().setBasePath(tempDir.toString());
        properties.getStorage().getLocal().setUrlPrefix("http://localhost:10003/files");
        LocalAigcStorageService storageService = new LocalAigcStorageService(properties);

        Path imageDir = tempDir.resolve("images");
        Files.createDirectories(imageDir);
        Path imageFile = imageDir.resolve("asset.png");
        Files.write(imageFile, new byte[]{1, 2, 3});

        boolean deleted = storageService.deleteByUrl("http://localhost:10003/files/images/asset.png");

        assertTrue(deleted);
        assertFalse(Files.exists(imageFile));
    }

    @Test
    void deleteByUrlIgnoresExternalUrl() throws Exception {
        AigcProperties properties = new AigcProperties();
        properties.getStorage().getLocal().setBasePath(tempDir.toString());
        properties.getStorage().getLocal().setUrlPrefix("http://localhost:10003/files");
        LocalAigcStorageService storageService = new LocalAigcStorageService(properties);

        boolean deleted = storageService.deleteByUrl("https://cdn.example.com/images/asset.png");

        assertFalse(deleted);
    }
}
