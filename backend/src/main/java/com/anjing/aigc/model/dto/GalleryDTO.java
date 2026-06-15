package com.anjing.aigc.model.dto;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 灵感广场DTO
 *
 * @author AIGC Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryDTO {

    /** 资产ID */
    private String id;

    /** 内容类型 */
    private ContentType contentType;

    /** 资源URL */
    private String url;

    /** 缩略图URL */
    private String thumbnailUrl;

    /** 公开预览 URL */
    private String previewUrl;

    /** 公开访问策略 */
    private String publicAccessMode;

    /** 提示词 */
    private String prompt;

    /** 使用的模型 */
    private String model;

    /** 是否已发布到广场 */
    private Boolean isPublished;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 作者名称 */
    private String authorName;

    /** 点赞数 */
    private Integer likeCount;

    /** 当前用户/会话是否已点赞。V1 前端会话内维护，后端默认 false。 */
    private Boolean likedByCurrentUser;

    /** 收藏数 */
    private Integer favoriteCount;

    /** 当前用户/会话是否已收藏。V1 前端会话内维护，后端默认 false。 */
    private Boolean favoritedByCurrentUser;
}
