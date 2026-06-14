package com.anjing.aigc.service;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.entity.AigcProviderParamConfig;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.repository.AigcProviderParamConfigRepository;
import com.anjing.model.errorcode.AigcErrorCode;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AigcProviderParamConfigServiceTest {

    private final AigcProperties properties = new AigcProperties();
    private final AigcProviderParamConfigRepository repository = mock(AigcProviderParamConfigRepository.class);
    private final AigcProviderParamConfigService service =
            new AigcProviderParamConfigService(properties, repository);
    private final ContentProvider googleProvider = new TestProvider(
            "Google Image", ContentProvider.ProviderType.GOOGLE);

    @Test
    void getGoogleDefaultParamsFallsBackToConfiguration() {
        when(repository.findByContentTypeAndProviderKey(ContentType.IMAGE, "google"))
                .thenReturn(Optional.empty());

        Map<String, Object> params = service.getDefaultParams(googleProvider, ContentType.IMAGE);

        assertEquals("16:9", params.get("aspectRatio"));
        assertEquals("1K", params.get("imageSize"));
        assertEquals("configuration", service.getParamConfigSource(googleProvider, ContentType.IMAGE));
    }

    @Test
    void saveGoogleDefaultParamsNormalizesAndPersistsAllowedKeys() {
        when(repository.findByContentTypeAndProviderKey(ContentType.VIDEO, "google"))
                .thenReturn(Optional.empty());
        when(repository.save(any(AigcProviderParamConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("aspectRatio", "9:16");
        input.put("resolution", "1080p");
        input.put("duration", "6");
        input.put("timeoutMs", 120000);

        AigcProviderParamConfig saved = service.saveGoogleDefaultParams(ContentType.VIDEO, input, googleProvider);

        assertEquals(ContentType.VIDEO, saved.getContentType());
        assertEquals("google", saved.getProviderKey());
        assertEquals("Google Image", saved.getProviderName());
        assertEquals("GOOGLE", saved.getProviderType());
        assertEquals("runtime-page", saved.getUpdatedBy());
        assertEquals("9:16", saved.getDefaultParams().get("aspectRatio"));
        assertEquals("1080p", saved.getDefaultParams().get("resolution"));
        assertEquals(6, saved.getDefaultParams().get("duration"));
        assertEquals(120000, saved.getDefaultParams().get("timeoutMs"));
    }

    @Test
    void saveGoogleDefaultParamsRejectsUnsupportedOption() {
        when(repository.findByContentTypeAndProviderKey(ContentType.IMAGE, "google"))
                .thenReturn(Optional.empty());

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("aspectRatio", "10:1");
        input.put("imageSize", "1K");
        input.put("timeoutMs", 60000);

        AigcException exception = assertThrows(AigcException.class,
                () -> service.saveGoogleDefaultParams(ContentType.IMAGE, input, googleProvider));

        assertEquals(AigcErrorCode.GENERATION_PARAM_INVALID, exception.getErrorCode());
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
