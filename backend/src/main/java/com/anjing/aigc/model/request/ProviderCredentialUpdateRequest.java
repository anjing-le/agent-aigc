package com.anjing.aigc.model.request;

import com.anjing.aigc.model.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Provider 凭证更新请求。
 */
@Data
@Schema(description = "Provider 凭证更新请求")
public class ProviderCredentialUpdateRequest {

    @NotNull(message = "contentType 不能为空")
    @Schema(description = "内容类型", example = "IMAGE")
    private ContentType contentType;

    @NotBlank(message = "provider 不能为空")
    @Schema(description = "Provider 类型或名称", example = "GOOGLE")
    private String provider;

    @Schema(description = "Provider 展示名称", example = "Google Nano Banana")
    private String providerName;

    @NotBlank(message = "credential 不能为空")
    @Size(max = 4096, message = "credential 长度不能超过 4096")
    @Schema(description = "Provider 凭证，只写入不回显", example = "paste-runtime-credential")
    private String credential;
}
