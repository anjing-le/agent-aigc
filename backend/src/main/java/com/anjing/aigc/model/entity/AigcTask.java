package com.anjing.aigc.model.entity;

import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AIGC任务实体
 *
 * @author AIGC Team
 */
@Entity
@Table(name = "aigc_task")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AigcTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务ID（业务ID） */
    @Column(name = "task_id", unique = true, nullable = false, length = 64)
    private String taskId;

    /** 用户ID */
    @Column(name = "user_id", length = 64)
    private String userId;

    /** 原始提示词 */
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    /** 优化后的提示词 */
    @Column(name = "optimized_prompt", columnDefinition = "TEXT")
    private String optimizedPrompt;

    /** 参考图片URL列表（JSON格式存储） */
    @Column(name = "reference_images", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> referenceImages;

    /** 内容类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20)
    private ContentType contentType;

    /** 识别的意图 */
    @Column(name = "intent", length = 50)
    private String intent;

    /** 使用的模型 */
    @Column(name = "model", length = 64)
    private String model;

    /** 任务状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private TaskStatus status;

    /** 进度百分比 0-100 */
    @Column(name = "progress")
    @Builder.Default
    private Integer progress = 0;

    /** 生成的资产ID */
    @Column(name = "asset_id", length = 64)
    private String assetId;

    /** 生成的资源URL */
    @Column(name = "result_url", length = 500)
    private String resultUrl;

    /** 缩略图URL */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /** 错误信息 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 耗时（毫秒） */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (progress == null) {
            progress = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
