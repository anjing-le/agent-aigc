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
@Schema(description = "AIGC gallery editorial topics response")
public class GalleryTopicsResponse {

    @Schema(description = "Optional content type filter")
    private ContentType contentType;

    @Schema(description = "Optional keyword filter")
    private String keyword;

    @Schema(description = "Max assets in each topic")
    private Integer topicSize;

    @Schema(description = "Response generation time")
    private String generatedAt;

    @Schema(description = "Editorial topics")
    private List<GalleryTopicResponse> topics;
}
