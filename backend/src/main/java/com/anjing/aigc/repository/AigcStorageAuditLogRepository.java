package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcStorageAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AigcStorageAuditLogRepository extends JpaRepository<AigcStorageAuditLog, Long> {
}
