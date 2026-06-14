package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcProviderRouteConfig;
import com.anjing.aigc.model.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AigcProviderRouteConfigRepository extends JpaRepository<AigcProviderRouteConfig, Long> {

    Optional<AigcProviderRouteConfig> findByContentType(ContentType contentType);
}
