package com.anjing.aigc.service;

import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.entity.AigcGalleryAuditLog;
import com.anjing.aigc.model.response.GalleryAuditLogResponse;
import com.anjing.aigc.repository.AigcGalleryAuditLogRepository;
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

/**
 * AIGC gallery publication and interaction audit logs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AigcGalleryAuditLogService {

    public static final String ACTION_PUBLISH = "publish";
    public static final String ACTION_UNPUBLISH = "unpublish";
    public static final String ACTION_LIKE = "like";
    public static final String ACTION_UNLIKE = "unlike";
    public static final String ACTION_FAVORITE = "favorite";
    public static final String ACTION_UNFAVORITE = "unfavorite";
    public static final String ACTION_PUBLIC_DOWNLOAD = "public-download";

    private final AigcGalleryAuditLogRepository auditLogRepository;
    private final AigcOwnershipService ownershipService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String action, AigcAsset asset) {
        record(action, asset, true, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String action, AigcAsset asset, Exception error) {
        record(action, asset, false, error == null ? null : error.getMessage());
    }

    public PageResult<GalleryAuditLogResponse> getAuditLogs(Integer current, Integer size,
            String action, String assetId, Boolean success) {
        int pageNumber = current != null && current > 0 ? current - 1 : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, 100) : 10;
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AigcGalleryAuditLog> page = auditLogRepository.findAll(
                buildAuditSpecification(action, assetId, success),
                pageRequest
        );
        return PageResult.of(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getTotalElements(),
                current != null && current > 0 ? current : 1,
                pageSize
        );
    }

    private void record(String action, AigcAsset asset, boolean success, String message) {
        try {
            AigcGalleryAuditLog logEntry = new AigcGalleryAuditLog();
            logEntry.setAction(action);
            if (asset != null) {
                logEntry.setAssetId(asset.getAssetId());
                logEntry.setContentType(asset.getContentType());
                logEntry.setModel(asset.getModel());
                logEntry.setPromptSnapshot(asset.getPrompt());
            }
            logEntry.setSuccess(success);
            logEntry.setMessage(truncate(message, 500));
            applyRequestContext(logEntry);
            auditLogRepository.save(logEntry);
        } catch (RuntimeException auditError) {
            String assetId = asset == null ? null : asset.getAssetId();
            log.warn("AIGC 广场审计写入失败，主流程继续: action={}, assetId={}", action, assetId, auditError);
        }
    }

    private Specification<AigcGalleryAuditLog> buildAuditSpecification(String action, String assetId,
            Boolean success) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String normalizedAction = normalizeFilter(action);
            String normalizedAssetId = normalizeFilter(assetId);
            if (normalizedAction != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), normalizedAction));
            }
            if (normalizedAssetId != null) {
                predicates.add(criteriaBuilder.equal(root.get("assetId"), normalizedAssetId));
            }
            if (success != null) {
                predicates.add(criteriaBuilder.equal(root.get("success"), success));
            }
            String ownerId = ownershipService.currentOwnerId();
            if (ownerId != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("operatorId")),
                        criteriaBuilder.equal(root.get("operatorId"), ownerId)
                ));
            }
            String tenantId = ownershipService.currentTenantId();
            if (tenantId != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("tenantId")),
                        criteriaBuilder.equal(root.get("tenantId"), tenantId)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void applyRequestContext(AigcGalleryAuditLog logEntry) {
        GlobalRequestContextHolder.current().ifPresent(context -> applyRequestContext(logEntry, context));
    }

    private void applyRequestContext(AigcGalleryAuditLog logEntry, GlobalRequestContext context) {
        logEntry.setRequestId(context.getRequestId());
        logEntry.setTraceId(context.getTraceId());
        logEntry.setTenantId(context.getTenantId());
        logEntry.setOperatorId(context.getUserId());
        logEntry.setOperatorName(context.getUserName());
        logEntry.setCallerId(context.getCallerId());
        logEntry.setClientIp(context.getIp());
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private GalleryAuditLogResponse toResponse(AigcGalleryAuditLog logEntry) {
        return GalleryAuditLogResponse.builder()
                .id(logEntry.getId())
                .action(logEntry.getAction())
                .assetId(logEntry.getAssetId())
                .contentType(logEntry.getContentType())
                .model(logEntry.getModel())
                .promptSnapshot(logEntry.getPromptSnapshot())
                .success(logEntry.getSuccess())
                .message(logEntry.getMessage())
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
