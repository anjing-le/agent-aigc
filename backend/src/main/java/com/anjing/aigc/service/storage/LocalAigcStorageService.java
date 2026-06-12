package com.anjing.aigc.service.storage;

import com.anjing.aigc.config.AigcProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalAigcStorageService {

    private final AigcProperties aigcProperties;

    public String saveBase64(String directory, String fileName, String base64Data) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64Data);
        return saveBytes(directory, fileName, bytes);
    }

    public String saveBytes(String directory, String fileName, byte[] bytes) throws IOException {
        var localConfig = aigcProperties.getStorage().getLocal();
        if (!localConfig.isEnabled()) {
            throw new IOException("本地存储未启用");
        }

        Path outputDir = Path.of(localConfig.getBasePath(), directory).toAbsolutePath().normalize();
        Files.createDirectories(outputDir);

        Path outputPath = outputDir.resolve(fileName).normalize();
        Files.write(outputPath, bytes);

        log.debug("AIGC 文件已保存: {}, 大小: {} bytes", outputPath, bytes.length);
        return buildUrl(directory, fileName);
    }

    public boolean deleteFile(String directory, String fileName) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }

        var localConfig = aigcProperties.getStorage().getLocal();
        if (!localConfig.isEnabled()) {
            return false;
        }

        Path outputDir = Path.of(localConfig.getBasePath(), directory).toAbsolutePath().normalize();
        Path outputPath = outputDir.resolve(fileName).normalize();
        if (!outputPath.startsWith(outputDir)) {
            throw new IOException("非法文件路径: " + fileName);
        }

        boolean deleted = Files.deleteIfExists(outputPath);
        log.debug("AIGC 文件删除结果: {}, path={}", deleted, outputPath);
        return deleted;
    }

    private String buildUrl(String directory, String fileName) {
        String prefix = aigcProperties.getStorage().getLocal().getUrlPrefix();
        String normalizedPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        return normalizedPrefix + "/" + directory + "/" + fileName;
    }
}
