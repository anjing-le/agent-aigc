package com.anjing.aigc.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 历史 AIGC 数据归属回填结果。
 */
@Data
@Builder
@Schema(description = "历史 AIGC 数据归属回填结果")
public class OwnershipBackfillResponse {

    @Schema(description = "是否仅预演")
    private Boolean dryRun;

    @Schema(description = "是否已显式确认写入")
    private Boolean confirmed;

    @Schema(description = "回填使用的用户 ID")
    private String ownerId;

    @Schema(description = "回填使用的租户 ID")
    private String tenantId;

    @Schema(description = "待回填资产数量")
    private Long assetCandidates;

    @Schema(description = "待回填参考素材数量")
    private Long materialCandidates;

    @Schema(description = "待回填任务数量")
    private Long taskCandidates;

    @Schema(description = "已回填资产数量")
    private Integer assetUpdated;

    @Schema(description = "已回填参考素材数量")
    private Integer materialUpdated;

    @Schema(description = "已回填任务数量")
    private Integer taskUpdated;

    @Schema(description = "执行提示")
    private String message;

    @Schema(description = "检查时间")
    private String checkedAt;
}
