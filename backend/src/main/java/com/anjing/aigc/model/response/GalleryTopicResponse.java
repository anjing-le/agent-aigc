package com.anjing.aigc.model.response;

import com.anjing.aigc.model.dto.GalleryDTO;
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
@Schema(description = "AIGC gallery editorial topic")
public class GalleryTopicResponse {

    @Schema(description = "Stable topic id")
    private String id;

    @Schema(description = "Topic title")
    private String title;

    @Schema(description = "Topic description")
    private String description;

    @Schema(description = "Topic operation scenario")
    private String scenario;

    @Schema(description = "Editorial curation rule")
    private String curationRule;

    @Schema(description = "Optional content type scope")
    private ContentType contentType;

    @Schema(description = "Topic item count")
    private Integer itemCount;

    @Schema(description = "Total like count in this topic")
    private Long totalLikeCount;

    @Schema(description = "Total favorite count in this topic")
    private Long totalFavoriteCount;

    @Schema(description = "Heat score: likes + favorites * 2")
    private Long heatScore;

    @Schema(description = "Suggested operation action")
    private String operationHint;

    @Schema(description = "Topic cover asset")
    private GalleryDTO coverAsset;

    @Schema(description = "Topic assets")
    private List<GalleryDTO> assets;
}
