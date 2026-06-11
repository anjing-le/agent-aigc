package com.anjing.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication token payload")
public class AuthTokenResponse {

    @Schema(description = "Access token used in Authorization header", example = "token_xxx", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessToken;

    @Schema(description = "Refresh token used to renew access token", example = "refresh_xxx", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tokenType;

    @Schema(description = "Access token lifetime in seconds", example = "7200", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer expiresIn;

    public static AuthTokenResponse bearer(String accessToken, String refreshToken, int expiresIn) {
        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}
