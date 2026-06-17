package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 灵感广场互动报表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryInteractionReportResponse {

    /** 报表窗口天数 */
    private Integer days;

    /** 内容类型筛选 */
    private ContentType contentType;

    /** 统计开始时间 */
    private String startAt;

    /** 报表生成时间 */
    private String generatedAt;

    /** 总事件数 */
    private Long totalEvents;

    /** 成功事件数 */
    private Long successfulEvents;

    /** 成功发布数 */
    private Long publishCount;

    /** 成功撤回数 */
    private Long unpublishCount;

    /** 成功点赞数 */
    private Long likeCount;

    /** 成功取消点赞数 */
    private Long unlikeCount;

    /** 成功收藏数 */
    private Long favoriteCount;

    /** 成功取消收藏数 */
    private Long unfavoriteCount;

    /** 成功公开下载数 */
    private Long downloadCount;

    /** 动作分布 */
    private List<GalleryActionMetricResponse> actionMetrics;

    /** 内容类型分布 */
    private List<GalleryContentTypeMetricResponse> contentTypeMetrics;

    /** 互动最高的作品 */
    private List<GalleryAssetMetricResponse> topAssets;

    /** 每日趋势 */
    private List<GalleryDailyMetricResponse> dailyMetrics;
}
