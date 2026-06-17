package com.anjing.aigc.model.response;

import com.anjing.aigc.model.dto.GalleryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公开分享页响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryShareResponse {

    /** 已发布的广场作品 */
    private GalleryDTO asset;

    /** 前端公开分享路径 */
    private String sharePath;

    /** 公开预览 API */
    private String previewUrl;

    /** 公开下载 API */
    private String downloadUrl;

    /** 分享页 SEO 标题 */
    private String seoTitle;

    /** 分享页 SEO 描述 */
    private String seoDescription;

    /** 分享页 SEO 关键词 */
    private String seoKeywords;

    /** 海报标题 */
    private String posterTitle;

    /** 海报副标题 */
    private String posterSubtitle;

    /** 海报页脚 */
    private String posterFooter;
}
