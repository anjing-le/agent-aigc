package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.enums.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AIGC资产Repository
 *
 * @author AIGC Team
 */
@Repository
public interface AigcAssetRepository extends JpaRepository<AigcAsset, Long> {

    /**
     * 根据资产ID查询
     */
    Optional<AigcAsset> findByAssetId(String assetId);

    Optional<AigcAsset> findByAssetIdAndIsPublishedTrue(String assetId);

    /**
     * 根据资产ID删除
     */
    @Modifying
    void deleteByAssetId(String assetId);

    /**
     * 查询已发布的资产
     */
    Page<AigcAsset> findByIsPublishedTrue(Pageable pageable);

    /**
     * 根据内容类型查询
     */
    Page<AigcAsset> findByContentType(ContentType contentType, Pageable pageable);

    @Query("""
            select a from AigcAsset a
            where (:ownerId is null or a.ownerId is null or a.ownerId = :ownerId)
              and (:tenantId is null or a.tenantId is null or a.tenantId = :tenantId)
              and (:contentType is null or a.contentType = :contentType)
            """)
    Page<AigcAsset> findVisibleAssets(
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId,
            @Param("contentType") ContentType contentType,
            Pageable pageable);

    @Query("""
            select a from AigcAsset a
            where a.assetId = :assetId
              and (:ownerId is null or a.ownerId is null or a.ownerId = :ownerId)
              and (:tenantId is null or a.tenantId is null or a.tenantId = :tenantId)
            """)
    Optional<AigcAsset> findVisibleByAssetId(
            @Param("assetId") String assetId,
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId);

    @Query("""
            select count(a) from AigcAsset a
            where a.ownerId is null or a.ownerId = ''
               or a.tenantId is null or a.tenantId = ''
            """)
    long countMissingOwnership();

    @Modifying
    @Query("""
            update AigcAsset a
            set a.ownerId = case
                    when a.ownerId is null or a.ownerId = '' then :ownerId
                    else a.ownerId
                end,
                a.tenantId = case
                    when (a.tenantId is null or a.tenantId = '') and :tenantId is not null then :tenantId
                    else a.tenantId
                end
            where a.ownerId is null or a.ownerId = ''
               or a.tenantId is null or a.tenantId = ''
            """)
    int backfillMissingOwnership(@Param("ownerId") String ownerId, @Param("tenantId") String tenantId);

    @Query("""
            select a from AigcAsset a
            where a.isPublished = true
              and (:contentType is null or a.contentType = :contentType)
              and (:model is null or lower(a.model) like lower(concat('%', :model, '%')))
              and (:keyword is null or lower(a.prompt) like lower(concat('%', :keyword, '%')))
            """)
    Page<AigcAsset> searchPublished(
            @Param("contentType") ContentType contentType,
            @Param("model") String model,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(value = """
            select a from AigcAsset a
            where a.isPublished = true
              and (:contentType is null or a.contentType = :contentType)
              and (:model is null or lower(a.model) like lower(concat('%', :model, '%')))
              and (:keyword is null or lower(a.prompt) like lower(concat('%', :keyword, '%')))
            order by (coalesce(a.likeCount, 0) + coalesce(a.favoriteCount, 0) * 2) desc,
                     a.createdAt desc
            """,
            countQuery = """
            select count(a) from AigcAsset a
            where a.isPublished = true
              and (:contentType is null or a.contentType = :contentType)
              and (:model is null or lower(a.model) like lower(concat('%', :model, '%')))
              and (:keyword is null or lower(a.prompt) like lower(concat('%', :keyword, '%')))
            """)
    Page<AigcAsset> searchPublishedRanking(
            @Param("contentType") ContentType contentType,
            @Param("model") String model,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
            select case
                    when a.ownerId is null or a.ownerId = '' then 'anonymous'
                    else a.ownerId
                end as authorId,
                count(a) as publishedCount,
                coalesce(sum(a.likeCount), 0) as totalLikeCount,
                coalesce(sum(a.favoriteCount), 0) as totalFavoriteCount
            from AigcAsset a
            where a.isPublished = true
              and (:contentType is null or a.contentType = :contentType)
              and (:keyword is null or lower(a.prompt) like lower(concat('%', :keyword, '%')))
            group by case
                    when a.ownerId is null or a.ownerId = '' then 'anonymous'
                    else a.ownerId
                end
            order by (coalesce(sum(a.likeCount), 0) + coalesce(sum(a.favoriteCount), 0) * 2) desc,
                     count(a) desc
            """)
    List<PublishedAuthorRankingProjection> rankPublishedAuthors(
            @Param("contentType") ContentType contentType,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
            select a from AigcAsset a
            where a.isPublished = true
              and (
                    (:anonymousOwner = true and (a.ownerId is null or a.ownerId = ''))
                    or (:anonymousOwner = false and a.ownerId = :ownerId)
                  )
              and (:contentType is null or a.contentType = :contentType)
            """)
    Page<AigcAsset> searchPublishedByOwner(
            @Param("ownerId") String ownerId,
            @Param("anonymousOwner") boolean anonymousOwner,
            @Param("contentType") ContentType contentType,
            Pageable pageable);

    @Query("""
            select count(a) from AigcAsset a
            where a.isPublished = true
              and (
                    (:anonymousOwner = true and (a.ownerId is null or a.ownerId = ''))
                    or (:anonymousOwner = false and a.ownerId = :ownerId)
                  )
              and (:contentType is null or a.contentType = :contentType)
            """)
    long countPublishedByOwner(
            @Param("ownerId") String ownerId,
            @Param("anonymousOwner") boolean anonymousOwner,
            @Param("contentType") ContentType contentType);

    @Query("""
            select coalesce(sum(a.likeCount), 0) from AigcAsset a
            where a.isPublished = true
              and (
                    (:anonymousOwner = true and (a.ownerId is null or a.ownerId = ''))
                    or (:anonymousOwner = false and a.ownerId = :ownerId)
                  )
            """)
    long sumPublishedLikeCountByOwner(
            @Param("ownerId") String ownerId,
            @Param("anonymousOwner") boolean anonymousOwner);

    @Query("""
            select coalesce(sum(a.favoriteCount), 0) from AigcAsset a
            where a.isPublished = true
              and (
                    (:anonymousOwner = true and (a.ownerId is null or a.ownerId = ''))
                    or (:anonymousOwner = false and a.ownerId = :ownerId)
                  )
            """)
    long sumPublishedFavoriteCountByOwner(
            @Param("ownerId") String ownerId,
            @Param("anonymousOwner") boolean anonymousOwner);

    interface PublishedAuthorRankingProjection {

        String getAuthorId();

        Long getPublishedCount();

        Long getTotalLikeCount();

        Long getTotalFavoriteCount();
    }
}
