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
@Schema(description = "AIGC gallery dynamic collections response")
public class GalleryCollectionsResponse {

    @Schema(description = "Optional content type filter")
    private ContentType contentType;

    @Schema(description = "Optional keyword filter")
    private String keyword;

    @Schema(description = "Max assets in each collection")
    private Integer collectionSize;

    @Schema(description = "Response generation time")
    private String generatedAt;

    @Schema(description = "Dynamic collections")
    private List<GalleryCollectionResponse> collections;
}
