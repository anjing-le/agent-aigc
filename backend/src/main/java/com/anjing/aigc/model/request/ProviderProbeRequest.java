package com.anjing.aigc.model.request;

import com.anjing.aigc.model.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Provider 运行前探测请求。
 */
@Data
@Schema(description = "Provider 运行前探测请求")
public class ProviderProbeRequest {

    @NotNull(message = "contentType 不能为空")
    @Schema(description = "内容类型", example = "IMAGE")
    private ContentType contentType;

    @NotBlank(message = "provider 不能为空")
    @Schema(description = "Provider 类型或名称", example = "GOOGLE")
    private String provider;

    @Schema(description = "Provider 展示名称", example = "Google Nano Banana")
    private String providerName;
}
