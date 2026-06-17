package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 灵感广场内容类型互动指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryContentTypeMetricResponse {

    /** 内容类型 */
    private ContentType contentType;

    /** 总事件数 */
    private Long totalEvents;

    /** 成功事件数 */
    private Long successfulEvents;
}
