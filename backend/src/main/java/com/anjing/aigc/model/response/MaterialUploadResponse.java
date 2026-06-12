package com.anjing.aigc.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MaterialUploadResponse {

    private String materialId;

    private String url;

    private String fileName;

    private String originalFileName;

    private String contentType;

    private Long size;

    private String createdAt;
}
