package com.anjing.aigc.service.storage;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.response.StorageBackendStatusResponse;
import com.anjing.aigc.model.response.StorageStatusResponse;
import com.anjing.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class AigcStorageService {

    private static final String MODE_LOCAL = "LOCAL";
    private static final String MODE_OSS = "OSS";

    private final AigcProperties aigcProperties;

    public StorageStatusResponse getStorageStatus() {
        StorageBackendStatusResponse local = buildLocalStatus();
        StorageBackendStatusResponse oss = buildOssStatus();
        String activeMode = Boolean.TRUE.equals(oss.getEnabled()) && Boolean.TRUE.equals(oss.getConfigured())
                ? MODE_OSS
                : MODE_LOCAL;

        return StorageStatusResponse.builder()
                .activeMode(activeMode)
                .local(local)
                .oss(oss)
                .assetCleanupSupported(Boolean.TRUE.equals(local.getCleanupSupported()))
                .materialCleanupSupported(Boolean.TRUE.equals(local.getCleanupSupported()))
                .message(resolveStatusMessage(activeMode, local, oss))
                .checkedAt(DateUtils.nowIso())
                .build();
    }

    private StorageBackendStatusResponse buildLocalStatus() {
        AigcProperties.LocalStorageConfig localConfig = aigcProperties.getStorage().getLocal();
        boolean enabled = localConfig.isEnabled();
        boolean configured = hasText(localConfig.getBasePath()) && hasText(localConfig.getUrlPrefix());
        Path basePath = hasText(localConfig.getBasePath())
                ? Path.of(localConfig.getBasePath()).toAbsolutePath().normalize()
                : null;
        boolean readable = false;
        boolean writable = false;
        String message;

        if (!enabled) {
            message = "本地存储未启用";
        } else if (!configured || basePath == null) {
            message = "本地存储配置不完整";
        } else {
            try {
                Files.createDirectories(basePath);
                readable = Files.isReadable(basePath);
                writable = Files.isWritable(basePath);
                message = writable ? "本地存储可写" : "本地存储不可写";
            } catch (IOException | SecurityException e) {
                message = "本地存储目录不可用: " + e.getMessage();
            }
        }

        return StorageBackendStatusResponse.builder()
                .backend(MODE_LOCAL)
                .enabled(enabled)
                .configured(configured)
                .available(enabled && configured && readable && writable)
                .readable(readable)
                .writable(writable)
                .cleanupSupported(enabled && configured)
                .basePath(basePath == null ? null : basePath.toString())
                .urlPrefix(localConfig.getUrlPrefix())
                .message(message)
                .build();
    }

    private StorageBackendStatusResponse buildOssStatus() {
        AigcProperties.OssConfig ossConfig = aigcProperties.getStorage().getOss();
        boolean enabled = ossConfig.isEnabled();
        boolean endpointConfigured = hasText(ossConfig.getEndpoint());
        boolean bucketConfigured = hasText(ossConfig.getBucketName());
        boolean credentialConfigured = hasText(ossConfig.getAccessKeyId()) && hasText(ossConfig.getAccessKeySecret());
        boolean configured = enabled && endpointConfigured && bucketConfigured && credentialConfigured;

        return StorageBackendStatusResponse.builder()
                .backend(MODE_OSS)
                .enabled(enabled)
                .configured(configured)
                .available(false)
                .readable(false)
                .writable(false)
                .cleanupSupported(false)
                .provider(ossConfig.getProvider())
                .endpointConfigured(endpointConfigured)
                .bucketConfigured(bucketConfigured)
                .cdnConfigured(hasText(ossConfig.getCdnDomain()))
                .message(resolveOssMessage(enabled, configured))
                .build();
    }

    private String resolveStatusMessage(String activeMode, StorageBackendStatusResponse local,
            StorageBackendStatusResponse oss) {
        if (MODE_OSS.equals(activeMode)) {
            return "OSS 配置已就绪，当前版本仍以本地存储 adapter 执行写入";
        }
        if (Boolean.TRUE.equals(local.getAvailable())) {
            return "本地存储已就绪";
        }
        if (Boolean.TRUE.equals(oss.getEnabled()) && !Boolean.TRUE.equals(oss.getConfigured())) {
            return "OSS 已启用但配置不完整，回退本地存储";
        }
        return local.getMessage();
    }

    private String resolveOssMessage(boolean enabled, boolean configured) {
        if (!enabled) {
            return "OSS 未启用";
        }
        if (!configured) {
            return "OSS 配置不完整";
        }
        return "OSS 配置已就绪，adapter 待接入";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !value.startsWith("<");
    }
}
