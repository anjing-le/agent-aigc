package com.anjing.aigc.service.impl;

import com.anjing.aigc.agent.RoutingAgent;
import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.dto.AssetDTO;
import com.anjing.aigc.model.dto.GalleryDTO;
import com.anjing.aigc.model.dto.MaterialDTO;
import com.anjing.aigc.model.dto.ModelInfo;
import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.entity.AigcMaterial;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.enums.TaskStatus;
import com.anjing.aigc.model.request.GenerateRequest;
import com.anjing.aigc.model.request.ProviderProbeRequest;
import com.anjing.aigc.model.request.ProviderRouteUpdateRequest;
import com.anjing.aigc.model.response.AgentAnalysis;
import com.anjing.aigc.model.response.AssetDetailResponse;
import com.anjing.aigc.model.response.GenerateResponse;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.model.response.ModelListResponse;
import com.anjing.aigc.model.response.ProviderExecutionSummary;
import com.anjing.aigc.model.response.ProviderProbeResponse;
import com.anjing.aigc.model.response.ProviderRouteUpdateResponse;
import com.anjing.aigc.model.response.TaskStatusResponse;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.provider.ProviderRouter;
import com.anjing.aigc.repository.AigcAssetRepository;
import com.anjing.aigc.repository.AigcMaterialRepository;
import com.anjing.aigc.repository.AigcTaskRepository;
import com.anjing.aigc.service.AigcReferenceMaterialPolicy;
import com.anjing.aigc.service.AigcService;
import com.anjing.aigc.service.AigcTaskExecutor;
import com.anjing.aigc.service.storage.LocalAigcStorageService;
import com.anjing.aigc.exception.AigcException;
import com.anjing.model.errorcode.AigcErrorCode;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final AigcProperties aigcProperties;
    private final AigcTaskRepository taskRepository;
    private final AigcAssetRepository assetRepository;
    private final AigcMaterialRepository materialRepository;
    private final AigcReferenceMaterialPolicy referenceMaterialPolicy;
    private final LocalAigcStorageService localAigcStorageService;

    @Override
    @Transactional
    public GenerateResponse generate(GenerateRequest request) {
        // 1. 通过Agent分析用户意图
        AgentAnalysis analysis = routingAgent.analyze(request);
        log.info("Agent分析结果: intent={}, contentType={}, model={}", 
                analysis.getIntent(), analysis.getContentType(), analysis.getSelectedModel());

        List<AigcMaterial> referenceMaterials = validateReferenceMaterials(request, analysis.getContentType());

        // 2. 创建任务记录
        AigcTask task = new AigcTask();
        task.setTaskId(IdUtils.uuid());
        task.setPrompt(request.getPrompt());
        task.setOptimizedPrompt(analysis.getOptimizedPrompt());
        task.setReferenceImages(resolveReferenceImages(request, referenceMaterials));
        task.setReferenceMaterialIds(resolveReferenceMaterialIds(request, referenceMaterials));
        task.setContentType(analysis.getContentType());
        task.setIntent(analysis.getIntent());
        task.setModel(analysis.getSelectedModel());
        task.setAgentAnalysis(analysis);
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
                .orElseThrow(() -> new AigcException(AigcErrorCode.TASK_NOT_FOUND));

        return toTaskStatusResponse(task);
    }

    @Override
    @Transactional
    public GenerateResponse retryTask(String taskId) {
        AigcTask sourceTask = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.TASK_NOT_FOUND));

        GenerateRequest request = new GenerateRequest();
        request.setPrompt(sourceTask.getPrompt());
        request.setContentTypeHint(sourceTask.getContentType() == null ? null : sourceTask.getContentType().name());
        request.setReferenceImages(sourceTask.getReferenceImages());
        request.setReferenceMaterialIds(sourceTask.getReferenceMaterialIds());
        return generate(request);
    }

    @Override
    public PageResult<TaskStatusResponse> getTasksByMaterial(String materialId, Integer current, Integer size) {
        materialRepository.findByMaterialId(materialId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.MATERIAL_NOT_FOUND));

        int pageNumber = current != null && current > 0 ? current - 1 : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, 100) : 20;
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AigcTask> page = taskRepository.findByReferenceMaterialId(toMaterialIdPattern(materialId), pageRequest);

        return PageResult.of(
                page.getContent().stream().map(this::toTaskStatusResponse).toList(),
                page.getTotalElements(),
                current != null && current > 0 ? current : 1,
                pageSize
        );
    }

    private TaskStatusResponse toTaskStatusResponse(AigcTask task) {
        TaskStatusResponse response = TaskStatusResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .progress(task.getProgress())
                .agentAnalysis(resolveAgentAnalysis(task))
                .providerExecution(resolveProviderExecution(task))
                .referenceMaterialIds(task.getReferenceMaterialIds())
                .referenceMaterials(getReferenceMaterials(task.getReferenceMaterialIds()))
                .errorMessage(task.getErrorMessage())
                .errorCode(task.getErrorCode())
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
        List<ModelInfo> imageModels = providerRouter.getImageProviders().stream()
                .map(provider -> toModelInfo(provider, ContentType.IMAGE))
                .toList();

        List<ModelInfo> videoModels = providerRouter.getVideoProviders().stream()
                .map(provider -> toModelInfo(provider, ContentType.VIDEO))
                .toList();

        List<ModelInfo> audioModels = providerRouter.getAudioProviders().stream()
                .map(provider -> toModelInfo(provider, ContentType.AUDIO))
                .toList();

        return ModelListResponse.builder()
                .imageModels(imageModels)
                .videoModels(videoModels)
                .audioModels(audioModels)
                .build();
    }

    private ModelInfo toModelInfo(ContentProvider provider, ContentType contentType) {
        boolean active = isActiveProvider(provider, getActiveProvider(contentType));
        return ModelInfo.builder()
                .id(toModelId(provider, contentType))
                .name(provider.getProviderName())
                .description(toModelDescription(provider, contentType))
                .contentType(contentType)
                .provider(provider.getProviderType().name())
                .activeProvider(getActiveProvider(contentType))
                .active(active)
                .available(provider.isAvailable())
                .configuredModel(resolveConfiguredModel(provider, contentType))
                .defaultParams(resolveDefaultParams(provider, contentType))
                .missingConfig(resolveMissingConfig(provider))
                .statusReason(resolveModelStatusReason(provider, active))
                .icon(contentType.name().toLowerCase())
                .build();
    }

    @Override
    public ProviderProbeResponse probeProvider(ProviderProbeRequest request) {
        ContentType contentType = request.getContentType();
        String activeProvider = getActiveProvider(contentType);
        ContentProvider provider = findProviderForProbe(contentType, request.getProvider(), request.getProviderName());

        if (provider == null) {
            return ProviderProbeResponse.builder()
                    .contentType(contentType)
                    .requestedProvider(request.getProvider())
                    .activeProvider(activeProvider)
                    .registered(false)
                    .active(false)
                    .available(false)
                    .routable(false)
                    .configurationComplete(false)
                    .defaultParams(Map.of())
                    .missingConfig("未找到已注册 Provider")
                    .statusReason("Provider 未注册到 Spring 容器")
                    .message("探测失败：Provider 未注册")
                    .checkedAt(DateUtils.nowIso())
                    .build();
        }

        boolean active = isActiveProvider(provider, activeProvider);
        boolean available = provider.isAvailable();
        String missingConfig = resolveMissingConfig(provider);
        boolean configurationComplete = missingConfig == null;
        boolean routable = active && available && configurationComplete;

        return ProviderProbeResponse.builder()
                .contentType(contentType)
                .requestedProvider(request.getProvider())
                .providerName(provider.getProviderName())
                .providerType(provider.getProviderType().name())
                .activeProvider(activeProvider)
                .registered(true)
                .active(active)
                .available(available)
                .routable(routable)
                .configurationComplete(configurationComplete)
                .configuredModel(resolveConfiguredModel(provider, contentType))
                .defaultParams(resolveDefaultParams(provider, contentType))
                .missingConfig(missingConfig)
                .statusReason(resolveModelStatusReason(provider, active))
                .message(resolveProbeMessage(routable, active, available, configurationComplete))
                .checkedAt(DateUtils.nowIso())
                .build();
    }

    @Override
    public ProviderRouteUpdateResponse updateActiveProvider(ProviderRouteUpdateRequest request) {
        ContentType contentType = request.getContentType();
        ContentProvider provider = findProviderForProbe(contentType, request.getProvider(), request.getProviderName());
        if (provider == null) {
            throw new AigcException(AigcErrorCode.PROVIDER_UNAVAILABLE, "Provider 未注册，无法切换路由");
        }

        String activeProvider = resolveActiveProviderKey(provider);
        setActiveProvider(contentType, activeProvider);

        boolean available = provider.isAvailable();
        String missingConfig = resolveMissingConfig(provider);
        boolean configurationComplete = missingConfig == null;
        boolean routable = available && configurationComplete;

        return ProviderRouteUpdateResponse.builder()
                .contentType(contentType)
                .activeProvider(activeProvider)
                .providerName(provider.getProviderName())
                .providerType(provider.getProviderType().name())
                .available(available)
                .routable(routable)
                .configurationComplete(configurationComplete)
                .configuredModel(resolveConfiguredModel(provider, contentType))
                .defaultParams(resolveDefaultParams(provider, contentType))
                .missingConfig(missingConfig)
                .statusReason(resolveModelStatusReason(provider, true))
                .message(resolveRouteUpdateMessage(routable, configurationComplete, available))
                .updatedAt(DateUtils.nowIso())
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

    private String getActiveProvider(ContentType contentType) {
        return switch (contentType) {
            case IMAGE -> aigcProperties.getImage().getActiveProvider();
            case VIDEO -> aigcProperties.getVideo().getActiveProvider();
            case AUDIO -> aigcProperties.getAudio().getActiveProvider();
            case TEXT -> "";
        };
    }

    private void setActiveProvider(ContentType contentType, String activeProvider) {
        switch (contentType) {
            case IMAGE -> aigcProperties.getImage().setActiveProvider(activeProvider);
            case VIDEO -> aigcProperties.getVideo().setActiveProvider(activeProvider);
            case AUDIO -> aigcProperties.getAudio().setActiveProvider(activeProvider);
            case TEXT -> throw new AigcException(AigcErrorCode.CONTENT_TYPE_UNSUPPORTED, "文本生成暂未开放 Provider");
        }
    }

    private String resolveActiveProviderKey(ContentProvider provider) {
        return switch (provider.getProviderType()) {
            case GOOGLE -> "google";
            case OPENAI -> "openai";
            case STABILITY -> "stability";
            case OTHER -> provider.getProviderName();
        };
    }

    private ContentProvider findProviderForProbe(ContentType contentType, String providerKey, String providerName) {
        return getProviders(contentType).stream()
                .filter(provider -> matchesProbeProvider(provider, providerKey, providerName))
                .findFirst()
                .orElse(null);
    }

    private List<? extends ContentProvider> getProviders(ContentType contentType) {
        return switch (contentType) {
            case IMAGE -> providerRouter.getImageProviders();
            case VIDEO -> providerRouter.getVideoProviders();
            case AUDIO -> providerRouter.getAudioProviders();
            case TEXT -> List.of();
        };
    }

    private boolean matchesProbeProvider(ContentProvider provider, String providerKey, String providerName) {
        if (providerName != null && !providerName.isBlank()
                && provider.getProviderName().equalsIgnoreCase(providerName.trim())) {
            return true;
        }
        if (providerKey == null || providerKey.isBlank()) {
            return false;
        }
        String normalizedKey = providerKey.trim();
        return provider.getProviderType().name().equalsIgnoreCase(normalizedKey)
                || provider.getProviderName().equalsIgnoreCase(normalizedKey)
                || provider.getProviderName().toLowerCase().contains(normalizedKey.toLowerCase());
    }

    private boolean isActiveProvider(ContentProvider provider, String activeProvider) {
        if (activeProvider == null || activeProvider.isBlank()) {
            return provider.isAvailable();
        }
        return switch (activeProvider.toLowerCase()) {
            case "google" -> provider.getProviderType() == ContentProvider.ProviderType.GOOGLE;
            case "openai" -> provider.getProviderType() == ContentProvider.ProviderType.OPENAI;
            case "stability" -> provider.getProviderType() == ContentProvider.ProviderType.STABILITY;
            default -> provider.getProviderName().toLowerCase().contains(activeProvider.toLowerCase());
        };
    }

    private String resolveConfiguredModel(ContentProvider provider, ContentType contentType) {
        if (provider.getProviderType() == ContentProvider.ProviderType.OTHER
                && provider.getProviderName().toLowerCase().contains("mock")) {
            return switch (contentType) {
                case IMAGE -> "mock-image-preview";
                case VIDEO -> "mock-video-preview";
                case AUDIO -> "mock-audio-preview";
                case TEXT -> "mock-text-preview";
            };
        }
        if (provider.getProviderType() != ContentProvider.ProviderType.GOOGLE) {
            return null;
        }
        return switch (contentType) {
            case IMAGE -> aigcProperties.getImage().getGoogle().getModel();
            case VIDEO -> aigcProperties.getVideo().getGoogle().getModel();
            case AUDIO -> aigcProperties.getAudio().getGoogle().getModel();
            case TEXT -> null;
        };
    }

    private Map<String, Object> resolveDefaultParams(ContentProvider provider, ContentType contentType) {
        if (provider.getProviderType() == ContentProvider.ProviderType.OTHER
                && provider.getProviderName().toLowerCase().contains("mock")) {
            return Map.of("mode", "local-demo", "externalKey", false);
        }
        if (provider.getProviderType() != ContentProvider.ProviderType.GOOGLE) {
            return Map.of();
        }
        return switch (contentType) {
            case IMAGE -> Map.of(
                    "aspectRatio", aigcProperties.getImage().getGoogle().getDefaultAspectRatio(),
                    "imageSize", aigcProperties.getImage().getGoogle().getDefaultImageSize(),
                    "timeoutMs", aigcProperties.getImage().getGoogle().getTimeout()
            );
            case VIDEO -> Map.of(
                    "aspectRatio", aigcProperties.getVideo().getGoogle().getDefaultAspectRatio(),
                    "resolution", aigcProperties.getVideo().getGoogle().getDefaultResolution(),
                    "duration", aigcProperties.getVideo().getGoogle().getDefaultDuration(),
                    "timeoutMs", aigcProperties.getVideo().getGoogle().getTimeout()
            );
            case AUDIO -> Map.of(
                    "voice", aigcProperties.getAudio().getGoogle().getDefaultVoice(),
                    "bpm", aigcProperties.getAudio().getGoogle().getDefaultBpm(),
                    "temperature", aigcProperties.getAudio().getGoogle().getDefaultTemperature(),
                    "timeoutMs", aigcProperties.getAudio().getGoogle().getTimeout()
            );
            case TEXT -> Map.of();
        };
    }

    private String resolveMissingConfig(ContentProvider provider) {
        if (provider.getProviderType() == ContentProvider.ProviderType.GOOGLE && !aigcProperties.isGoogleConfigured()) {
            return "缺少 aigc.providers.google.api-key";
        }
        return null;
    }

    private String resolveModelStatusReason(ContentProvider provider, boolean active) {
        if (!provider.isAvailable()) {
            String missingConfig = resolveMissingConfig(provider);
            return missingConfig != null ? missingConfig : "Provider 未启用或配置不完整";
        }
        if (active) {
            return "当前路由会优先使用此 Provider";
        }
        return "已注册，可通过 active-provider 切换";
    }

    private String resolveProbeMessage(boolean routable, boolean active, boolean available, boolean configurationComplete) {
        if (routable) {
            return "探测通过：当前路由可用";
        }
        if (!active) {
            return "探测通过：Provider 已注册，但不是当前路由";
        }
        if (!configurationComplete) {
            return "探测未通过：配置不完整";
        }
        if (!available) {
            return "探测未通过：Provider 当前不可用";
        }
        return "探测未通过：路由不可用";
    }

    private String resolveRouteUpdateMessage(boolean routable, boolean configurationComplete, boolean available) {
        if (routable) {
            return "已切换：当前路由可用";
        }
        if (!configurationComplete) {
            return "已切换：配置不完整，生成前需要补齐密钥或参数";
        }
        if (!available) {
            return "已切换：Provider 当前不可用";
        }
        return "已切换：路由需要进一步检查";
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
                .orElseThrow(() -> new AigcException(AigcErrorCode.ASSET_NOT_FOUND));
        
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
    public AssetDetailResponse getAssetDetail(String assetId) {
        AigcAsset asset = assetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.ASSET_NOT_FOUND));

        return AssetDetailResponse.builder()
                .asset(toAssetDTO(asset))
                .task(taskRepository.findByAssetId(asset.getAssetId())
                        .map(this::toTaskStatusResponse)
                        .orElse(null))
                .build();
    }

    @Override
    @Transactional
    public void deleteAsset(String assetId) {
        AigcAsset asset = assetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.ASSET_NOT_FOUND));
        deleteLocalAssetFiles(asset);
        assetRepository.deleteByAssetId(asset.getAssetId());
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
            throw new AigcException(AigcErrorCode.CONTENT_TYPE_UNSUPPORTED, "不支持的内容类型: " + contentType);
        }
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void deleteLocalAssetFiles(AigcAsset asset) {
        deleteLocalAssetFile(asset.getUrl(), asset.getAssetId(), "url");
        if (asset.getThumbnailUrl() != null && !asset.getThumbnailUrl().equals(asset.getUrl())) {
            deleteLocalAssetFile(asset.getThumbnailUrl(), asset.getAssetId(), "thumbnailUrl");
        }
    }

    private void deleteLocalAssetFile(String url, String assetId, String fieldName) {
        try {
            localAigcStorageService.deleteByUrl(url);
        } catch (IOException e) {
            log.warn("资产本地文件删除失败，继续删除资产记录: assetId={}, field={}, url={}",
                    assetId, fieldName, url, e);
        }
    }

    private AgentAnalysis resolveAgentAnalysis(AigcTask task) {
        if (task.getAgentAnalysis() != null) {
            return task.getAgentAnalysis();
        }
        if (task.getIntent() == null && task.getContentType() == null && task.getOptimizedPrompt() == null) {
            return null;
        }
        return AgentAnalysis.builder()
                .intent(task.getIntent())
                .contentType(task.getContentType())
                .selectedModel(task.getModel())
                .originalPrompt(task.getPrompt())
                .optimizedPrompt(task.getOptimizedPrompt())
                .build();
    }

    private ProviderExecutionSummary resolveProviderExecution(AigcTask task) {
        if (task.getProviderName() == null && task.getProviderType() == null && task.getDurationMs() == null) {
            return null;
        }
        return ProviderExecutionSummary.builder()
                .providerName(task.getProviderName())
                .providerType(task.getProviderType())
                .model(task.getModel())
                .durationMs(task.getDurationMs())
                .costStatus(resolveCostStatus(task))
                .build();
    }

    private String resolveCostStatus(AigcTask task) {
        if (task.getDurationMs() == null) {
            return "PENDING";
        }
        if ("OTHER".equals(task.getProviderType())
                && task.getProviderName() != null
                && task.getProviderName().toLowerCase().contains("mock")) {
            return "MOCK_FREE";
        }
        return "UNTRACKED";
    }

    private List<MaterialDTO> getReferenceMaterials(List<String> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }
        return materialRepository.findByMaterialIdIn(materialIds).stream()
                .map(this::toMaterialDTO)
                .toList();
    }

    private List<AigcMaterial> validateReferenceMaterials(GenerateRequest request, ContentType contentType) {
        List<AigcMaterial> materials = loadReferenceMaterials(request.getReferenceMaterialIds());
        referenceMaterialPolicy.validate(contentType, materials, request.getReferenceImages());
        return materials;
    }

    private List<AigcMaterial> loadReferenceMaterials(List<String> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }

        Set<String> expectedIds = materialIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        if (expectedIds.isEmpty()) {
            return List.of();
        }

        List<AigcMaterial> materials = materialRepository.findByMaterialIdIn(expectedIds);
        Set<String> foundIds = materials.stream()
                .map(AigcMaterial::getMaterialId)
                .collect(Collectors.toSet());
        if (!foundIds.containsAll(expectedIds)) {
            throw new AigcException(AigcErrorCode.MATERIAL_NOT_FOUND, "引用素材不存在或已删除");
        }
        return materials;
    }

    private List<String> resolveReferenceImages(GenerateRequest request, List<AigcMaterial> materials) {
        if (materials != null && !materials.isEmpty()) {
            return materials.stream().map(AigcMaterial::getUrl).toList();
        }
        return request.getReferenceImages();
    }

    private List<String> resolveReferenceMaterialIds(GenerateRequest request, List<AigcMaterial> materials) {
        if (materials != null && !materials.isEmpty()) {
            return materials.stream().map(AigcMaterial::getMaterialId).toList();
        }
        return request.getReferenceMaterialIds();
    }

    private String toMaterialIdPattern(String materialId) {
        return "%\"" + materialId + "\"%";
    }

    private MaterialDTO toMaterialDTO(AigcMaterial material) {
        return MaterialDTO.builder()
                .id(material.getMaterialId())
                .url(material.getUrl())
                .fileName(material.getFileName())
                .originalFileName(material.getOriginalFileName())
                .contentType(material.getContentType())
                .size(material.getSize())
                .createdAt(material.getCreatedAt().toString())
                .build();
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
