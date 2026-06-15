package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AigcMaterialRepository extends JpaRepository<AigcMaterial, Long> {

    Optional<AigcMaterial> findByMaterialId(String materialId);

    List<AigcMaterial> findByMaterialIdIn(Collection<String> materialIds);

    void deleteByMaterialId(String materialId);

    Page<AigcMaterial> findByContentTypeStartingWith(String contentTypePrefix, Pageable pageable);

    @Query("""
            select m from AigcMaterial m
            where (:ownerId is null or m.ownerId is null or m.ownerId = :ownerId)
              and (:tenantId is null or m.tenantId is null or m.tenantId = :tenantId)
              and (:contentTypePrefix is null or lower(m.contentType) like lower(concat(:contentTypePrefix, '%')))
            """)
    Page<AigcMaterial> findVisibleMaterials(
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId,
            @Param("contentTypePrefix") String contentTypePrefix,
            Pageable pageable);

    @Query("""
            select m from AigcMaterial m
            where m.materialId = :materialId
              and (:ownerId is null or m.ownerId is null or m.ownerId = :ownerId)
              and (:tenantId is null or m.tenantId is null or m.tenantId = :tenantId)
            """)
    Optional<AigcMaterial> findVisibleByMaterialId(
            @Param("materialId") String materialId,
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId);

    @Query("""
            select m from AigcMaterial m
            where m.materialId in :materialIds
              and (:ownerId is null or m.ownerId is null or m.ownerId = :ownerId)
              and (:tenantId is null or m.tenantId is null or m.tenantId = :tenantId)
            """)
    List<AigcMaterial> findVisibleByMaterialIdIn(
            @Param("materialIds") Collection<String> materialIds,
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId);

    @Query("""
            select count(m) from AigcMaterial m
            where m.ownerId is null or m.ownerId = ''
               or m.tenantId is null or m.tenantId = ''
            """)
    long countMissingOwnership();

    @Modifying
    @Query("""
            update AigcMaterial m
            set m.ownerId = case
                    when m.ownerId is null or m.ownerId = '' then :ownerId
                    else m.ownerId
                end,
                m.tenantId = case
                    when (m.tenantId is null or m.tenantId = '') and :tenantId is not null then :tenantId
                    else m.tenantId
                end
            where m.ownerId is null or m.ownerId = ''
               or m.tenantId is null or m.tenantId = ''
            """)
    int backfillMissingOwnership(@Param("ownerId") String ownerId, @Param("tenantId") String tenantId);
}
