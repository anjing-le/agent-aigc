package com.anjing.aigc.service.storage;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.response.AigcStorageDownloadResource;
import com.anjing.aigc.model.response.StorageBackendStatusResponse;
import com.anjing.aigc.model.response.StorageStatusResponse;
import com.anjing.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AigcStorageService {

    private static final String MODE_LOCAL = "LOCAL";
    private static final String MODE_OSS = "OSS";

    private final AigcProperties aigcProperties;
    private final LocalAigcStorageService localAigcStorageService;
    private final OssAigcStorageService ossAigcStorageService;
    private final AigcStorageAuditLogService auditLogService;

    public String saveBytes(String directory, String fileName, byte[] bytes) throws IOException {
        String backend = getStorageStatus().getActiveMode();
        Long sizeBytes = bytes == null ? null : (long) bytes.length;
        try {
            String url = MODE_OSS.equals(backend)
                    ? ossAigcStorageService.saveBytes(directory, fileName, bytes)
                    : localAigcStorageService.saveBytes(directory, fileName, bytes);
            auditLogService.recordSuccess(
                    AigcStorageAuditLogService.ACTION_UPLOAD,
                    backend,
                    directory,
                    fileName,
                    url,
                    sizeBytes
            );
            return url;
        } catch (IOException e) {
            recordFailure(AigcStorageAuditLogService.ACTION_UPLOAD, backend, directory, fileName, null, sizeBytes, e);
            throw e;
        } catch (RuntimeException e) {
            recordFailure(AigcStorageAuditLogService.ACTION_UPLOAD, backend, directory, fileName, null, sizeBytes, e);
            throw e;
        }
    }

    public boolean deleteFile(String directory, String fileName) throws IOException {
        String backend = getStorageStatus().getActiveMode();
        try {
            boolean deleted = MODE_OSS.equals(backend)
                    ? ossAigcStorageService.deleteFile(directory, fileName)
                    : localAigcStorageService.deleteFile(directory, fileName);
            auditLogService.recordSuccess(
                    AigcStorageAuditLogService.ACTION_DELETE_FILE,
                    backend,
                    directory,
                    fileName,
                    null,
                    null
            );
            return deleted;
        } catch (IOException e) {
            recordFailure(AigcStorageAuditLogService.ACTION_DELETE_FILE, backend, directory, fileName, null, null, e);
            throw e;
        } catch (RuntimeException e) {
            recordFailure(AigcStorageAuditLogService.ACTION_DELETE_FILE, backend, directory, fileName, null, null, e);
            throw e;
        }
    }

    public boolean deleteByUrl(String url) throws IOException {
        if (ossAigcStorageService.isConfigured()) {
            try {
                boolean deletedByOss = ossAigcStorageService.deleteByUrl(url);
                if (deletedByOss) {
                    auditLogService.recordSuccess(
                            AigcStorageAuditLogService.ACTION_DELETE_URL,
                            MODE_OSS,
                            null,
                            null,
                            url,
                            null
                    );
                    return true;
                }
            } catch (IOException e) {
                recordFailure(AigcStorageAuditLogService.ACTION_DELETE_URL, MODE_OSS, null, null, url, null, e);
                throw e;
            } catch (RuntimeException e) {
                recordFailure(AigcStorageAuditLogService.ACTION_DELETE_URL, MODE_OSS, null, null, url, null, e);
                throw e;
            }
        }

        try {
            boolean deletedByLocal = localAigcStorageService.deleteByUrl(url);
            if (deletedByLocal) {
                auditLogService.recordSuccess(
                        AigcStorageAuditLogService.ACTION_DELETE_URL,
                        MODE_LOCAL,
                        null,
                        null,
                        url,
                        null
                );
            }
            return deletedByLocal;
        } catch (IOException e) {
            recordFailure(AigcStorageAuditLogService.ACTION_DELETE_URL, MODE_LOCAL, null, null, url, null, e);
            throw e;
        } catch (RuntimeException e) {
            recordFailure(AigcStorageAuditLogService.ACTION_DELETE_URL, MODE_LOCAL, null, null, url, null, e);
            throw e;
        }
    }

    public AigcStorageDownloadResource resolveDownload(String url, String fileName) throws IOException {
        if (isDataUrl(url)) {
            return resolveDataUrlDownload(url, fileName);
        }

        Resource localResource = localAigcStorageService.getResourceByUrl(url);
        if (localResource != null && localResource.exists()) {
            return AigcStorageDownloadResource.builder()
                    .resource(localResource)
                    .fileName(resolveDownloadFileName(fileName, url))
                    .contentType(resolveContentType(resolveDownloadFileName(fileName, url)))
                    .contentLength(localAigcStorageService.getContentLength(url))
                    .build();
        }

        if (ossAigcStorageService.isConfigured()) {
            String authorizedUrl = ossAigcStorageService.buildAuthorizedDownloadUrl(url);
            if (hasText(authorizedUrl)) {
                return AigcStorageDownloadResource.builder()
                        .redirectUri(URI.create(authorizedUrl))
                        .fileName(resolveDownloadFileName(fileName, url))
                        .contentType(resolveContentType(resolveDownloadFileName(fileName, url)))
                        .build();
            }
        }

        throw new IOException("文件不存在或不在受管存储边界");
    }

    private AigcStorageDownloadResource resolveDataUrlDownload(String url, String fileName) throws IOException {
        int commaIndex = url.indexOf(',');
        if (commaIndex <= "data:".length()) {
            throw new IOException("无效 data URL");
        }
        String metadata = url.substring("data:".length(), commaIndex);
        String payload = url.substring(commaIndex + 1);
        String contentType = resolveDataUrlContentType(metadata);
        byte[] bytes;
        try {
            bytes = metadata.toLowerCase(Locale.ROOT).contains(";base64")
                    ? Base64.getDecoder().decode(payload)
                    : URLDecoder.decode(payload, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IOException("无效 data URL 内容", e);
        }
        return AigcStorageDownloadResource.builder()
                .resource(new ByteArrayResource(bytes))
                .fileName(resolveDownloadFileName(fileName, url))
                .contentType(contentType)
                .contentLength((long) bytes.length)
                .build();
    }

    private String resolveDataUrlContentType(String metadata) {
        if (!hasText(metadata)) {
            return "text/plain";
        }
        String contentType = metadata.split(";", 2)[0].trim();
        return hasText(contentType) ? contentType : "text/plain";
    }

    public StorageStatusResponse getStorageStatus() {
        StorageBackendStatusResponse local = buildLocalStatus();
        StorageBackendStatusResponse oss = buildOssStatus();
        String activeMode = Boolean.TRUE.equals(oss.getEnabled()) && Boolean.TRUE.equals(oss.getAvailable())
                ? MODE_OSS
                : MODE_LOCAL;

        return StorageStatusResponse.builder()
                .activeMode(activeMode)
                .local(local)
                .oss(oss)
                .assetCleanupSupported(resolveCleanupSupported(activeMode, local, oss))
                .materialCleanupSupported(resolveCleanupSupported(activeMode, local, oss))
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
                .cleanupAuditEnabled(aigcProperties.getStorage().getOss().isCleanupAuditEnabled())
                .staticServingEnabled(localConfig.isStaticServingEnabled())
                .basePath(basePath == null ? null : basePath.toString())
                .urlPrefix(localConfig.getUrlPrefix())
                .message(message)
                .build();
    }

    private StorageBackendStatusResponse buildOssStatus() {
        AigcProperties.OssConfig ossConfig = aigcProperties.getStorage().getOss();
        boolean enabled = ossAigcStorageService.isEnabled();
        boolean endpointConfigured = hasText(ossConfig.getEndpoint());
        boolean bucketConfigured = hasText(ossConfig.getBucketName());
        boolean configured = ossAigcStorageService.isConfigured();

        return StorageBackendStatusResponse.builder()
                .backend(MODE_OSS)
                .enabled(enabled)
                .configured(configured)
                .available(configured)
                .readable(configured)
                .writable(configured)
                .cleanupSupported(configured)
                .cleanupAuditEnabled(ossConfig.isCleanupAuditEnabled())
                .provider(ossConfig.getProvider())
                .endpointConfigured(endpointConfigured)
                .bucketConfigured(bucketConfigured)
                .cdnConfigured(hasText(ossConfig.getCdnDomain()))
                .publicRead(ossConfig.isPublicRead())
                .signedUrlEnabled(ossConfig.isSignedUrlEnabled())
                .signedUrlExpirationSeconds(ossConfig.getSignedUrlExpirationSeconds())
                .retryCount(ossConfig.getRetryCount())
                .retryIntervalMs(ossConfig.getRetryIntervalMs())
                .objectKeyPrefix(ossConfig.getObjectKeyPrefix())
                .pathStyleAccess(ossConfig.isPathStyleAccess())
                .message(resolveOssMessage(enabled, configured))
                .build();
    }

    private void recordFailure(String action, String backend, String directory, String fileName,
            String url, Long sizeBytes, Exception error) {
        auditLogService.recordFailure(action, backend, directory, fileName, url, sizeBytes, error);
    }

    private String resolveStatusMessage(String activeMode, StorageBackendStatusResponse local,
            StorageBackendStatusResponse oss) {
        if (MODE_OSS.equals(activeMode)) {
            return "OSS 存储已就绪";
        }
        if (Boolean.TRUE.equals(local.getAvailable())) {
            return "本地存储已就绪";
        }
        if (Boolean.TRUE.equals(oss.getEnabled()) && !Boolean.TRUE.equals(oss.getConfigured())) {
            return "OSS 已启用但配置不完整，回退本地存储";
        }
        return local.getMessage();
    }

    private boolean resolveCleanupSupported(String activeMode, StorageBackendStatusResponse local,
            StorageBackendStatusResponse oss) {
        if (MODE_OSS.equals(activeMode)) {
            return Boolean.TRUE.equals(oss.getCleanupSupported());
        }
        return Boolean.TRUE.equals(local.getCleanupSupported());
    }

    private String resolveOssMessage(boolean enabled, boolean configured) {
        if (!enabled) {
            return "OSS 未启用";
        }
        if (!configured) {
            return "OSS 配置不完整";
        }
        return "OSS adapter 已就绪";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !value.startsWith("<");
    }

    private boolean isDataUrl(String url) {
        return url != null && url.startsWith("data:");
    }

    private String resolveDownloadFileName(String fileName, String url) {
        if (hasText(fileName)) {
            return fileName.trim();
        }
        if (url == null || url.isBlank()) {
            return "aigc-file";
        }
        int queryIndex = url.indexOf('?');
        String withoutQuery = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
        int slashIndex = withoutQuery.lastIndexOf('/');
        String resolved = slashIndex >= 0 ? withoutQuery.substring(slashIndex + 1) : withoutQuery;
        return hasText(resolved) ? resolved : "aigc-file";
    }

    private String resolveContentType(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        return "application/octet-stream";
    }
}
