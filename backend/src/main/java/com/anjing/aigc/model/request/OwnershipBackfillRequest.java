package com.anjing.aigc.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 历史 AIGC 数据归属回填请求。
 */
@Data
@Schema(description = "历史 AIGC 数据归属回填请求")
public class OwnershipBackfillRequest {

    @Schema(description = "是否仅预演；默认 true，不写入数据", defaultValue = "true")
    private Boolean dryRun = true;

    @Schema(description = "当 dryRun=false 时必须显式为 true，防止误写历史数据", defaultValue = "false")
    private Boolean confirmBackfill = false;
}
