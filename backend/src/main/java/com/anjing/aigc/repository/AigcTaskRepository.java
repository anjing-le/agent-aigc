package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * AIGC任务Repository
 *
 * @author AIGC Team
 */
@Repository
public interface AigcTaskRepository extends JpaRepository<AigcTask, Long> {

    /**
     * 根据任务ID查询
     */
    Optional<AigcTask> findByTaskId(String taskId);

    /**
     * 根据资产ID查询来源任务
     */
    Optional<AigcTask> findByAssetId(String assetId);

    @Query("""
            select t from AigcTask t
            where t.taskId = :taskId
              and (:ownerId is null or t.userId is null or t.userId = :ownerId)
              and (:tenantId is null or t.tenantId is null or t.tenantId = :tenantId)
            """)
    Optional<AigcTask> findVisibleByTaskId(
            @Param("taskId") String taskId,
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId);

    @Query("""
            select t from AigcTask t
            where t.assetId = :assetId
              and (:ownerId is null or t.userId is null or t.userId = :ownerId)
              and (:tenantId is null or t.tenantId is null or t.tenantId = :tenantId)
            """)
    Optional<AigcTask> findVisibleByAssetId(
            @Param("assetId") String assetId,
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId);

    /**
     * 按引用素材 ID 反查任务。
     */
    @Query(
            value = """
                    select * from aigc_task
                    where reference_material_ids like :materialIdPattern
                      and (:ownerId is null or user_id is null or user_id = :ownerId)
                      and (:tenantId is null or tenant_id is null or tenant_id = :tenantId)
                    """,
            countQuery = """
                    select count(*) from aigc_task
                    where reference_material_ids like :materialIdPattern
                      and (:ownerId is null or user_id is null or user_id = :ownerId)
                      and (:tenantId is null or tenant_id is null or tenant_id = :tenantId)
                    """,
            nativeQuery = true
    )
    Page<AigcTask> findVisibleByReferenceMaterialId(
            @Param("materialIdPattern") String materialIdPattern,
            @Param("ownerId") String ownerId,
            @Param("tenantId") String tenantId,
            Pageable pageable);
}
