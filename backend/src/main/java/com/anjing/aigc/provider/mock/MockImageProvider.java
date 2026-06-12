package com.anjing.aigc.provider.mock;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.provider.ImageGenerationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class MockImageProvider implements ImageGenerationProvider {

    private final AigcProperties aigcProperties;

    @Override
    public String getProviderName() {
        return "Mock Image Provider";
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
    public boolean supportsImageToImage() {
        return true;
    }

    @Override
    public boolean supportsImageEditing() {
        return true;
    }

    @Override
    public GenerationResult generate(AigcTask task) {
        String url = MockAssetFactory.createSvgDataUri(ContentType.IMAGE, task.getOptimizedPrompt());
        return GenerationResult.builder()
                .success(true)
                .taskId(task.getTaskId())
                .contentType(ContentType.IMAGE)
                .url(url)
                .thumbnailUrl(url)
                .prompt(task.getOptimizedPrompt())
                .model("mock-image-preview")
                .metadata(Map.of("width", 1280, "height", 720, "format", "svg"))
                .build();
    }

    @Override
    public CompletableFuture<GenerationResult> generateAsync(AigcTask task) {
        return CompletableFuture.completedFuture(generate(task));
    }
}
