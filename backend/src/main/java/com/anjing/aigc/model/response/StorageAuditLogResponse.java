package com.anjing.aigc.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * AIGC storage audit log response.
 */
@Data
@Builder
public class StorageAuditLogResponse {

    private Long id;
    private String action;
    private String backend;
    private String directory;
    private String fileName;
    private String url;
    private Long sizeBytes;
    private Boolean success;
    private String errorMessage;
    private String requestId;
    private String traceId;
    private String tenantId;
    private String operatorId;
    private String operatorName;
    private String callerId;
    private String clientIp;
    private String createdAt;
}
