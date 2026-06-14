package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcProviderAuditLog;
import com.anjing.aigc.model.enums.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AigcProviderAuditLogRepository extends JpaRepository<AigcProviderAuditLog, Long> {

    Page<AigcProviderAuditLog> findByContentType(ContentType contentType, Pageable pageable);

    Page<AigcProviderAuditLog> findByAction(String action, Pageable pageable);

    Page<AigcProviderAuditLog> findByContentTypeAndAction(ContentType contentType, String action, Pageable pageable);
}
