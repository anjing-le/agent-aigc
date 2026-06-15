package com.anjing.aigc.model.entity;

import com.anjing.util.DateUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AIGC storage operation audit log.
 */
@Entity
@Table(name = "aigc_storage_audit_log")
@Data
public class AigcStorageAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "backend", nullable = false, length = 32)
    private String backend;

    @Column(name = "directory", length = 160)
    private String directory;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

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
