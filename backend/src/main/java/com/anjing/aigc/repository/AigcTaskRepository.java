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

    /**
     * 按引用素材 ID 反查任务。
     */
    @Query(
            value = "select * from aigc_task where reference_material_ids like :materialIdPattern",
            countQuery = "select count(*) from aigc_task where reference_material_ids like :materialIdPattern",
            nativeQuery = true
    )
    Page<AigcTask> findByReferenceMaterialId(@Param("materialIdPattern") String materialIdPattern, Pageable pageable);
}
