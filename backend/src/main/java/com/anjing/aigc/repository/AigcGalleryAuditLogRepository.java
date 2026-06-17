package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcGalleryAuditLog;
import com.anjing.aigc.model.enums.ContentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AigcGalleryAuditLogRepository extends JpaRepository<AigcGalleryAuditLog, Long>,
        JpaSpecificationExecutor<AigcGalleryAuditLog> {

    @Query("""
            select count(log)
            from AigcGalleryAuditLog log
            where (:ownerId is null or log.operatorId is null or log.operatorId = :ownerId)
              and (:tenantId is null or log.tenantId is null or log.tenantId = :tenantId)
              and (:contentType is null or log.contentType = :contentType)
              and (:startAt is null or log.createdAt >= :startAt)
            """)
    long countVisible(
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId,
            @Param("contentType") ContentType contentType,
            @Param("startAt") LocalDateTime startAt);

    @Query("""
            select count(log)
            from AigcGalleryAuditLog log
            where (:ownerId is null or log.operatorId is null or log.operatorId = :ownerId)
              and (:tenantId is null or log.tenantId is null or log.tenantId = :tenantId)
              and (:contentType is null or log.contentType = :contentType)
              and (:startAt is null or log.createdAt >= :startAt)
              and log.success = true
            """)
    long countVisibleSuccessful(
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId,
            @Param("contentType") ContentType contentType,
            @Param("startAt") LocalDateTime startAt);

    @Query("""
            select log.action as action,
                   count(log) as totalEvents,
                   coalesce(sum(case when log.success = true then 1 else 0 end), 0) as successfulEvents
            from AigcGalleryAuditLog log
            where (:ownerId is null or log.operatorId is null or log.operatorId = :ownerId)
              and (:tenantId is null or log.tenantId is null or log.tenantId = :tenantId)
              and (:contentType is null or log.contentType = :contentType)
              and (:startAt is null or log.createdAt >= :startAt)
            group by log.action
            order by count(log) desc
            """)
    List<ActionMetricProjection> summarizeActions(
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId,
            @Param("contentType") ContentType contentType,
            @Param("startAt") LocalDateTime startAt);

    @Query("""
            select log.contentType as contentType,
                   count(log) as totalEvents,
                   coalesce(sum(case when log.success = true then 1 else 0 end), 0) as successfulEvents
            from AigcGalleryAuditLog log
            where (:ownerId is null or log.operatorId is null or log.operatorId = :ownerId)
              and (:tenantId is null or log.tenantId is null or log.tenantId = :tenantId)
              and (:contentType is null or log.contentType = :contentType)
              and (:startAt is null or log.createdAt >= :startAt)
              and log.contentType is not null
            group by log.contentType
            order by count(log) desc
            """)
    List<ContentTypeMetricProjection> summarizeContentTypes(
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId,
            @Param("contentType") ContentType contentType,
            @Param("startAt") LocalDateTime startAt);

    @Query("""
            select log.assetId as assetId,
                   log.contentType as contentType,
                   log.model as model,
                   count(log) as totalEvents,
                   coalesce(sum(case when log.action = :likeAction and log.success = true then 1 else 0 end), 0) as likeCount,
                   coalesce(sum(case when log.action = :favoriteAction and log.success = true then 1 else 0 end), 0) as favoriteCount,
                   coalesce(sum(case when log.action = :downloadAction and log.success = true then 1 else 0 end), 0) as downloadCount
            from AigcGalleryAuditLog log
            where (:ownerId is null or log.operatorId is null or log.operatorId = :ownerId)
              and (:tenantId is null or log.tenantId is null or log.tenantId = :tenantId)
              and (:contentType is null or log.contentType = :contentType)
              and (:startAt is null or log.createdAt >= :startAt)
              and log.assetId is not null
            group by log.assetId, log.contentType, log.model
            order by count(log) desc
            """)
    List<AssetMetricProjection> summarizeTopAssets(
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId,
            @Param("contentType") ContentType contentType,
            @Param("startAt") LocalDateTime startAt,
            @Param("likeAction") String likeAction,
            @Param("favoriteAction") String favoriteAction,
            @Param("downloadAction") String downloadAction,
            Pageable pageable);

    @Query("""
            select log
            from AigcGalleryAuditLog log
            where (:ownerId is null or log.operatorId is null or log.operatorId = :ownerId)
              and (:tenantId is null or log.tenantId is null or log.tenantId = :tenantId)
              and (:contentType is null or log.contentType = :contentType)
              and (:startAt is null or log.createdAt >= :startAt)
            order by log.createdAt asc, log.id asc
            """)
    List<AigcGalleryAuditLog> findVisibleForReport(
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId,
            @Param("contentType") ContentType contentType,
            @Param("startAt") LocalDateTime startAt);

    interface ActionMetricProjection {
        String getAction();

        Long getTotalEvents();

        Long getSuccessfulEvents();
    }

    interface ContentTypeMetricProjection {
        ContentType getContentType();

        Long getTotalEvents();

        Long getSuccessfulEvents();
    }

    interface AssetMetricProjection {
        String getAssetId();

        ContentType getContentType();

        String getModel();

        Long getTotalEvents();

        Long getLikeCount();

        Long getFavoriteCount();

        Long getDownloadCount();
    }
}
