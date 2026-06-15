package com.anjing.aigc.service;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.errorcode.AuthErrorCode;
import com.anjing.model.exception.BizException;
import com.anjing.model.request.GlobalRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provider 管理权限边界。
 *
 * <p>V1 复用脚手架请求上下文中的 userRoles，后续可在此处替换为权限中心。</p>
 */
@Service
@RequiredArgsConstructor
public class AigcProviderManagementPermissionService {

    private final AigcProperties aigcProperties;
    private final AigcProviderAuditLogService auditLogService;

    public void assertCanManageProvider(String managementAction, ContentType contentType, String providerKey) {
        GlobalRequestContext context = GlobalRequestContextHolder.current().orElse(null);
        Set<String> userRoles = parseRoles(context == null ? null : context.getUserRoles());
        Set<String> allowedRoles = allowedRoles();
        boolean allowed = userRoles.stream().anyMatch(allowedRoles::contains);
        if (allowed) {
            return;
        }

        auditLogService.recordPermissionDenied(contentType, providerKey, managementAction, auditSummary(
                "managementAction", managementAction,
                "requiredRoles", allowedRoles,
                "userRoles", userRoles,
                "operatorId", context == null ? null : context.getUserId(),
                "operatorName", context == null ? null : context.getUserName()
        ));
        throw new BizException(AuthErrorCode.PERMISSION_DENIED);
    }

    private Set<String> allowedRoles() {
        return aigcProperties.getProviderManagement().getAdminRoles().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> parseRoles(String roles) {
        if (!StringUtils.hasText(roles)) {
            return Set.of();
        }
        return Arrays.stream(roles.split("[,;\\s]+"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, Object> auditSummary(Object... values) {
        Map<String, Object> summary = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            Object value = values[index + 1];
            if (value != null) {
                summary.put(String.valueOf(values[index]), value);
            }
        }
        return summary;
    }
}
