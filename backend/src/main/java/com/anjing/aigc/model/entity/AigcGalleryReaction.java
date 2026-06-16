package com.anjing.aigc.model.entity;

import com.anjing.util.DateUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AIGC gallery user/session reaction relation.
 */
@Entity
@Table(
        name = "aigc_gallery_reaction",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_aigc_gallery_reaction_actor",
                        columnNames = {"asset_id", "reaction_type", "actor_id", "tenant_key"}
                )
        }
)
@Data
public class AigcGalleryReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false, length = 64)
    private String assetId;

    @Column(name = "reaction_type", nullable = false, length = 32)
    private String reactionType;

    @Column(name = "actor_id", nullable = false, length = 160)
    private String actorId;

    @Column(name = "actor_name", length = 128)
    private String actorName;

    @Column(name = "tenant_id", length = 128)
    private String tenantId;

    @Column(name = "tenant_key", nullable = false, length = 160)
    private String tenantKey;

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
