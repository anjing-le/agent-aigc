package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 灵感广场作品对比指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryAssetComparisonResponse {

    /** 资产 ID */
    private String assetId;

    /** 内容类型 */
    private ContentType contentType;

    /** 生成模型 */
    private String model;

    /** 总事件数 */
    private Long totalEvents;

    /** 互动事件数，包含点赞、收藏和公开下载 */
    private Long engagementEvents;

    /** 点赞事件数 */
    private Long likeCount;

    /** 收藏事件数 */
    private Long favoriteCount;

    /** 公开下载事件数 */
    private Long downloadCount;

    /** 占当前报表总事件比例，0-100 */
    private Double eventShareRate;

    /** 收藏在互动事件中的比例，0-100 */
    private Double favoriteRate;

    /** 下载在互动事件中的比例，0-100 */
    private Double downloadRate;
}
