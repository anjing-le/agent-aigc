package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import lombok.Builder;
import lombok.Data;

/**
 * AIGC gallery audit log response.
 */
@Data
@Builder
public class GalleryAuditLogResponse {

    private Long id;
    private String action;
    private String assetId;
    private ContentType contentType;
    private String model;
    private String promptSnapshot;
    private Boolean success;
    private String message;
    private String requestId;
    private String traceId;
    private String tenantId;
    private String operatorId;
    private String operatorName;
    private String callerId;
    private String clientIp;
    private String createdAt;
}
