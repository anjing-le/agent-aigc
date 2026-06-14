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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AIGC Provider 默认参数模板配置。
 */
@Entity
@Table(
        name = "aigc_provider_param_config",
        uniqueConstraints = @UniqueConstraint(columnNames = {"content_type", "provider_key"})
)
@Data
public class AigcProviderParamConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 内容类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    /** Provider key，例如 google */
    @Column(name = "provider_key", nullable = false, length = 64)
    private String providerKey;

    /** Provider 展示名称 */
    @Column(name = "provider_name", length = 128)
    private String providerName;

    /** Provider 类型 */
    @Column(name = "provider_type", length = 32)
    private String providerType;

    /** 默认参数模板 */
    @Convert(converter = MapStringObjectConverter.class)
    @Column(name = "default_params", columnDefinition = "TEXT")
    private Map<String, Object> defaultParams = new LinkedHashMap<>();

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
