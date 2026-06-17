package com.anjing.aigc.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 灵感广场互动动作指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryActionMetricResponse {

    /** 互动动作 */
    private String action;

    /** 总事件数 */
    private Long totalEvents;

    /** 成功事件数 */
    private Long successfulEvents;
}
