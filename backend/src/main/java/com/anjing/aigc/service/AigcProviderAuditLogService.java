package com.anjing.aigc.service;

import com.anjing.aigc.model.entity.AigcProviderAuditLog;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.response.ProviderAuditLogResponse;
import com.anjing.aigc.repository.AigcProviderAuditLogRepository;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.request.GlobalRequestContext;
import com.anjing.model.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provider 管理审计日志。
 */
@Service
@RequiredArgsConstructor
public class AigcProviderAuditLogService {

    public static final String ACTION_ACTIVE_PROVIDER = "active-provider";
    public static final String ACTION_CREDENTIAL = "credential";
    public static final String ACTION_PARAMS = "params";
    public static final String ACTION_SMOKE_TEST = "smoke-test";
    public static final String ACTION_PERMISSION_DENIED = "permission-denied";

    private final AigcProviderAuditLogRepository auditLogRepository;

    @Transactional
    public AigcProviderAuditLog record(String action, ContentType contentType, String providerKey,
            String providerName, String providerType, Map<String, Object> beforeSummary,
            Map<String, Object> afterSummary) {
        AigcProviderAuditLog log = new AigcProviderAuditLog();
        log.setAction(action);
        log.setContentType(contentType);
        log.setProviderKey(providerKey);
        log.setProviderName(providerName);
        log.setProviderType(providerType);
        log.setBeforeSummary(copySummary(beforeSummary));
        log.setAfterSummary(copySummary(afterSummary));
        applyRequestContext(log);
        return auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AigcProviderAuditLog recordPermissionDenied(ContentType contentType, String providerKey,
            String managementAction, Map<String, Object> summary) {
        return record(
                ACTION_PERMISSION_DENIED,
                contentType,
                providerKey,
                providerKey,
                null,
                Map.of(),
                summary == null ? Map.of() : new LinkedHashMap<>(summary)
        );
    }

    public PageResult<ProviderAuditLogResponse> getAuditLogs(Integer current, Integer size,
            ContentType contentType, String action) {
        int pageNumber = current != null && current > 0 ? current - 1 : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, 100) : 10;
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        String normalizedAction = action == null || action.isBlank() ? null : action.trim();
        Page<AigcProviderAuditLog> page = findPage(contentType, normalizedAction, pageRequest);
        return PageResult.of(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getTotalElements(),
                current != null && current > 0 ? current : 1,
                pageSize
        );
    }

    private Page<AigcProviderAuditLog> findPage(ContentType contentType, String action, PageRequest pageRequest) {
        if (contentType != null && action != null) {
            return auditLogRepository.findByContentTypeAndAction(contentType, action, pageRequest);
        }
        if (contentType != null) {
            return auditLogRepository.findByContentType(contentType, pageRequest);
        }
        if (action != null) {
            return auditLogRepository.findByAction(action, pageRequest);
        }
        return auditLogRepository.findAll(pageRequest);
    }

    private void applyRequestContext(AigcProviderAuditLog log) {
        GlobalRequestContextHolder.current().ifPresent(context -> applyRequestContext(log, context));
    }

    private void applyRequestContext(AigcProviderAuditLog log, GlobalRequestContext context) {
        log.setRequestId(context.getRequestId());
        log.setTraceId(context.getTraceId());
        log.setTenantId(context.getTenantId());
        log.setOperatorId(context.getUserId());
        log.setOperatorName(context.getUserName());
        log.setCallerId(context.getCallerId());
        log.setClientIp(context.getIp());
    }

    private Map<String, Object> copySummary(Map<String, Object> summary) {
        return summary == null ? Map.of() : new LinkedHashMap<>(summary);
    }

    private ProviderAuditLogResponse toResponse(AigcProviderAuditLog log) {
        return ProviderAuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .contentType(log.getContentType())
                .providerKey(log.getProviderKey())
                .providerName(log.getProviderName())
                .providerType(log.getProviderType())
                .beforeSummary(copySummary(log.getBeforeSummary()))
                .afterSummary(copySummary(log.getAfterSummary()))
                .requestId(log.getRequestId())
                .traceId(log.getTraceId())
                .tenantId(log.getTenantId())
                .operatorId(log.getOperatorId())
                .operatorName(log.getOperatorName())
                .callerId(log.getCallerId())
                .clientIp(log.getClientIp())
                .createdAt(log.getCreatedAt() == null ? null : log.getCreatedAt().toString())
                .build();
    }
}
