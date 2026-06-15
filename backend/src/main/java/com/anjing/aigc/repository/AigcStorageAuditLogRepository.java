package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcStorageAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AigcStorageAuditLogRepository extends JpaRepository<AigcStorageAuditLog, Long>,
        JpaSpecificationExecutor<AigcStorageAuditLog> {
}
