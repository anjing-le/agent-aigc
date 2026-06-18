package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AIGC gallery curation rule")
public class GalleryCurationRuleResponse {

    @Schema(description = "Stable rule id")
    private String id;

    @Schema(description = "Rule type, such as collection, topic, asset-ranking, or creator-ranking")
    private String ruleType;

    @Schema(description = "Rule title")
    private String title;

    @Schema(description = "Rule description")
    private String description;

    @Schema(description = "Operation scenario")
    private String scenario;

    @Schema(description = "Optional content type scope")
    private ContentType contentType;

    @Schema(description = "Collection or ranking strategy")
    private String strategy;

    @Schema(description = "Machine-readable curation rule")
    private String curationRule;

    @Schema(description = "Prompt tokens used by manual topics")
    private List<String> promptTokens;

    @Schema(description = "Default returned item size")
    private Integer defaultSize;

    @Schema(description = "Maximum returned item size")
    private Integer maxSize;

    @Schema(description = "Suggested operation action")
    private String operationHint;

    @Schema(description = "Whether this rule is enabled")
    private Boolean enabled;
}
