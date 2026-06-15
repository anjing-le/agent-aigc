package com.anjing.aigc.model.entity;

import com.anjing.aigc.model.enums.ContentType;
import com.anjing.util.DateUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AIGC gallery publication and interaction audit log.
 */
@Entity
@Table(name = "aigc_gallery_audit_log")
@Data
public class AigcGalleryAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "asset_id", length = 64)
    private String assetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20)
    private ContentType contentType;

    @Column(name = "model", length = 128)
    private String model;

    @Column(name = "prompt_snapshot", columnDefinition = "TEXT")
    private String promptSnapshot;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Column(name = "tenant_id", length = 128)
    private String tenantId;

    @Column(name = "operator_id", length = 128)
    private String operatorId;

    @Column(name = "operator_name", length = 128)
    private String operatorName;

    @Column(name = "caller_id", length = 128)
    private String callerId;

    @Column(name = "client_ip", length = 128)
    private String clientIp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = DateUtils.nowLocalDateTime();
    }
}
