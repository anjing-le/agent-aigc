package com.anjing.aigc.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 灵感广场运营规则配置更新请求。
 */
@Data
@Schema(description = "灵感广场运营规则配置更新请求")
public class GalleryCurationRuleUpdateRequest {

    @NotBlank(message = "ruleId 不能为空")
    @Schema(description = "规则 id", example = "trending")
    private String ruleId;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "默认返回数量", example = "4")
    private Integer defaultSize;

    @Schema(description = "最大返回数量", example = "8")
    private Integer maxSize;

    @Schema(description = "页面可见运营建议")
    private String operationHint;
}
