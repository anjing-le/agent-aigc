package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.enums.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
}

