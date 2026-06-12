package com.anjing.aigc.agent;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.dto.AnalyzedIntent;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.request.GenerateRequest;
import com.anjing.aigc.model.response.AgentAnalysis;
import com.anjing.aigc.provider.ProviderRouter;
import com.anjing.model.errorcode.AigcErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoutingAgentTest {

    private final IntentAnalyzer intentAnalyzer = mock(IntentAnalyzer.class);
    private final PromptEnhancer promptEnhancer = mock(PromptEnhancer.class);
    private final ProviderRouter providerRouter = mock(ProviderRouter.class);
    private final AigcProperties aigcProperties = new AigcProperties();
    private final RoutingAgent routingAgent = new RoutingAgent(
            intentAnalyzer,
            promptEnhancer,
            providerRouter,
            aigcProperties
    );

    @Test
    void analyzeAcceptsSupportedImageParams() {
        aigcProperties.getImage().setActiveProvider("mock");
        when(intentAnalyzer.analyze(eq("生成一张海报"), eq(false))).thenReturn(baseIntent(ContentType.IMAGE));
        when(promptEnhancer.enhance(any(), any(), eq(false))).thenReturn("optimized prompt");

        GenerateRequest request = new GenerateRequest();
        request.setPrompt("生成一张海报");
        request.setContentTypeHint("IMAGE");
        request.setGenerationParams(Map.of(
                "aspectRatio", "1:1",
                "imageSize", "2k",
                "style", "watercolor"
        ));

        AgentAnalysis analysis = routingAgent.analyze(request);

        assertEquals("mock-image-preview", analysis.getSelectedModel());
        assertEquals("1:1", analysis.getAnalyzedIntent().getImageParams().getAspectRatio());
        assertEquals("2K", analysis.getAnalyzedIntent().getImageParams().getImageSize());
        assertEquals("watercolor", analysis.getAnalyzedIntent().getImageParams().getStyle());
    }

    @Test
    void analyzeRejectsUnsupportedContentTypeHint() {
        when(intentAnalyzer.analyze(eq("写一段文案"), eq(false))).thenReturn(baseIntent(ContentType.IMAGE));

        GenerateRequest request = new GenerateRequest();
        request.setPrompt("写一段文案");
        request.setContentTypeHint("TEXT");

        AigcException error = assertThrows(AigcException.class, () -> routingAgent.analyze(request));

        assertEquals(AigcErrorCode.GENERATION_PARAM_INVALID, error.getErrorCode());
    }

    @Test
    void analyzeRejectsUnsupportedAspectRatio() {
        when(intentAnalyzer.analyze(eq("生成一张海报"), eq(false))).thenReturn(baseIntent(ContentType.IMAGE));

        GenerateRequest request = new GenerateRequest();
        request.setPrompt("生成一张海报");
        request.setContentTypeHint("IMAGE");
        request.setGenerationParams(Map.of("aspectRatio", "7:5"));

        AigcException error = assertThrows(AigcException.class, () -> routingAgent.analyze(request));

        assertEquals(AigcErrorCode.GENERATION_PARAM_INVALID, error.getErrorCode());
    }

    @Test
    void analyzeRejectsUnsupportedVideoDuration() {
        when(intentAnalyzer.analyze(eq("生成一段视频"), eq(false))).thenReturn(baseIntent(ContentType.VIDEO));

        GenerateRequest request = new GenerateRequest();
        request.setPrompt("生成一段视频");
        request.setContentTypeHint("VIDEO");
        request.setGenerationParams(Map.of("duration", 5));

        AigcException error = assertThrows(AigcException.class, () -> routingAgent.analyze(request));

        assertEquals(AigcErrorCode.GENERATION_PARAM_INVALID, error.getErrorCode());
    }

    @Test
    void analyzeRejectsNonNumericIntegerParam() {
        when(intentAnalyzer.analyze(eq("生成一段音乐"), eq(false))).thenReturn(baseIntent(ContentType.AUDIO));

        GenerateRequest request = new GenerateRequest();
        request.setPrompt("生成一段音乐");
        request.setContentTypeHint("AUDIO");
        request.setGenerationParams(Map.of("bpm", "fast"));

        AigcException error = assertThrows(AigcException.class, () -> routingAgent.analyze(request));

        assertEquals(AigcErrorCode.GENERATION_PARAM_INVALID, error.getErrorCode());
    }

    private AnalyzedIntent baseIntent(ContentType contentType) {
        return AnalyzedIntent.builder()
                .contentType(contentType)
                .intent("text_to_" + contentType.name().toLowerCase())
                .cleanPrompt("clean prompt")
                .confidence(0.8)
                .build();
    }
}
