package com.anjing.aigc.service.storage;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcStorageAuditLog;
import com.anjing.aigc.model.response.StorageAuditLogResponse;
import com.anjing.aigc.repository.AigcStorageAuditLogRepository;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.request.GlobalRequestContext;
import com.anjing.model.response.PageResult;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

    public PageResult<StorageAuditLogResponse> getAuditLogs(Integer current, Integer size,
            String action, String backend, Boolean success) {
        int pageNumber = current != null && current > 0 ? current - 1 : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, 100) : 10;
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AigcStorageAuditLog> page = auditLogRepository.findAll(
                buildAuditSpecification(action, backend, success),
                pageRequest
        );
        return PageResult.of(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getTotalElements(),
                current != null && current > 0 ? current : 1,
                pageSize
        );
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

    private Specification<AigcStorageAuditLog> buildAuditSpecification(String action, String backend,
            Boolean success) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String normalizedAction = normalizeFilter(action);
            String normalizedBackend = normalizeFilter(backend);
            if (normalizedAction != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), normalizedAction));
            }
            if (normalizedBackend != null) {
                predicates.add(criteriaBuilder.equal(root.get("backend"), normalizedBackend.toUpperCase()));
            }
            if (success != null) {
                predicates.add(criteriaBuilder.equal(root.get("success"), success));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private StorageAuditLogResponse toResponse(AigcStorageAuditLog logEntry) {
        return StorageAuditLogResponse.builder()
                .id(logEntry.getId())
                .action(logEntry.getAction())
                .backend(logEntry.getBackend())
                .directory(logEntry.getDirectory())
                .fileName(logEntry.getFileName())
                .url(logEntry.getUrl())
                .sizeBytes(logEntry.getSizeBytes())
                .success(logEntry.getSuccess())
                .errorMessage(logEntry.getErrorMessage())
                .requestId(logEntry.getRequestId())
                .traceId(logEntry.getTraceId())
                .tenantId(logEntry.getTenantId())
                .operatorId(logEntry.getOperatorId())
                .operatorName(logEntry.getOperatorName())
                .callerId(logEntry.getCallerId())
                .clientIp(logEntry.getClientIp())
                .createdAt(logEntry.getCreatedAt() == null ? null : logEntry.getCreatedAt().toString())
                .build();
    }
}
