package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcGalleryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AigcGalleryAuditLogRepository extends JpaRepository<AigcGalleryAuditLog, Long>,
        JpaSpecificationExecutor<AigcGalleryAuditLog> {
}
