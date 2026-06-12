package com.anjing.aigc.provider.mock;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.provider.AudioGenerationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class MockAudioProvider implements AudioGenerationProvider {

    private final AigcProperties aigcProperties;

    @Override
    public String getProviderName() {
        return "Mock Audio Provider";
    }

    @Override
    public boolean isAvailable() {
        return aigcProperties.getProviders().getMock().isEnabled();
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.OTHER;
    }

    @Override
    public boolean supportsMusicGeneration() {
        return true;
    }

    @Override
    public boolean supportsTTS() {
        return true;
    }

    @Override
    public GenerationResult generate(AigcTask task) {
        String coverUrl = MockAssetFactory.createSvgDataUri(ContentType.AUDIO, task.getOptimizedPrompt());
        return GenerationResult.builder()
                .success(true)
                .taskId(task.getTaskId())
                .contentType(ContentType.AUDIO)
                .url(coverUrl)
                .thumbnailUrl(coverUrl)
                .prompt(task.getOptimizedPrompt())
                .model("mock-audio-preview")
                .metadata(Map.of("duration", 12, "previewOnly", true))
                .build();
    }

    @Override
    public CompletableFuture<GenerationResult> generateAsync(AigcTask task) {
        return CompletableFuture.completedFuture(generate(task));
    }
}
