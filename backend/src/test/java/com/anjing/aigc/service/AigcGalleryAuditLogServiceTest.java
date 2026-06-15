package com.anjing.aigc.service;

import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.entity.AigcGalleryAuditLog;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.repository.AigcGalleryAuditLogRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AigcGalleryAuditLogServiceTest {

    private final AigcGalleryAuditLogRepository repository = mock(AigcGalleryAuditLogRepository.class);
    private final AigcOwnershipService ownershipService = new AigcOwnershipService();
    private final AigcGalleryAuditLogService service =
            new AigcGalleryAuditLogService(repository, ownershipService);

    @AfterEach
    void tearDown() {
        GlobalRequestContextHolder.clear();
    }

    @Test
    void recordSuccessCopiesAssetAndGlobalRequestContext() {
        GlobalRequestContextHolder.set(GlobalRequestContext.builder()
                .requestId("rid-1")
                .traceId("tid-1")
                .tenantId("tenant-1")
                .userId("user-1")
                .userName("安静")
                .callerId("frontend")
                .ip("127.0.0.1")
                .build());
        when(repository.save(any(AigcGalleryAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AigcAsset asset = new AigcAsset();
        asset.setAssetId("asset-1");
        asset.setContentType(ContentType.IMAGE);
        asset.setModel("mock-image-preview");
        asset.setPrompt("create a gallery image");

        service.recordSuccess(AigcGalleryAuditLogService.ACTION_PUBLISH, asset);

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(log ->
                AigcGalleryAuditLogService.ACTION_PUBLISH.equals(log.getAction())
                        && "asset-1".equals(log.getAssetId())
                        && ContentType.IMAGE.equals(log.getContentType())
                        && "mock-image-preview".equals(log.getModel())
                        && "create a gallery image".equals(log.getPromptSnapshot())
                        && Boolean.TRUE.equals(log.getSuccess())
                        && "rid-1".equals(log.getRequestId())
                        && "tenant-1".equals(log.getTenantId())
                        && "user-1".equals(log.getOperatorId())
                        && "安静".equals(log.getOperatorName())
        ));
    }

    @Test
    void getAuditLogsReturnsPagedResponse() {
        AigcGalleryAuditLog log = new AigcGalleryAuditLog();
        log.setId(7L);
        log.setAction(AigcGalleryAuditLogService.ACTION_FAVORITE);
        log.setAssetId("asset-7");
        log.setContentType(ContentType.IMAGE);
        log.setModel("mock-image-preview");
        log.setSuccess(true);
        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 10), 1));

        PageResult<?> result = service.getAuditLogs(1, 10,
                AigcGalleryAuditLogService.ACTION_FAVORITE, "asset-7", true);

        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }
}
