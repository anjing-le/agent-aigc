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
@Schema(description = "AIGC gallery public creator ranking")
public class GalleryCreatorRankingResponse {

    @Schema(description = "Optional content type scope")
    private ContentType contentType;

    @Schema(description = "Optional keyword scope")
    private String keyword;

    @Schema(description = "Ranking size")
    private Integer size;

    @Schema(description = "Generated timestamp")
    private String generatedAt;

    @Schema(description = "Ranked public creators")
    private List<GalleryCreatorRankingItemResponse> creators;
}
