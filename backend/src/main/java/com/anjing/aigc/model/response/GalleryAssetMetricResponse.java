package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 灵感广场作品互动指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryAssetMetricResponse {

    /** 资产 ID */
    private String assetId;

    /** 内容类型 */
    private ContentType contentType;

    /** 生成模型 */
    private String model;

    /** 总互动事件数 */
    private Long totalEvents;

    /** 点赞事件数 */
    private Long likeCount;

    /** 收藏事件数 */
    private Long favoriteCount;

    /** 公开下载事件数 */
    private Long downloadCount;
}
