package com.anjing.aigc.model.response;

import com.anjing.aigc.model.dto.GalleryDTO;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.model.response.PageResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 灵感广场公开作者主页响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryAuthorProfileResponse {

    /** 公开作者标识 */
    private String authorId;

    /** 公开作者名称 */
    private String authorName;

    /** 公开作品总数 */
    private Long publishedCount;

    /** 公开图片数 */
    private Long imageCount;

    /** 公开视频数 */
    private Long videoCount;

    /** 公开音频数 */
    private Long audioCount;

    /** 公开作品总点赞数 */
    private Long totalLikeCount;

    /** 公开作品总收藏数 */
    private Long totalFavoriteCount;

    /** 公开作品总互动数 */
    private Long totalInteractionCount;

    /** 作者当前最主要的公开创作类型 */
    private ContentType dominantContentType;

    /** 高互动公开作品 */
    private List<GalleryDTO> topAssets;

    /** 公开作品分页 */
    private PageResult<GalleryDTO> assets;
}
