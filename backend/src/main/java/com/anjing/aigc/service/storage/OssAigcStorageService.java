package com.anjing.aigc.service.storage;

import com.anjing.aigc.config.AigcProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssAigcStorageService {

    private final AigcProperties aigcProperties;

    private volatile S3Client s3Client;

    public boolean isEnabled() {
        return config().isEnabled();
    }

    public boolean isConfigured() {
        AigcProperties.OssConfig ossConfig = config();
        return ossConfig.isEnabled()
                && hasText(ossConfig.getEndpoint())
                && hasText(ossConfig.getAccessKeyId())
                && hasText(ossConfig.getAccessKeySecret())
                && hasText(ossConfig.getBucketName());
    }

    public String saveBytes(String directory, String fileName, byte[] bytes) throws IOException {
        ensureConfigured();
        String objectKey = buildObjectKey(directory, fileName);
        AigcProperties.OssConfig ossConfig = config();

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(ossConfig.getBucketName())
                .key(objectKey)
                .contentType(resolveContentType(fileName))
                .contentLength((long) bytes.length);
        if (ossConfig.isPublicRead()) {
            requestBuilder.acl(ObjectCannedACL.PUBLIC_READ);
        }

        executeWithRetry("putObject", objectKey,
                () -> client().putObject(requestBuilder.build(), RequestBody.fromBytes(bytes)));
        log.info("AIGC 文件已上传 OSS: provider={}, bucket={}, key={}, size={} bytes",
                ossConfig.getProvider(), ossConfig.getBucketName(), objectKey, bytes.length);
        return buildPublicUrl(objectKey);
    }

    public boolean deleteFile(String directory, String fileName) throws IOException {
        ensureConfigured();
        String objectKey = buildObjectKey(directory, fileName);
        return deleteObject(objectKey);
    }

    public boolean deleteByUrl(String url) throws IOException {
        if (url == null || url.isBlank()) {
            return false;
        }
        ensureConfigured();
        String objectKey = resolveObjectKeyFromUrl(url);
        if (!hasText(objectKey)) {
            return false;
        }
        return deleteObject(objectKey);
    }

    private boolean deleteObject(String objectKey) throws IOException {
        AigcProperties.OssConfig ossConfig = config();
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(ossConfig.getBucketName())
                .key(objectKey)
                .build();
        executeWithRetry("deleteObject", objectKey, () -> client().deleteObject(request));
        log.info("AIGC OSS 文件已删除: provider={}, bucket={}, key={}",
                ossConfig.getProvider(), ossConfig.getBucketName(), objectKey);
        return true;
    }

    private S3Client client() {
        S3Client current = s3Client;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (s3Client == null) {
                AigcProperties.OssConfig ossConfig = config();
                s3Client = S3Client.builder()
                        .endpointOverride(URI.create(ossConfig.getEndpoint()))
                        .region(Region.of(hasText(ossConfig.getRegion()) ? ossConfig.getRegion() : "us-east-1"))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(ossConfig.getAccessKeyId(), ossConfig.getAccessKeySecret())
                        ))
                        .serviceConfiguration(S3Configuration.builder()
                                .pathStyleAccessEnabled(ossConfig.isPathStyleAccess())
                                .build())
                        .build();
            }
            return s3Client;
        }
    }

    private String buildObjectKey(String directory, String fileName) {
        String prefix = trimSlashes(config().getObjectKeyPrefix());
        String normalizedDirectory = trimSlashes(directory);
        String normalizedFileName = trimSlashes(fileName);
        String path = normalizedDirectory.isBlank() ? normalizedFileName : normalizedDirectory + "/" + normalizedFileName;
        return prefix.isBlank() ? path : prefix + "/" + path;
    }

    private String buildPublicUrl(String objectKey) {
        AigcProperties.OssConfig ossConfig = config();
        if (hasText(ossConfig.getCdnDomain())) {
            return normalizeBaseUrl(ossConfig.getCdnDomain()) + "/" + objectKey;
        }

        String endpoint = normalizeBaseUrl(ossConfig.getEndpoint());
        if (ossConfig.isPathStyleAccess()) {
            return endpoint + "/" + ossConfig.getBucketName() + "/" + objectKey;
        }

        URI endpointUri = URI.create(endpoint);
        String scheme = endpointUri.getScheme() == null ? "https" : endpointUri.getScheme();
        String host = endpointUri.getHost() == null ? endpoint.replaceFirst("^https?://", "") : endpointUri.getHost();
        int port = endpointUri.getPort();
        String authority = port > 0 ? ossConfig.getBucketName() + "." + host + ":" + port
                : ossConfig.getBucketName() + "." + host;
        return scheme + "://" + authority + "/" + objectKey;
    }

    private String resolveObjectKeyFromUrl(String url) {
        AigcProperties.OssConfig ossConfig = config();
        String normalizedCdn = hasText(ossConfig.getCdnDomain()) ? normalizeBaseUrl(ossConfig.getCdnDomain()) + "/" : null;
        if (normalizedCdn != null && url.startsWith(normalizedCdn)) {
            return url.substring(normalizedCdn.length());
        }

        String endpoint = normalizeBaseUrl(ossConfig.getEndpoint());
        String pathStylePrefix = endpoint + "/" + ossConfig.getBucketName() + "/";
        if (url.startsWith(pathStylePrefix)) {
            return url.substring(pathStylePrefix.length());
        }

        try {
            URI uri = URI.create(url);
            URI endpointUri = URI.create(endpoint);
            String endpointHost = endpointUri.getHost();
            String requestHost = uri.getHost();
            if (!hasText(endpointHost) || !hasText(requestHost)) {
                return null;
            }

            String path = uri.getPath();
            if (path == null || path.length() <= 1) {
                return null;
            }

            String virtualHost = ossConfig.getBucketName() + "." + endpointHost;
            if (requestHost.equalsIgnoreCase(virtualHost)) {
                return path.substring(1);
            }

            if (!requestHost.equalsIgnoreCase(endpointHost)) {
                return null;
            }

            String withoutLeadingSlash = path.substring(1);
            String bucketPrefix = ossConfig.getBucketName() + "/";
            if (withoutLeadingSlash.startsWith(bucketPrefix)) {
                return withoutLeadingSlash.substring(bucketPrefix.length());
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    private void executeWithRetry(String operation, String objectKey, StorageOperation operationCall) throws IOException {
        AigcProperties.OssConfig ossConfig = config();
        int retryCount = Math.max(0, ossConfig.getRetryCount());
        long retryIntervalMs = Math.max(0, ossConfig.getRetryIntervalMs());
        RuntimeException lastError = null;

        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                operationCall.run();
                if (attempt > 0) {
                    log.info("AIGC OSS 操作重试成功: operation={}, key={}, attempt={}",
                            operation, objectKey, attempt + 1);
                }
                return;
            } catch (RuntimeException e) {
                lastError = e;
                if (attempt >= retryCount) {
                    break;
                }
                log.warn("AIGC OSS 操作失败，准备重试: operation={}, key={}, attempt={}, maxRetry={}, error={}",
                        operation, objectKey, attempt + 1, retryCount, e.getMessage());
                sleepBeforeRetry(retryIntervalMs, operation, objectKey);
            }
        }

        throw new IOException("OSS " + operation + " 失败: " + objectKey, lastError);
    }

    private void sleepBeforeRetry(long retryIntervalMs, String operation, String objectKey) throws IOException {
        if (retryIntervalMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retryIntervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("OSS " + operation + " 重试等待被中断: " + objectKey, e);
        }
    }

    private void ensureConfigured() throws IOException {
        if (!isConfigured()) {
            throw new IOException("OSS 存储未启用或配置不完整");
        }
    }

    private AigcProperties.OssConfig config() {
        return aigcProperties.getStorage().getOss();
    }

    private String normalizeBaseUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String trimSlashes(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !value.startsWith("<");
    }

    @FunctionalInterface
    private interface StorageOperation {
        void run();
    }
}
