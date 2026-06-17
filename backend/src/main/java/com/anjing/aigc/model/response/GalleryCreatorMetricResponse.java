package com.anjing.aigc.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 灵感广场创作者互动指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryCreatorMetricResponse {

    /** 作者公开标识 */
    private String authorId;

    /** 作者公开名称 */
    private String authorName;

    /** 参与统计的作品数 */
    private Long assetCount;

    /** 总事件数 */
    private Long totalEvents;

    /** 成功事件数 */
    private Long successfulEvents;

    /** 点赞事件数 */
    private Long likeCount;

    /** 收藏事件数 */
    private Long favoriteCount;

    /** 公开下载事件数 */
    private Long downloadCount;
}
