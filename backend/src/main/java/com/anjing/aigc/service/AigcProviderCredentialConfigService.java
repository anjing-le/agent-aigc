package com.anjing.aigc.service;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcProviderCredentialConfig;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.repository.AigcProviderCredentialConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Provider 凭证配置来源。
 *
 * <p>优先读取数据库中的页面配置；没有页面配置时回落到环境变量和 yml。</p>
 */
@Service
@RequiredArgsConstructor
public class AigcProviderCredentialConfigService {

    public static final String GOOGLE_PROVIDER_KEY = "google";

    private static final String UPDATED_BY_RUNTIME_PAGE = "runtime-page";

    private final AigcProperties aigcProperties;
    private final AigcProviderCredentialConfigRepository credentialConfigRepository;

    public Optional<String> getGoogleCredential() {
        return getDatabaseCredential(GOOGLE_PROVIDER_KEY)
                .or(() -> normalizeCredential(aigcProperties.getProviders().getGoogle().getApiKey()));
    }

    public boolean isGoogleConfigured() {
        return aigcProperties.getProviders().getGoogle() != null
                && aigcProperties.getProviders().getGoogle().isEnabled()
                && getGoogleCredential().isPresent();
    }

    public String getGoogleCredentialSource() {
        if (getDatabaseCredential(GOOGLE_PROVIDER_KEY).isPresent()) {
            return "database";
        }
        if (normalizeCredential(aigcProperties.getProviders().getGoogle().getApiKey()).isPresent()) {
            return "configuration";
        }
        return "missing";
    }

    public LocalDateTime getGoogleCredentialUpdatedAt() {
        return credentialConfigRepository.findByProviderKey(GOOGLE_PROVIDER_KEY)
                .map(AigcProviderCredentialConfig::getUpdatedAt)
                .orElse(null);
    }

    @Transactional
    public AigcProviderCredentialConfig saveGoogleCredential(String credential, ContentProvider provider) {
        String normalizedCredential = normalizeCredential(credential)
                .orElseThrow(() -> new IllegalArgumentException("credential 不能为空"));
        AigcProviderCredentialConfig config = credentialConfigRepository.findByProviderKey(GOOGLE_PROVIDER_KEY)
                .orElseGet(AigcProviderCredentialConfig::new);
        config.setProviderKey(GOOGLE_PROVIDER_KEY);
        config.setProviderName(provider.getProviderName());
        config.setProviderType(provider.getProviderType().name());
        config.setCredentialValue(normalizedCredential);
        config.setCredentialFingerprint(sha256Hex(normalizedCredential));
        config.setUpdatedBy(UPDATED_BY_RUNTIME_PAGE);
        return credentialConfigRepository.save(config);
    }

    private Optional<String> getDatabaseCredential(String providerKey) {
        return credentialConfigRepository.findByProviderKey(providerKey)
                .flatMap(config -> normalizeCredential(config.getCredentialValue()));
    }

    private Optional<String> normalizeCredential(String value) {
        if (value == null || value.isBlank() || value.startsWith("<")) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
