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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AIGC Provider 路由配置。
 */
@Entity
@Table(name = "aigc_provider_route_config")
@Data
public class AigcProviderRouteConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 内容类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", unique = true, nullable = false, length = 20)
    private ContentType contentType;

    /** 当前激活 Provider key */
    @Column(name = "active_provider", nullable = false, length = 128)
    private String activeProvider;

    /** Provider 展示名称 */
    @Column(name = "provider_name", length = 128)
    private String providerName;

    /** Provider 类型 */
    @Column(name = "provider_type", length = 32)
    private String providerType;

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
