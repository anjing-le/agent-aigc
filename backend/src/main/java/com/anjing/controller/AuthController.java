package com.anjing.controller;

import com.anjing.model.constants.ApiConstants;
import com.anjing.model.errorcode.AuthErrorCode;
import com.anjing.model.exception.BizException;
import com.anjing.model.request.LoginRequest;
import com.anjing.model.request.RefreshTokenRequest;
import com.anjing.model.response.APIResponse;
import com.anjing.model.response.AuthTokenResponse;
import com.anjing.model.response.CurrentUserResponse;
import com.anjing.util.DateUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 认证控制器
 *
 * <p>提供用户登录、登出、用户信息获取等基础认证接口</p>
 * <p>当前为脚手架示例实现（Mock），实际项目中应接入数据库和 JWT</p>
 *
 * @author Backend Template
 * @since 2025-01-20
 */
@Slf4j
@RestController
@RequestMapping(ApiConstants.Auth.BASE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Auth", description = "Authentication and current-user APIs")
public class AuthController {

    /**
     * 用户登录
     *
     * <p>Mock 实现：admin/admin123 登录成功，返回模拟 Token</p>
     * <p>实际项目中应查询数据库、校验密码、生成 JWT Token</p>
     *
     * @param request 登录请求
     * @return 包含 Token 的登录响应
     */
    @PostMapping(ApiConstants.Auth.LOGIN)
    @Operation(summary = "Login")
    public APIResponse<AuthTokenResponse> login(@RequestBody @Validated LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());

        // Mock 校验：仅 admin/admin123 可登录
        if (!"admin".equals(request.getUsername()) || !"admin123".equals(request.getPassword())) {
            throw new BizException(AuthErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // Mock Token 生成
        String token = "token_" + UUID.randomUUID().toString().replace("-", "");
        String refreshToken = "refresh_" + UUID.randomUUID().toString().replace("-", "");

        log.info("用户登录成功: username={}", request.getUsername());
        return APIResponse.success(AuthTokenResponse.bearer(token, refreshToken, 7200), "登录成功");
    }

    /**
     * 获取当前用户信息
     *
     * <p>Mock 实现：返回预设的管理员用户信息</p>
     * <p>实际项目中应从 Token 解析用户 ID，查询数据库获取信息</p>
     *
     * @param authorization 请求头中的 Token
     * @return 用户信息
     */
    @GetMapping(ApiConstants.Auth.ME)
    @Operation(summary = "Get current user")
    public APIResponse<CurrentUserResponse> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        log.info("获取当前用户信息");

        // Mock 用户信息
        CurrentUserResponse userInfo = CurrentUserResponse.builder()
                .userId(1L)
                .userName("admin")
                .nickName("系统管理员")
                .email("admin@example.com")
                .avatar("")
                .roles(List.of("R_SUPER", "R_ADMIN"))
                .permissions(List.of("*:*:*"))
                .createTime(DateUtils.nowIso())
                .build();

        return APIResponse.success(userInfo);
    }

    /**
     * 用户登出
     *
     * <p>实际项目中应清除 Token 缓存、记录登出日志</p>
     *
     * @return 登出结果
     */
    @PostMapping(ApiConstants.Auth.LOGOUT)
    @Operation(summary = "Logout")
    public APIResponse<Void> logout() {
        log.info("用户登出");
        return APIResponse.successMessage("登出成功");
    }

    /**
     * 刷新 Token
     *
     * <p>Mock 实现：返回新的模拟 Token</p>
     *
     * @param request 包含 refreshToken 的请求体
     * @return 新的 Token
     */
    @PostMapping(ApiConstants.Auth.REFRESH)
    @Operation(summary = "Refresh token")
    public APIResponse<AuthTokenResponse> refreshToken(@RequestBody @Validated RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        log.info("刷新 Token 请求");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BizException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        String newToken = "token_" + UUID.randomUUID().toString().replace("-", "");
        String newRefreshToken = "refresh_" + UUID.randomUUID().toString().replace("-", "");

        return APIResponse.success(AuthTokenResponse.bearer(newToken, newRefreshToken, 7200), "Token 刷新成功");
    }
}
