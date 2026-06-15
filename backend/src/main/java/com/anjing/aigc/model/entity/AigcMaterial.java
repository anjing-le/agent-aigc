package com.anjing.aigc.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AIGC 参考素材实体。
 */
@Entity
@Table(name = "aigc_material")
@Data
public class AigcMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_id", unique = true, nullable = false, length = 64)
    private String materialId;

    @Column(name = "file_name", nullable = false, length = 160)
    private String fileName;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long size;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "owner_id", length = 128)
    private String ownerId;

    @Column(name = "tenant_id", length = 128)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
