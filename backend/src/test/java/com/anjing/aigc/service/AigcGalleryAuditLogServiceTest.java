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
                any(LocalDateTime.class))).thenReturn(18L);
        when(repository.countVisibleSuccessful(nullable(String.class), nullable(String.class), eq(ContentType.IMAGE),
                any(LocalDateTime.class))).thenReturn(16L);
        when(repository.summarizeActions(nullable(String.class), nullable(String.class), eq(ContentType.IMAGE),
                any(LocalDateTime.class))).thenReturn(List.of(
                        actionMetric(AigcGalleryAuditLogService.ACTION_PUBLISH, 2L, 2L),
                        actionMetric(AigcGalleryAuditLogService.ACTION_LIKE, 7L, 6L),
                        actionMetric(AigcGalleryAuditLogService.ACTION_FAVORITE, 3L, 2L),
                        actionMetric(AigcGalleryAuditLogService.ACTION_PUBLIC_DOWNLOAD, 1L, 1L),
                        actionMetric(AigcGalleryAuditLogService.ACTION_SHARE_VIEW, 4L, 4L),
                        actionMetric(AigcGalleryAuditLogService.ACTION_PROMPT_REUSE, 2L, 2L)
                ));
        when(repository.summarizeContentTypes(nullable(String.class), nullable(String.class), eq(ContentType.IMAGE),
                any(LocalDateTime.class))).thenReturn(List.of(contentTypeMetric(ContentType.IMAGE, 18L, 16L)));
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
        when(repository.summarizeCreators(
                nullable(String.class),
                nullable(String.class),
                eq(ContentType.IMAGE),
                any(LocalDateTime.class),
                eq(AigcGalleryAuditLogService.ACTION_LIKE),
                eq(AigcGalleryAuditLogService.ACTION_FAVORITE),
                eq(AigcGalleryAuditLogService.ACTION_PUBLIC_DOWNLOAD),
                any(PageRequest.class)
        )).thenReturn(List.of(creatorMetric("creator-1", 2L, 8L, 7L, 5L, 2L, 1L)));
        when(repository.findVisibleForReport(nullable(String.class), nullable(String.class), eq(ContentType.IMAGE),
                any(LocalDateTime.class))).thenReturn(List.of(
                        reportLog(AigcGalleryAuditLogService.ACTION_LIKE, true, LocalDateTime.now().minusDays(1)),
                        reportLog(AigcGalleryAuditLogService.ACTION_PUBLIC_DOWNLOAD, true, LocalDateTime.now()),
                        reportLog(AigcGalleryAuditLogService.ACTION_SHARE_VIEW, true, LocalDateTime.now()),
                        reportLog(AigcGalleryAuditLogService.ACTION_PROMPT_REUSE, true, LocalDateTime.now())
                ));

        var report = service.getInteractionReport(7, "image");

        assertEquals(7, report.getDays());
        assertEquals(ContentType.IMAGE, report.getContentType());
        assertEquals(18L, report.getTotalEvents());
        assertEquals(16L, report.getSuccessfulEvents());
        assertEquals(2L, report.getPublishCount());
        assertEquals(6L, report.getLikeCount());
        assertEquals(2L, report.getFavoriteCount());
        assertEquals(1L, report.getDownloadCount());
        assertEquals(4L, report.getShareViewCount());
        assertEquals(2L, report.getPromptReuseCount());
        assertEquals(4L, report.getShareFunnel().getShareViewCount());
        assertEquals(1L, report.getShareFunnel().getDownloadCount());
        assertEquals(2L, report.getShareFunnel().getPromptReuseCount());
        assertEquals(25D, report.getShareFunnel().getDownloadRate());
        assertEquals(50D, report.getShareFunnel().getPromptReuseRate());
        assertEquals(6, report.getActionMetrics().size());
        assertEquals(1, report.getContentTypeMetrics().size());
        assertEquals("asset-1", report.getTopAssets().get(0).getAssetId());
        assertEquals("creator-1", report.getCreatorMetrics().get(0).getAuthorId());
        assertEquals(2L, report.getCreatorMetrics().get(0).getAssetCount());
        assertEquals("asset-1", report.getAssetComparisons().get(0).getAssetId());
        assertEquals(8L, report.getAssetComparisons().get(0).getEngagementEvents());
        assertEquals(44.44D, report.getAssetComparisons().get(0).getEventShareRate());
        assertEquals(25D, report.getAssetComparisons().get(0).getFavoriteRate());
        assertEquals(12.5D, report.getAssetComparisons().get(0).getDownloadRate());
        assertEquals(7, report.getDailyMetrics().size());
        assertEquals(4L, report.getDailyMetrics().stream().mapToLong(metric -> metric.getTotalEvents()).sum());
        assertEquals(1L, report.getDailyMetrics().stream().mapToLong(metric -> metric.getLikeCount()).sum());
        assertEquals(1L, report.getDailyMetrics().stream().mapToLong(metric -> metric.getDownloadCount()).sum());
        assertEquals(1L, report.getDailyMetrics().stream().mapToLong(metric -> metric.getShareViewCount()).sum());
        assertEquals(1L, report.getDailyMetrics().stream().mapToLong(metric -> metric.getPromptReuseCount()).sum());
    }

    private static AigcGalleryAuditLog reportLog(String action, boolean success, LocalDateTime createdAt) {
        AigcGalleryAuditLog logEntry = new AigcGalleryAuditLog();
        logEntry.setAction(action);
        logEntry.setSuccess(success);
        logEntry.setCreatedAt(createdAt);
        return logEntry;
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

    private static AigcGalleryAuditLogRepository.CreatorMetricProjection creatorMetric(
            String authorId,
            Long assetCount,
            Long totalEvents,
            Long successfulEvents,
            Long likeCount,
            Long favoriteCount,
            Long downloadCount) {
        return new AigcGalleryAuditLogRepository.CreatorMetricProjection() {
            @Override
            public String getAuthorId() {
                return authorId;
            }

            @Override
            public Long getAssetCount() {
                return assetCount;
            }

            @Override
            public Long getTotalEvents() {
                return totalEvents;
            }

            @Override
            public Long getSuccessfulEvents() {
                return successfulEvents;
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
