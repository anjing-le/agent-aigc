package com.anjing.aigc.service;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcProviderCredentialConfig;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.repository.AigcProviderCredentialConfigRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AigcProviderCredentialConfigServiceTest {

    private final AigcProperties properties = new AigcProperties();
    private final AigcProviderCredentialConfigRepository repository =
            mock(AigcProviderCredentialConfigRepository.class);
    private final AigcProviderCredentialCodec credentialCodec =
            new AigcProviderCredentialCodec(properties);
    private final AigcProviderCredentialConfigService service =
            new AigcProviderCredentialConfigService(properties, repository, credentialCodec);
    private final ContentProvider googleProvider = new TestProvider(
            "Google Image", ContentProvider.ProviderType.GOOGLE);

    @Test
    void saveGoogleCredentialEncryptsStoredValue() {
        when(repository.findByProviderKey("google")).thenReturn(Optional.empty());
        when(repository.save(any(AigcProviderCredentialConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AigcProviderCredentialConfig saved = service.saveGoogleCredential("runtime-secret-value", googleProvider);

        assertTrue(saved.getCredentialValue().startsWith(AigcProviderCredentialCodec.ENCRYPTED_PREFIX));
        assertFalse(saved.getCredentialValue().contains("runtime-secret-value"));
        assertEquals("google", saved.getProviderKey());
        assertEquals("GOOGLE", saved.getProviderType());
    }

    @Test
    void getGoogleCredentialDecodesEncryptedDatabaseValue() {
        AigcProviderCredentialConfig config = new AigcProviderCredentialConfig();
        config.setProviderKey("google");
        config.setCredentialValue(credentialCodec.encode("runtime-secret-value"));
        when(repository.findByProviderKey("google")).thenReturn(Optional.of(config));

        assertEquals(Optional.of("runtime-secret-value"), service.getGoogleCredential());
        assertEquals("database", service.getGoogleCredentialSource());
        assertEquals("encrypted-database", service.getGoogleCredentialStorageMode());
    }

    @Test
    void getGoogleCredentialKeepsLegacyPlaintextReadable() {
        AigcProviderCredentialConfig config = new AigcProviderCredentialConfig();
        config.setProviderKey("google");
        config.setCredentialValue("legacy-secret-value");
        when(repository.findByProviderKey("google")).thenReturn(Optional.of(config));

        assertEquals(Optional.of("legacy-secret-value"), service.getGoogleCredential());
        assertEquals("legacy-database", service.getGoogleCredentialStorageMode());
    }

    @Test
    void getGoogleCredentialFallsBackToConfiguration() {
        properties.getProviders().getGoogle().setApiKey("configured-secret-value");
        when(repository.findByProviderKey("google")).thenReturn(Optional.empty());

        assertEquals(Optional.of("configured-secret-value"), service.getGoogleCredential());
        assertEquals("configuration", service.getGoogleCredentialSource());
        assertEquals("configuration", service.getGoogleCredentialStorageMode());
    }

    private record TestProvider(String providerName, ProviderType providerType) implements ContentProvider {

        @Override
        public String getProviderName() {
            return providerName;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public ProviderType getProviderType() {
            return providerType;
        }

        @Override
        public GenerationResult generate(AigcTask task) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public CompletableFuture<GenerationResult> generateAsync(AigcTask task) {
            throw new UnsupportedOperationException("not used in this test");
        }
    }
}
