package com.anjing.aigc.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 保存作品到灵感广场请求。
 */
@Data
@Schema(description = "保存作品到灵感广场请求")
public class SaveToGalleryRequest {

    @NotBlank(message = "assetId 不能为空")
    @Schema(description = "资产 ID", example = "d1f2c3a4")
    private String assetId;
}
