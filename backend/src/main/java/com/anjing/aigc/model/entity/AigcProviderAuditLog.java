package com.anjing.aigc.model.entity;

import com.anjing.aigc.model.enums.ContentType;
import com.anjing.util.DateUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AIGC Provider 管理审计日志。
 */
@Entity
@Table(name = "aigc_provider_audit_log")
@Data
public class AigcProviderAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 审计动作，例如 active-provider、credential、params */
    @Column(name = "action", nullable = false, length = 64)
    private String action;

    /** 内容类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    /** Provider key，例如 google 或 mock provider 名称 */
    @Column(name = "provider_key", length = 128)
    private String providerKey;

    /** Provider 展示名称 */
    @Column(name = "provider_name", length = 128)
    private String providerName;

    /** Provider 类型 */
    @Column(name = "provider_type", length = 32)
    private String providerType;

    /** 变更前摘要，不包含密钥明文 */
    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "before_summary", columnDefinition = "TEXT")
    private Map<String, Object> beforeSummary = new LinkedHashMap<>();

    /** 变更后摘要，不包含密钥明文 */
    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "after_summary", columnDefinition = "TEXT")
    private Map<String, Object> afterSummary = new LinkedHashMap<>();

    /** 请求 ID */
    @Column(name = "request_id", length = 128)
    private String requestId;

    /** 链路 ID */
    @Column(name = "trace_id", length = 128)
    private String traceId;

    /** 租户 ID */
    @Column(name = "tenant_id", length = 128)
    private String tenantId;

    /** 操作人 ID */
    @Column(name = "operator_id", length = 128)
    private String operatorId;

    /** 操作人名称 */
    @Column(name = "operator_name", length = 128)
    private String operatorName;

    /** 调用方 */
    @Column(name = "caller_id", length = 128)
    private String callerId;

    /** 客户端 IP */
    @Column(name = "client_ip", length = 128)
    private String clientIp;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = DateUtils.nowLocalDateTime();
    }
}
