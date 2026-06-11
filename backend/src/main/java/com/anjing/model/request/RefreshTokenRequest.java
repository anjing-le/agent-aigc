package com.anjing.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Refresh token request")
public class RefreshTokenRequest extends BaseRequest {

    @NotBlank(message = "刷新Token不能为空")
    @Schema(description = "Refresh token returned by login", example = "refresh_xxx", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
