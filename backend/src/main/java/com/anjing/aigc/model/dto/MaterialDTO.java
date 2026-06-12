package com.anjing.aigc.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MaterialDTO {

    private String id;

    private String url;

    private String fileName;

    private String originalFileName;

    private String contentType;

    private Long size;

    private String createdAt;
}
