package com.anjing.aigc.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公开分享页转化漏斗。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryShareFunnelResponse {

    /** 分享页访问数 */
    private Long shareViewCount;

    /** 公开下载数 */
    private Long downloadCount;

    /** Prompt 复用数 */
    private Long promptReuseCount;

    /** 公开下载转化率 */
    private Double downloadRate;

    /** Prompt 复用转化率 */
    private Double promptReuseRate;
}
