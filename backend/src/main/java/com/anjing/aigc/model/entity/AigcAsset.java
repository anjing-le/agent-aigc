package com.anjing.aigc.model.entity;

import com.anjing.aigc.model.enums.ContentType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AIGC资产实体
 *
 * @author AIGC Team
 */
@Entity
@Table(name = "aigc_asset")
@Data
public class AigcAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 资产ID（业务ID） */
    @Column(name = "asset_id", unique = true, nullable = false, length = 64)
    private String assetId;

    /** 内容类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20)
    private ContentType contentType;

    /** 资源URL */
    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    /** 缩略图URL */
    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    /** 提示词 */
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    /** 使用的模型 */
    @Column(name = "model", length = 64)
    private String model;

    /** 归属用户 ID，空值兼容历史演示数据 */
    @Column(name = "owner_id", length = 128)
    private String ownerId;

    /** 租户 ID，空值兼容历史演示数据 */
    @Column(name = "tenant_id", length = 128)
    private String tenantId;

    /** 是否已发布到广场 */
    @Column(name = "is_published")
    private Boolean isPublished;

    /** 灵感广场点赞数 */
    @Column(name = "like_count")
    private Integer likeCount;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (likeCount == null) {
            likeCount = 0;
        }
    }
}
