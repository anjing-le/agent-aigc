package com.anjing.aigc.model.response;

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
@Schema(description = "AIGC gallery curation rules")
public class GalleryCurationRulesResponse {

    @Schema(description = "Rule version")
    private String version;

    @Schema(description = "Generated timestamp")
    private String generatedAt;

    @Schema(description = "Default collection or topic size")
    private Integer defaultCollectionSize;

    @Schema(description = "Maximum collection or topic size")
    private Integer maxCollectionSize;

    @Schema(description = "Default creator ranking size")
    private Integer defaultCreatorRankingSize;

    @Schema(description = "Maximum creator ranking size")
    private Integer maxCreatorRankingSize;

    @Schema(description = "Curation rules")
    private List<GalleryCurationRuleResponse> rules;
}
