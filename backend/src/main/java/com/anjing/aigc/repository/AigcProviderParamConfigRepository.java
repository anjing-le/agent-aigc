package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcProviderParamConfig;
import com.anjing.aigc.model.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AigcProviderParamConfigRepository extends JpaRepository<AigcProviderParamConfig, Long> {

    Optional<AigcProviderParamConfig> findByContentTypeAndProviderKey(ContentType contentType, String providerKey);
}
