package com.anjing.aigc.model.dto;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资产DTO
 *
 * @author AIGC Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDTO {

    /** 资产ID */
    private String id;

    /** 内容类型 */
    private ContentType contentType;

    /** 资源URL */
    private String url;

    /** 缩略图URL */
    private String thumbnailUrl;

    /** 提示词 */
    private String prompt;

    /** 使用的模型 */
    private String model;

    /** 是否已发布到广场 */
    private Boolean isPublished;

    /** 创建时间 */
    private LocalDateTime createdAt;
}

