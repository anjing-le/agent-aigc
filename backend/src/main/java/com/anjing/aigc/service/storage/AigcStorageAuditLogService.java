package com.anjing.aigc.service.storage;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcStorageAuditLog;
import com.anjing.aigc.repository.AigcStorageAuditLogRepository;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.request.GlobalRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AigcStorageAuditLogService {

    public static final String ACTION_UPLOAD = "upload";
    public static final String ACTION_DELETE_FILE = "delete-file";
    public static final String ACTION_DELETE_URL = "delete-url";

    private final AigcProperties aigcProperties;
    private final AigcStorageAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String action, String backend, String directory, String fileName,
            String url, Long sizeBytes) {
        record(action, backend, directory, fileName, url, sizeBytes, true, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String action, String backend, String directory, String fileName,
            String url, Long sizeBytes, Exception error) {
        record(action, backend, directory, fileName, url, sizeBytes, false, error);
    }

    private void record(String action, String backend, String directory, String fileName,
            String url, Long sizeBytes, boolean success, Exception error) {
        if (!aigcProperties.getStorage().getOss().isCleanupAuditEnabled()) {
            return;
        }
        try {
            AigcStorageAuditLog logEntry = new AigcStorageAuditLog();
            logEntry.setAction(action);
            logEntry.setBackend(backend);
            logEntry.setDirectory(directory);
            logEntry.setFileName(fileName);
            logEntry.setUrl(url);
            logEntry.setSizeBytes(sizeBytes);
            logEntry.setSuccess(success);
            logEntry.setErrorMessage(error == null ? null : truncate(error.getMessage(), 500));
            applyRequestContext(logEntry);
            auditLogRepository.save(logEntry);
        } catch (RuntimeException auditError) {
            log.warn("AIGC 存储审计写入失败，主流程继续: action={}, backend={}", action, backend, auditError);
        }
    }

    private void applyRequestContext(AigcStorageAuditLog logEntry) {
        GlobalRequestContextHolder.current().ifPresent(context -> applyRequestContext(logEntry, context));
    }

    private void applyRequestContext(AigcStorageAuditLog logEntry, GlobalRequestContext context) {
        logEntry.setRequestId(context.getRequestId());
        logEntry.setTraceId(context.getTraceId());
        logEntry.setTenantId(context.getTenantId());
        logEntry.setOperatorId(context.getUserId());
        logEntry.setOperatorName(context.getUserName());
        logEntry.setCallerId(context.getCallerId());
        logEntry.setClientIp(context.getIp());
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
