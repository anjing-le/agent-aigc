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
@Schema(description = "AIGC gallery dynamic collection")
public class GalleryCollectionResponse {

    @Schema(description = "Stable collection id")
    private String id;

    @Schema(description = "Collection title")
    private String title;

    @Schema(description = "Collection description")
    private String description;

    @Schema(description = "Optional content type scope")
    private ContentType contentType;

    @Schema(description = "Collection strategy, such as trending, latest, or content-type")
    private String strategy;

    @Schema(description = "Collection item count")
    private Integer itemCount;

    @Schema(description = "Total like count in this collection")
    private Long totalLikeCount;

    @Schema(description = "Total favorite count in this collection")
    private Long totalFavoriteCount;

    @Schema(description = "Heat score: likes + favorites * 2")
    private Long heatScore;

    @Schema(description = "Cover asset")
    private GalleryDTO coverAsset;

    @Schema(description = "Collection assets")
    private List<GalleryDTO> assets;
}
