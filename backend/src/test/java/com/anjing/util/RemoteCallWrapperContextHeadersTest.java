package com.anjing.util;

import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.constants.PlatformContractConstants;
import com.anjing.model.constants.RequestHeaderConstants;
import com.anjing.model.request.GlobalRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteCallWrapperContextHeadersTest {

    @AfterEach
    void tearDown() {
        GlobalRequestContextHolder.clear();
    }

    @Test
    void currentContextHeadersShouldFollowBackendPropagationContract() {
        GlobalRequestContextHolder.set(GlobalRequestContext.builder()
                .requestId("rid-1")
                .traceId("tid-1")
                .tenantId("tenant-a")
                .userId("user-a")
                .userName("Alice")
                .userRoles("admin,user")
                .callerId("gateway")
                .timeZone("Asia/Shanghai")
                .locale("zh-CN")
                .build());

        Map<String, String> headers = RemoteCallWrapper.currentContextHeaders();

        assertEquals(PlatformContractConstants.BACKEND_PROPAGATED_HEADER_KEYS.length, headers.size());
        assertEquals("rid-1", headers.get(RequestHeaderConstants.REQUEST_ID));
        assertEquals("tid-1", headers.get(RequestHeaderConstants.TRACE_ID));
        assertEquals("tenant-a", headers.get(RequestHeaderConstants.TENANT_ID));
        assertEquals("user-a", headers.get(RequestHeaderConstants.USER_ID));
        assertEquals("Alice", headers.get(RequestHeaderConstants.USER_NAME));
        assertEquals("admin,user", headers.get(RequestHeaderConstants.USER_ROLES));
        assertEquals("gateway", headers.get(RequestHeaderConstants.CALLER_ID));
        assertEquals("Asia/Shanghai", headers.get(RequestHeaderConstants.TIME_ZONE));
        assertEquals("zh-CN", headers.get(RequestHeaderConstants.ACCEPT_LANGUAGE));
    }

    @Test
    void serviceCallHeadersShouldCreateTraceHeadersWhenNoRequestContextExists() {
        Map<String, String> headers = RemoteCallWrapper.serviceCallHeaders("infra-dev-scaffolding");

        assertTrue(headers.containsKey(RequestHeaderConstants.REQUEST_ID));
        assertEquals(headers.get(RequestHeaderConstants.REQUEST_ID), headers.get(RequestHeaderConstants.TRACE_ID));
        assertEquals("infra-dev-scaffolding", headers.get(RequestHeaderConstants.CALLER_ID));
        assertFalse(headers.containsKey(RequestHeaderConstants.USER_ID));
    }
}
