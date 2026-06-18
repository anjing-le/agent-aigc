package com.anjing.aigc.model.response;

import com.anjing.aigc.model.dto.GalleryDTO;
import com.anjing.aigc.model.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AIGC gallery public creator ranking item")
public class GalleryCreatorRankingItemResponse {

    @Schema(description = "Public author id")
    private String authorId;

    @Schema(description = "Public author display name")
    private String authorName;

    @Schema(description = "Published asset count in current ranking scope")
    private Long publishedCount;

    @Schema(description = "Published image count")
    private Long imageCount;

    @Schema(description = "Published video count")
    private Long videoCount;

    @Schema(description = "Published audio count")
    private Long audioCount;

    @Schema(description = "Total like count in current ranking scope")
    private Long totalLikeCount;

    @Schema(description = "Total favorite count in current ranking scope")
    private Long totalFavoriteCount;

    @Schema(description = "Heat score: likes + favorites * 2")
    private Long heatScore;

    @Schema(description = "Creator dominant content type")
    private ContentType dominantContentType;

    @Schema(description = "Representative high-interaction asset")
    private GalleryDTO topAsset;
}
