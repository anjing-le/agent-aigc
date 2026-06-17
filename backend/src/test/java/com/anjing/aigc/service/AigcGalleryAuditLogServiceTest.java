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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
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

    @Test
    void getInteractionReportAggregatesVisibleAuditMetrics() {
        when(repository.countVisible(nullable(String.class), nullable(String.class), eq(ContentType.IMAGE),
                any(LocalDateTime.class))).thenReturn(12L);
        when(repository.countVisibleSuccessful(nullable(String.class), nullable(String.class), eq(ContentType.IMAGE),
                any(LocalDateTime.class))).thenReturn(10L);
        when(repository.summarizeActions(nullable(String.class), nullable(String.class), eq(ContentType.IMAGE),
                any(LocalDateTime.class))).thenReturn(List.of(
                        actionMetric(AigcGalleryAuditLogService.ACTION_PUBLISH, 2L, 2L),
                        actionMetric(AigcGalleryAuditLogService.ACTION_LIKE, 7L, 6L),
                        actionMetric(AigcGalleryAuditLogService.ACTION_FAVORITE, 3L, 2L),
                        actionMetric(AigcGalleryAuditLogService.ACTION_PUBLIC_DOWNLOAD, 1L, 1L)
                ));
        when(repository.summarizeContentTypes(nullable(String.class), nullable(String.class), eq(ContentType.IMAGE),
                any(LocalDateTime.class))).thenReturn(List.of(contentTypeMetric(ContentType.IMAGE, 12L, 10L)));
        when(repository.summarizeTopAssets(
                nullable(String.class),
                nullable(String.class),
                eq(ContentType.IMAGE),
                any(LocalDateTime.class),
                eq(AigcGalleryAuditLogService.ACTION_LIKE),
                eq(AigcGalleryAuditLogService.ACTION_FAVORITE),
                eq(AigcGalleryAuditLogService.ACTION_PUBLIC_DOWNLOAD),
                any(PageRequest.class)
        )).thenReturn(List.of(assetMetric("asset-1", ContentType.IMAGE, "mock-image-preview", 8L, 5L, 2L, 1L)));

        var report = service.getInteractionReport(7, "image");

        assertEquals(7, report.getDays());
        assertEquals(ContentType.IMAGE, report.getContentType());
        assertEquals(12L, report.getTotalEvents());
        assertEquals(10L, report.getSuccessfulEvents());
        assertEquals(2L, report.getPublishCount());
        assertEquals(6L, report.getLikeCount());
        assertEquals(2L, report.getFavoriteCount());
        assertEquals(1L, report.getDownloadCount());
        assertEquals(4, report.getActionMetrics().size());
        assertEquals(1, report.getContentTypeMetrics().size());
        assertEquals("asset-1", report.getTopAssets().get(0).getAssetId());
    }

    private static AigcGalleryAuditLogRepository.ActionMetricProjection actionMetric(
            String action, Long totalEvents, Long successfulEvents) {
        return new AigcGalleryAuditLogRepository.ActionMetricProjection() {
            @Override
            public String getAction() {
                return action;
            }

            @Override
            public Long getTotalEvents() {
                return totalEvents;
            }

            @Override
            public Long getSuccessfulEvents() {
                return successfulEvents;
            }
        };
    }

    private static AigcGalleryAuditLogRepository.ContentTypeMetricProjection contentTypeMetric(
            ContentType contentType, Long totalEvents, Long successfulEvents) {
        return new AigcGalleryAuditLogRepository.ContentTypeMetricProjection() {
            @Override
            public ContentType getContentType() {
                return contentType;
            }

            @Override
            public Long getTotalEvents() {
                return totalEvents;
            }

            @Override
            public Long getSuccessfulEvents() {
                return successfulEvents;
            }
        };
    }

    private static AigcGalleryAuditLogRepository.AssetMetricProjection assetMetric(
            String assetId,
            ContentType contentType,
            String model,
            Long totalEvents,
            Long likeCount,
            Long favoriteCount,
            Long downloadCount) {
        return new AigcGalleryAuditLogRepository.AssetMetricProjection() {
            @Override
            public String getAssetId() {
                return assetId;
            }

            @Override
            public ContentType getContentType() {
                return contentType;
            }

            @Override
            public String getModel() {
                return model;
            }

            @Override
            public Long getTotalEvents() {
                return totalEvents;
            }

            @Override
            public Long getLikeCount() {
                return likeCount;
            }

            @Override
            public Long getFavoriteCount() {
                return favoriteCount;
            }

            @Override
            public Long getDownloadCount() {
                return downloadCount;
            }
        };
    }
}
