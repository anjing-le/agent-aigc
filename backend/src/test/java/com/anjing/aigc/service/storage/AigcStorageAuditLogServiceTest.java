package com.anjing.aigc.service.storage;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcStorageAuditLog;
import com.anjing.aigc.repository.AigcStorageAuditLogRepository;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.request.GlobalRequestContext;
import com.anjing.model.response.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AigcStorageAuditLogServiceTest {

    private final AigcProperties properties = new AigcProperties();
    private final AigcStorageAuditLogRepository repository = mock(AigcStorageAuditLogRepository.class);
    private final AigcStorageAuditLogService service = new AigcStorageAuditLogService(properties, repository);

    @AfterEach
    void tearDown() {
        GlobalRequestContextHolder.clear();
    }

    @Test
    void recordSuccessCopiesGlobalRequestContext() {
        GlobalRequestContextHolder.set(GlobalRequestContext.builder()
                .requestId("rid-1")
                .traceId("tid-1")
                .tenantId("tenant-1")
                .userId("user-1")
                .userName("安静")
                .callerId("frontend")
                .ip("127.0.0.1")
                .build());
        when(repository.save(any(AigcStorageAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordSuccess(
                AigcStorageAuditLogService.ACTION_UPLOAD,
                "LOCAL",
                "images",
                "asset.png",
                "http://localhost:10003/files/images/asset.png",
                3L
        );

        org.mockito.Mockito.verify(repository).save(org.mockito.ArgumentMatchers.argThat(log ->
                "rid-1".equals(log.getRequestId())
                        && "tid-1".equals(log.getTraceId())
                        && "tenant-1".equals(log.getTenantId())
                        && "user-1".equals(log.getOperatorId())
                        && "安静".equals(log.getOperatorName())
                        && "frontend".equals(log.getCallerId())
                        && "127.0.0.1".equals(log.getClientIp())
                        && Boolean.TRUE.equals(log.getSuccess())
        ));
    }

    @Test
    void getAuditLogsReturnsPagedResponse() {
        AigcStorageAuditLog log = new AigcStorageAuditLog();
        log.setId(7L);
        log.setAction(AigcStorageAuditLogService.ACTION_DELETE_URL);
        log.setBackend("OSS");
        log.setUrl("https://cdn.example.com/aigc/images/asset.png");
        log.setSuccess(true);
        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 10), 1));

        PageResult<?> result = service.getAuditLogs(1, 10,
                AigcStorageAuditLogService.ACTION_DELETE_URL, "oss", true);

        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }
}
