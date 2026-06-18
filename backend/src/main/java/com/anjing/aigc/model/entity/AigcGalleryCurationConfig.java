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
 * 灵感广场运营规则运行时配置。
 */
@Entity
@Table(name = "aigc_gallery_curation_config")
@Data
public class AigcGalleryCurationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 规则 id，例如 trending、course-cover、creator-ranking */
    @Column(name = "rule_id", nullable = false, unique = true, length = 80)
    private String ruleId;

    /** 是否启用 */
    @Column(name = "enabled")
    private Boolean enabled;

    /** 默认返回数量 */
    @Column(name = "default_size")
    private Integer defaultSize;

    /** 最大返回数量 */
    @Column(name = "max_size")
    private Integer maxSize;

    /** 页面可见运营建议 */
    @Column(name = "operation_hint", length = 500)
    private String operationHint;

    /** 更新来源 */
    @Column(name = "updated_by", length = 80)
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
