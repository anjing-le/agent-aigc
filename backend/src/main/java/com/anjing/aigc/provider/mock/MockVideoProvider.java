package com.anjing.aigc.provider.mock;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.provider.VideoGenerationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class MockVideoProvider implements VideoGenerationProvider {

    private final AigcProperties aigcProperties;

    @Override
    public String getProviderName() {
        return "Mock Video Provider";
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
    public boolean supportsImageToVideo() {
        return true;
    }

    @Override
    public boolean supportsAudioGeneration() {
        return true;
    }

    @Override
    public GenerationResult generate(AigcTask task) {
        String thumbnailUrl = MockAssetFactory.createSvgDataUri(ContentType.VIDEO, task.getOptimizedPrompt());
        return GenerationResult.builder()
                .success(true)
                .taskId(task.getTaskId())
                .contentType(ContentType.VIDEO)
                .url(thumbnailUrl)
                .thumbnailUrl(thumbnailUrl)
                .prompt(task.getOptimizedPrompt())
                .model("mock-video-preview")
                .metadata(Map.of("duration", 8, "resolution", "720p", "previewOnly", true))
                .build();
    }

    @Override
    public CompletableFuture<GenerationResult> generateAsync(AigcTask task) {
        return CompletableFuture.completedFuture(generate(task));
    }
}
