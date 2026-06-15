package com.anjing.aigc.service;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.errorcode.AuthErrorCode;
import com.anjing.model.exception.BizException;
import com.anjing.model.request.GlobalRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AigcProviderManagementPermissionServiceTest {

    private final AigcProperties properties = new AigcProperties();
    private final AigcProviderAuditLogService auditLogService = mock(AigcProviderAuditLogService.class);
    private final AigcProviderManagementPermissionService service =
            new AigcProviderManagementPermissionService(properties, auditLogService);

    @AfterEach
    void tearDown() {
        GlobalRequestContextHolder.clear();
    }

    @Test
    void assertCanManageProviderAllowsAdminRole() {
        GlobalRequestContextHolder.set(context("R_USER,R_ADMIN"));

        service.assertCanManageProvider("credential", ContentType.IMAGE, "GOOGLE");

        verify(auditLogService, never()).recordPermissionDenied(
                eq(ContentType.IMAGE), eq("GOOGLE"), eq("credential"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void assertCanManageProviderRejectsUserRoleAndAuditsDenial() {
        GlobalRequestContextHolder.set(context("R_USER"));

        BizException error = assertThrows(BizException.class,
                () -> service.assertCanManageProvider("params", ContentType.VIDEO, "GOOGLE"));

        assertEquals(AuthErrorCode.PERMISSION_DENIED, error.getErrorCode());
        verify(auditLogService).recordPermissionDenied(
                eq(ContentType.VIDEO),
                eq("GOOGLE"),
                eq("params"),
                argThat(summary -> "params".equals(summary.get("managementAction"))
                        && summary.get("requiredRoles").toString().contains("R_ADMIN")
                        && summary.get("userRoles").toString().contains("R_USER")
                        && "user-1".equals(summary.get("operatorId")))
        );
    }

    @Test
    void assertCanManageProviderRejectsMissingRoles() {
        BizException error = assertThrows(BizException.class,
                () -> service.assertCanManageProvider("active-provider", ContentType.IMAGE, "OTHER"));

        assertEquals(AuthErrorCode.PERMISSION_DENIED, error.getErrorCode());
        verify(auditLogService).recordPermissionDenied(
                eq(ContentType.IMAGE),
                eq("OTHER"),
                eq("active-provider"),
                org.mockito.ArgumentMatchers.<Map<String, Object>>any()
        );
    }

    private GlobalRequestContext context(String roles) {
        return GlobalRequestContext.builder()
                .requestId("request-1")
                .traceId("trace-1")
                .userId("user-1")
                .userName("User One")
                .userRoles(roles)
                .build();
    }
}
