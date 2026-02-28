package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcTask;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

