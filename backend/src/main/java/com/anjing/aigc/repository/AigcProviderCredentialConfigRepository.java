package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcProviderCredentialConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AigcProviderCredentialConfigRepository extends JpaRepository<AigcProviderCredentialConfig, Long> {

    Optional<AigcProviderCredentialConfig> findByProviderKey(String providerKey);
}
