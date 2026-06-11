package com.anjing.model.response;

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
@Schema(description = "Current authenticated user payload")
public class CurrentUserResponse {

    @Schema(description = "User id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(description = "Login username", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userName;

    @Schema(description = "Display nickname", example = "系统管理员")
    private String nickName;

    @Schema(description = "User email", example = "admin@example.com")
    private String email;

    @Schema(description = "Avatar URL")
    private String avatar;

    @Schema(description = "Role codes", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> roles;

    @Schema(description = "Permission codes")
    private List<String> permissions;

    @Schema(description = "User creation time in ISO-8601 UTC format")
    private String createTime;
}
