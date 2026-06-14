package com.anjing.aigc.model.entity;

import com.anjing.util.DateUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AIGC Provider 凭证配置。
 *
 * <p>V1 只做运行时页面覆盖和不回显边界；生产级加密/KMS 后续接入。</p>
 */
@Entity
@Table(name = "aigc_provider_credential_config")
@Data
public class AigcProviderCredentialConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Provider key，例如 google */
    @Column(name = "provider_key", unique = true, nullable = false, length = 64)
    private String providerKey;

    /** Provider 展示名称 */
    @Column(name = "provider_name", length = 128)
    private String providerName;

    /** Provider 类型 */
    @Column(name = "provider_type", length = 32)
    private String providerType;

    /** 运行时凭证值：只写入，不通过接口回显 */
    @Column(name = "credential_value", nullable = false, length = 4096)
    private String credentialValue;

    /** 凭证指纹，供审计和调试使用，不可反推出原值 */
    @Column(name = "credential_fingerprint", length = 64)
    private String credentialFingerprint;

    /** 更新人，V1 先记录系统来源 */
    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = DateUtils.nowLocalDateTime();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = DateUtils.nowLocalDateTime();
    }
}
