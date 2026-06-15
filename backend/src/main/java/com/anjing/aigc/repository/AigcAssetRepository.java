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
}
