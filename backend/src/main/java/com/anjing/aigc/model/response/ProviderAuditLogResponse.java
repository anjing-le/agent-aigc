package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Provider 管理审计日志响应。
 */
@Data
@Builder
public class ProviderAuditLogResponse {

    private Long id;
    private String action;
    private ContentType contentType;
    private String providerKey;
    private String providerName;
    private String providerType;
    private Map<String, Object> beforeSummary;
    private Map<String, Object> afterSummary;
    private String requestId;
    private String traceId;
    private String tenantId;
    private String operatorId;
    private String operatorName;
    private String callerId;
    private String clientIp;
    private String createdAt;
}
