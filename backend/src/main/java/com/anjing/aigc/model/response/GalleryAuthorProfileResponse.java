package com.anjing.aigc.model.response;

import com.anjing.aigc.model.dto.GalleryDTO;
import com.anjing.model.response.PageResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /** 公开作品分页 */
    private PageResult<GalleryDTO> assets;
}
