package com.anjing.aigc.service.impl;

import com.anjing.aigc.agent.RoutingAgent;
import com.anjing.aigc.model.dto.AssetDTO;
import com.anjing.aigc.model.dto.GalleryDTO;
import com.anjing.aigc.model.dto.ModelInfo;
import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.enums.TaskStatus;
import com.anjing.aigc.model.request.GenerateRequest;
import com.anjing.aigc.model.response.AgentAnalysis;
import com.anjing.aigc.model.response.GenerateResponse;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.model.response.ModelListResponse;
import com.anjing.aigc.model.response.TaskStatusResponse;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.provider.ProviderRouter;
import com.anjing.aigc.repository.AigcAssetRepository;
import com.anjing.aigc.repository.AigcTaskRepository;
import com.anjing.aigc.service.AigcService;
import com.anjing.aigc.service.AigcTaskExecutor;
import com.anjing.aigc.exception.AigcException;
import com.anjing.model.response.PageResult;
import com.anjing.util.DateUtils;
import com.anjing.util.IdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AIGC 服务实现类
 * 
 * <p>实现AIGC创作工坊的核心业务逻辑</p>
 *
 * @author AIGC Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AigcServiceImpl implements AigcService {

    private final RoutingAgent routingAgent;
    private final AigcTaskExecutor taskExecutor;
    private final ProviderRouter providerRouter;
    private final AigcTaskRepository taskRepository;
    private final AigcAssetRepository assetRepository;

    @Override
    @Transactional
    public GenerateResponse generate(GenerateRequest request) {
        // 1. 通过Agent分析用户意图
        AgentAnalysis analysis = routingAgent.analyze(request);
        log.info("Agent分析结果: intent={}, contentType={}, model={}", 
                analysis.getIntent(), analysis.getContentType(), analysis.getSelectedModel());

        // 2. 创建任务记录
        AigcTask task = new AigcTask();
        task.setTaskId(IdUtils.uuid());
        task.setPrompt(request.getPrompt());
        task.setOptimizedPrompt(analysis.getOptimizedPrompt());
        task.setReferenceImages(request.getReferenceImages());
        task.setContentType(analysis.getContentType());
        task.setIntent(analysis.getIntent());
        task.setModel(analysis.getSelectedModel());
        task.setStatus(TaskStatus.PENDING);
        task.setProgress(0);
        task.setCreatedAt(DateUtils.nowLocalDateTime());
        task.setUpdatedAt(DateUtils.nowLocalDateTime());
        taskRepository.save(task);

        // 3. 异步执行生成任务
        dispatchGenerationAfterCommit(task.getTaskId());

        // 4. 返回响应
        return GenerateResponse.builder()
                .taskId(task.getTaskId())
                .status(TaskStatus.PENDING)
                .agentAnalysis(analysis)
                .estimatedTime(estimateTime(analysis.getContentType()))
                .build();
    }

    @Override
    public TaskStatusResponse getTaskStatus(String taskId) {
        AigcTask task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new AigcException("TASK_NOT_FOUND", "任务不存在"));

        TaskStatusResponse response = TaskStatusResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .progress(task.getProgress())
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();

        // 如果任务完成，获取生成结果
        if (task.getStatus() == TaskStatus.COMPLETED && task.getAssetId() != null) {
            assetRepository.findByAssetId(task.getAssetId()).ifPresent(asset -> {
                response.setResult(GenerationResult.builder()
                        .assetId(asset.getAssetId())
                        .contentType(asset.getContentType())
                        .url(asset.getUrl())
                        .thumbnailUrl(asset.getThumbnailUrl())
                        .prompt(asset.getPrompt())
                        .model(asset.getModel())
                        .build());
            });
        }

        return response;
    }

    @Override
    public ModelListResponse getAvailableModels() {
        List<ModelInfo> imageModels = providerRouter.getAvailableImageProviders().stream()
                .map(provider -> toModelInfo(provider, ContentType.IMAGE))
                .toList();

        List<ModelInfo> videoModels = providerRouter.getAvailableVideoProviders().stream()
                .map(provider -> toModelInfo(provider, ContentType.VIDEO))
                .toList();

        List<ModelInfo> audioModels = providerRouter.getAvailableAudioProviders().stream()
                .map(provider -> toModelInfo(provider, ContentType.AUDIO))
                .toList();

        return ModelListResponse.builder()
                .imageModels(imageModels)
                .videoModels(videoModels)
                .audioModels(audioModels)
                .build();
    }

    private ModelInfo toModelInfo(ContentProvider provider, ContentType contentType) {
        return ModelInfo.builder()
                .id(toModelId(provider, contentType))
                .name(provider.getProviderName())
                .description(toModelDescription(provider, contentType))
                .contentType(contentType)
                .provider(provider.getProviderType().name())
                .available(provider.isAvailable())
                .icon(contentType.name().toLowerCase())
                .build();
    }

    private String toModelId(ContentProvider provider, ContentType contentType) {
        String normalizedProvider = provider.getProviderName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return contentType.name().toLowerCase() + "-" + normalizedProvider;
    }

    private String toModelDescription(ContentProvider provider, ContentType contentType) {
        if (provider.getProviderType() == ContentProvider.ProviderType.OTHER
                && provider.getProviderName().toLowerCase().contains("mock")) {
            return "本地演示模型，无需外部 API Key";
        }
        return switch (contentType) {
            case IMAGE -> "图片生成 Provider，可用于文生图和参考图创作";
            case VIDEO -> "视频生成 Provider，可用于文生视频和图生视频";
            case AUDIO -> "音频生成 Provider，可用于配音或音乐创作";
            case TEXT -> "文本生成 Provider";
        };
    }

    @Override
    public PageResult<GalleryDTO> getGalleryList(Integer current, Integer size, String contentType, String model, String keyword) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AigcAsset> page = assetRepository.searchPublished(
                parseContentType(contentType),
                normalizeFilter(model),
                normalizeFilter(keyword),
                pageRequest);
        
        List<GalleryDTO> records = page.getContent().stream()
                .map(this::toGalleryDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotalElements(), current, size);
    }

    @Override
    @Transactional
    public void saveToGallery(String assetId) {
        AigcAsset asset = assetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new AigcException("ASSET_NOT_FOUND", "资产不存在"));
        
        asset.setIsPublished(true);
        assetRepository.save(asset);
    }

    @Override
    public PageResult<AssetDTO> getAssetList(Integer current, Integer size, String contentType) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<AigcAsset> page;
        if (normalizeFilter(contentType) != null) {
            page = assetRepository.findByContentType(parseContentType(contentType), pageRequest);
        } else {
            page = assetRepository.findAll(pageRequest);
        }
        
        List<AssetDTO> records = page.getContent().stream()
                .map(this::toAssetDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotalElements(), current, size);
    }

    @Override
    @Transactional
    public void deleteAsset(String assetId) {
        assetRepository.deleteByAssetId(assetId);
    }

    /**
     * 估算生成时间（秒）
     */
    private Integer estimateTime(ContentType contentType) {
        return switch (contentType) {
            case TEXT -> 10;
            case IMAGE -> 30;
            case VIDEO -> 120;
            case AUDIO -> 60;
        };
    }

    private ContentType parseContentType(String contentType) {
        String normalized = normalizeFilter(contentType);
        if (normalized == null) {
            return null;
        }
        try {
            return ContentType.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AigcException("INVALID_CONTENT_TYPE", "不支持的内容类型: " + contentType);
        }
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void dispatchGenerationAfterCommit(String taskId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            taskExecutor.executeGeneration(taskId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                taskExecutor.executeGeneration(taskId);
            }
        });
    }

    /**
     * 转换为GalleryDTO
     */
    private GalleryDTO toGalleryDTO(AigcAsset asset) {
        return GalleryDTO.builder()
                .id(asset.getAssetId())
                .contentType(asset.getContentType())
                .url(asset.getUrl())
                .thumbnailUrl(asset.getThumbnailUrl())
                .prompt(asset.getPrompt())
                .model(asset.getModel())
                .isPublished(asset.getIsPublished())
                .createdAt(asset.getCreatedAt())
                .authorName(null) // TODO: 关联用户
                .likeCount(0) // TODO: 点赞功能
                .build();
    }

    /**
     * 转换为AssetDTO
     */
    private AssetDTO toAssetDTO(AigcAsset asset) {
        return AssetDTO.builder()
                .id(asset.getAssetId())
                .contentType(asset.getContentType())
                .url(asset.getUrl())
                .thumbnailUrl(asset.getThumbnailUrl())
                .prompt(asset.getPrompt())
                .model(asset.getModel())
                .isPublished(asset.getIsPublished())
                .createdAt(asset.getCreatedAt())
                .build();
    }
}
