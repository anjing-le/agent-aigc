package com.anjing.aigc.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 灵感广场每日互动指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryDailyMetricResponse {

    /** 日期，格式 yyyy-MM-dd */
    private String date;

    /** 总事件数 */
    private Long totalEvents;

    /** 成功事件数 */
    private Long successfulEvents;

    /** 成功发布数 */
    private Long publishCount;

    /** 成功点赞数 */
    private Long likeCount;

    /** 成功收藏数 */
    private Long favoriteCount;

    /** 成功公开下载数 */
    private Long downloadCount;
}
