package com.anjing.aigc.service;

import com.anjing.aigc.model.entity.AigcProviderAuditLog;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.repository.AigcProviderAuditLogRepository;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.request.GlobalRequestContext;
import com.anjing.model.response.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AigcProviderAuditLogServiceTest {

    private final AigcProviderAuditLogRepository repository = mock(AigcProviderAuditLogRepository.class);
    private final AigcProviderAuditLogService service = new AigcProviderAuditLogService(repository);

    @AfterEach
    void tearDown() {
        GlobalRequestContextHolder.clear();
    }

    @Test
    void recordCopiesGlobalRequestContext() {
        GlobalRequestContextHolder.set(GlobalRequestContext.builder()
                .requestId("rid-1")
                .traceId("tid-1")
                .tenantId("tenant-1")
                .userId("user-1")
                .userName("安静")
                .callerId("frontend")
                .ip("127.0.0.1")
                .build());
        when(repository.save(any(AigcProviderAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AigcProviderAuditLog log = service.record(
                AigcProviderAuditLogService.ACTION_ACTIVE_PROVIDER,
                ContentType.IMAGE,
                "google",
                "Google Nano Banana",
                "GOOGLE",
                Map.of("activeProvider", "mock"),
                Map.of("activeProvider", "google")
        );

        assertEquals("rid-1", log.getRequestId());
        assertEquals("tid-1", log.getTraceId());
        assertEquals("tenant-1", log.getTenantId());
        assertEquals("user-1", log.getOperatorId());
        assertEquals("安静", log.getOperatorName());
        assertEquals("frontend", log.getCallerId());
        assertEquals("127.0.0.1", log.getClientIp());
        assertEquals("mock", log.getBeforeSummary().get("activeProvider"));
        assertEquals("google", log.getAfterSummary().get("activeProvider"));
    }

    @Test
    void getAuditLogsReturnsPagedResponse() {
        AigcProviderAuditLog log = new AigcProviderAuditLog();
        log.setId(7L);
        log.setAction(AigcProviderAuditLogService.ACTION_PARAMS);
        log.setContentType(ContentType.VIDEO);
        log.setProviderKey("google");
        log.setProviderName("Google Veo");
        log.setProviderType("GOOGLE");
        log.setBeforeSummary(Map.of("duration", 8));
        log.setAfterSummary(Map.of("duration", 6));
        when(repository.findByContentTypeAndAction(any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 10), 1));

        PageResult<?> result = service.getAuditLogs(1, 10, ContentType.VIDEO,
                AigcProviderAuditLogService.ACTION_PARAMS);

        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }
}
