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
import com.anjing.aigc.model.request.ProviderCredentialUpdateRequest;
import com.anjing.aigc.model.request.ProviderParamUpdateRequest;
import com.anjing.aigc.model.request.ProviderProbeRequest;
import com.anjing.aigc.model.request.ProviderRouteUpdateRequest;
import com.anjing.aigc.model.request.ProviderSmokeTestRequest;
import com.anjing.aigc.model.response.AgentAnalysis;
import com.anjing.aigc.model.response.AssetDetailResponse;
import com.anjing.aigc.model.response.GenerateResponse;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.model.response.ModelListResponse;
import com.anjing.aigc.model.response.ProviderAuditLogResponse;
import com.anjing.aigc.model.response.ProviderExecutionSummary;
import com.anjing.aigc.model.response.ProviderCredentialUpdateResponse;
import com.anjing.aigc.model.response.ProviderCostEstimate;
import com.anjing.aigc.model.response.ProviderDiagnosticCheck;
import com.anjing.aigc.model.response.ProviderParamUpdateResponse;
import com.anjing.aigc.model.response.ProviderProbeResponse;
import com.anjing.aigc.model.response.ProviderRouteUpdateResponse;
import com.anjing.aigc.model.response.ProviderSmokeTestResponse;
import com.anjing.aigc.model.response.TaskStatusResponse;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.provider.ImageGenerationProvider;
import com.anjing.aigc.provider.ProviderRouter;
import com.anjing.aigc.repository.AigcAssetRepository;
import com.anjing.aigc.repository.AigcMaterialRepository;
import com.anjing.aigc.repository.AigcTaskRepository;
import com.anjing.aigc.service.AigcProviderCredentialConfigService;
import com.anjing.aigc.service.AigcGalleryAuditLogService;
import com.anjing.aigc.service.AigcProviderAuditLogService;
import com.anjing.aigc.service.AigcProviderCostEstimator;
import com.anjing.aigc.service.AigcProviderManagementPermissionService;
import com.anjing.aigc.service.AigcProviderParamConfigService;
import com.anjing.aigc.service.AigcReferenceMaterialPolicy;
import com.anjing.aigc.service.AigcOwnershipService;
import com.anjing.aigc.service.AigcProviderRouteConfigService;
import com.anjing.aigc.service.AigcService;
import com.anjing.aigc.service.AigcTaskExecutor;
import com.anjing.aigc.service.storage.AigcStorageService;
import com.anjing.model.constants.ApiConstants;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final AigcProviderAuditLogService auditLogService;
    private final AigcGalleryAuditLogService galleryAuditLogService;
    private final AigcProviderCostEstimator costEstimator;
    private final AigcProviderManagementPermissionService permissionService;
    private final AigcProviderCredentialConfigService credentialConfigService;
    private final AigcProviderParamConfigService paramConfigService;
    private final AigcProviderRouteConfigService routeConfigService;
    private final AigcTaskRepository taskRepository;
    private final AigcAssetRepository assetRepository;
    private final AigcMaterialRepository materialRepository;
    private final AigcReferenceMaterialPolicy referenceMaterialPolicy;
    private final AigcStorageService aigcStorageService;
    private final AigcOwnershipService ownershipService;

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
        ownershipService.applyOwnership(task);
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
        AigcTask task = findVisibleTask(taskId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.TASK_NOT_FOUND));

        return toTaskStatusResponse(task);
    }

    @Override
    @Transactional
    public GenerateResponse retryTask(String taskId) {
        AigcTask sourceTask = findVisibleTask(taskId)
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
        materialRepository.findVisibleByMaterialId(
                        materialId,
                        ownershipService.currentOwnerId(),
                        ownershipService.currentTenantId())
                .orElseThrow(() -> new AigcException(AigcErrorCode.MATERIAL_NOT_FOUND));

        int pageNumber = current != null && current > 0 ? current - 1 : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, 100) : 20;
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AigcTask> page = taskRepository.findVisibleByReferenceMaterialId(
                toMaterialIdPattern(materialId),
                ownershipService.currentOwnerId(),
                ownershipService.currentTenantId(),
                pageRequest
        );

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
            findVisibleAsset(task.getAssetId()).ifPresent(asset -> {
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
        boolean available = provider.isAvailable();
        String missingConfig = resolveMissingConfig(provider);
        ProviderCostEstimate costEstimate = resolveCostProbe(provider, contentType);
        return ModelInfo.builder()
                .id(toModelId(provider, contentType))
                .name(provider.getProviderName())
                .description(toModelDescription(provider, contentType))
                .contentType(contentType)
                .provider(provider.getProviderType().name())
                .activeProvider(getActiveProvider(contentType))
                .routeConfigSource(routeConfigService.getRouteConfigSource(contentType))
                .credentialSource(resolveCredentialSource(provider))
                .credentialStorageMode(resolveCredentialStorageMode(provider))
                .credentialUpdatedAt(resolveCredentialUpdatedAt(provider))
                .active(active)
                .available(available)
                .configuredModel(resolveConfiguredModel(provider, contentType))
                .defaultParams(resolveDefaultParams(provider, contentType))
                .paramConfigSource(resolveParamConfigSource(provider, contentType))
                .paramConfigUpdatedAt(resolveParamConfigUpdatedAt(provider, contentType))
                .costStatus(costEstimate.getCostStatus())
                .costEstimateConfigured(isCostEstimateConfigured(costEstimate))
                .checks(buildProviderChecks(provider, contentType, true, active, available, missingConfig, costEstimate))
                .missingConfig(missingConfig)
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
                    .credentialSource("missing")
                    .credentialStorageMode("missing")
                    .registered(false)
                    .active(false)
                    .available(false)
                    .routable(false)
                    .configurationComplete(false)
                    .defaultParams(Map.of())
                    .paramConfigSource("missing")
                    .costStatus(AigcProviderCostEstimator.STATUS_UNTRACKED)
                    .costEstimateConfigured(false)
                    .checks(buildMissingProviderChecks(request.getProvider()))
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
        ProviderCostEstimate costEstimate = resolveCostProbe(provider, contentType);

        return ProviderProbeResponse.builder()
                .contentType(contentType)
                .requestedProvider(request.getProvider())
                .providerName(provider.getProviderName())
                .providerType(provider.getProviderType().name())
                .activeProvider(activeProvider)
                .credentialSource(resolveCredentialSource(provider))
                .credentialStorageMode(resolveCredentialStorageMode(provider))
                .registered(true)
                .active(active)
                .available(available)
                .routable(routable)
                .configurationComplete(configurationComplete)
                .configuredModel(resolveConfiguredModel(provider, contentType))
                .defaultParams(resolveDefaultParams(provider, contentType))
                .paramConfigSource(resolveParamConfigSource(provider, contentType))
                .paramConfigUpdatedAt(resolveParamConfigUpdatedAt(provider, contentType))
                .costStatus(costEstimate.getCostStatus())
                .costEstimateConfigured(isCostEstimateConfigured(costEstimate))
                .checks(buildProviderChecks(provider, contentType, true, active, available, missingConfig, costEstimate))
                .missingConfig(missingConfig)
                .statusReason(resolveModelStatusReason(provider, active))
                .message(resolveProbeMessage(routable, active, available, configurationComplete))
                .checkedAt(DateUtils.nowIso())
                .build();
    }

    @Override
    public ProviderRouteUpdateResponse updateActiveProvider(ProviderRouteUpdateRequest request) {
        ContentType contentType = request.getContentType();
        assertProviderManagementPermission(
                AigcProviderAuditLogService.ACTION_ACTIVE_PROVIDER, contentType, request.getProvider());
        ContentProvider provider = findProviderForProbe(contentType, request.getProvider(), request.getProviderName());
        if (provider == null) {
            throw new AigcException(AigcErrorCode.PROVIDER_UNAVAILABLE, "Provider 未注册，无法切换路由");
        }

        Map<String, Object> beforeSummary = auditSummary(
                "activeProvider", getActiveProvider(contentType),
                "routeConfigSource", routeConfigService.getRouteConfigSource(contentType)
        );
        String activeProvider = resolveActiveProviderKey(provider);
        routeConfigService.saveActiveProvider(contentType, activeProvider, provider);
        Map<String, Object> afterSummary = auditSummary(
                "activeProvider", activeProvider,
                "routeConfigSource", routeConfigService.getRouteConfigSource(contentType),
                "configuredModel", resolveConfiguredModel(provider, contentType)
        );
        recordProviderAudit(AigcProviderAuditLogService.ACTION_ACTIVE_PROVIDER,
                contentType, activeProvider, provider, beforeSummary, afterSummary);

        boolean available = provider.isAvailable();
        String missingConfig = resolveMissingConfig(provider);
        boolean configurationComplete = missingConfig == null;
        boolean routable = available && configurationComplete;

        return ProviderRouteUpdateResponse.builder()
                .contentType(contentType)
                .activeProvider(activeProvider)
                .routeConfigSource("database")
                .providerName(provider.getProviderName())
                .providerType(provider.getProviderType().name())
                .credentialSource(resolveCredentialSource(provider))
                .credentialStorageMode(resolveCredentialStorageMode(provider))
                .available(available)
                .routable(routable)
                .configurationComplete(configurationComplete)
                .configuredModel(resolveConfiguredModel(provider, contentType))
                .defaultParams(resolveDefaultParams(provider, contentType))
                .paramConfigSource(resolveParamConfigSource(provider, contentType))
                .paramConfigUpdatedAt(resolveParamConfigUpdatedAt(provider, contentType))
                .missingConfig(missingConfig)
                .statusReason(resolveModelStatusReason(provider, true))
                .message(resolveRouteUpdateMessage(routable, configurationComplete, available))
                .updatedAt(DateUtils.nowIso())
                .build();
    }

    @Override
    public ProviderParamUpdateResponse updateProviderParams(ProviderParamUpdateRequest request) {
        ContentType contentType = request.getContentType();
        assertProviderManagementPermission(
                AigcProviderAuditLogService.ACTION_PARAMS, contentType, request.getProvider());
        ContentProvider provider = findProviderForProbe(contentType, request.getProvider(), request.getProviderName());
        if (provider == null) {
            throw new AigcException(AigcErrorCode.PROVIDER_UNAVAILABLE, "Provider 未注册，无法更新参数模板");
        }
        if (provider.getProviderType() != ContentProvider.ProviderType.GOOGLE) {
            throw new AigcException(AigcErrorCode.PROVIDER_UNAVAILABLE, "当前 V1 仅支持更新 Google Provider 参数模板");
        }

        Map<String, Object> beforeSummary = auditSummary(
                "paramConfigSource", resolveParamConfigSource(provider, contentType),
                "defaultParams", resolveDefaultParams(provider, contentType)
        );
        paramConfigService.saveGoogleDefaultParams(contentType, request.getDefaultParams(), provider);
        Map<String, Object> afterSummary = auditSummary(
                "paramConfigSource", resolveParamConfigSource(provider, contentType),
                "defaultParams", resolveDefaultParams(provider, contentType)
        );
        recordProviderAudit(AigcProviderAuditLogService.ACTION_PARAMS,
                contentType, AigcProviderParamConfigService.GOOGLE_PROVIDER_KEY, provider, beforeSummary, afterSummary);

        return ProviderParamUpdateResponse.builder()
                .contentType(contentType)
                .providerName(provider.getProviderName())
                .providerType(provider.getProviderType().name())
                .paramConfigSource(resolveParamConfigSource(provider, contentType))
                .defaultParams(resolveDefaultParams(provider, contentType))
                .message("Provider 参数模板已保存")
                .updatedAt(DateUtils.nowIso())
                .build();
    }

    @Override
    public ProviderCredentialUpdateResponse updateProviderCredential(ProviderCredentialUpdateRequest request) {
        ContentType contentType = request.getContentType();
        assertProviderManagementPermission(
                AigcProviderAuditLogService.ACTION_CREDENTIAL, contentType, request.getProvider());
        ContentProvider provider = findProviderForProbe(contentType, request.getProvider(), request.getProviderName());
        if (provider == null) {
            throw new AigcException(AigcErrorCode.PROVIDER_UNAVAILABLE, "Provider 未注册，无法更新凭证");
        }
        if (provider.getProviderType() != ContentProvider.ProviderType.GOOGLE) {
            throw new AigcException(AigcErrorCode.PROVIDER_UNAVAILABLE, "当前 V1 仅支持更新 Google Provider 凭证");
        }

        Map<String, Object> beforeSummary = auditSummary(
                "credentialSource", resolveCredentialSource(provider),
                "credentialStorageMode", resolveCredentialStorageMode(provider),
                "configurationComplete", resolveMissingConfig(provider) == null
        );
        credentialConfigService.saveGoogleCredential(request.getCredential(), provider);

        boolean available = provider.isAvailable();
        String missingConfig = resolveMissingConfig(provider);
        boolean configurationComplete = missingConfig == null;
        Map<String, Object> afterSummary = auditSummary(
                "credentialSource", resolveCredentialSource(provider),
                "credentialStorageMode", resolveCredentialStorageMode(provider),
                "configurationComplete", configurationComplete,
                "available", available
        );
        recordProviderAudit(AigcProviderAuditLogService.ACTION_CREDENTIAL,
                contentType, AigcProviderCredentialConfigService.GOOGLE_PROVIDER_KEY, provider, beforeSummary, afterSummary);

        return ProviderCredentialUpdateResponse.builder()
                .contentType(contentType)
                .providerName(provider.getProviderName())
                .providerType(provider.getProviderType().name())
                .credentialSource(resolveCredentialSource(provider))
                .credentialStorageMode(resolveCredentialStorageMode(provider))
                .configurationComplete(configurationComplete)
                .available(available)
                .statusReason(resolveModelStatusReason(provider, isActiveProvider(provider, getActiveProvider(contentType))))
                .message(resolveCredentialUpdateMessage(configurationComplete, available))
                .updatedAt(DateUtils.nowIso())
                .build();
    }

    @Override
    @Transactional
    public ProviderSmokeTestResponse smokeTestProvider(ProviderSmokeTestRequest request) {
        ContentType contentType = request.getContentType();
        ContentProvider provider = findProviderForProbe(contentType, request.getProvider(), request.getProviderName());
        if (provider == null) {
            return smokeTestSkipped(request, null, "Provider 未注册，无法运行 smoke test");
        }
        if (contentType != ContentType.IMAGE || !(provider instanceof ImageGenerationProvider)) {
            return smokeTestSkipped(request, provider, "V1 smoke test 仅支持图片 Provider");
        }
        boolean confirmExternalCall = Boolean.TRUE.equals(request.getConfirmExternalCall());
        if (provider.getProviderType() == ContentProvider.ProviderType.GOOGLE && !confirmExternalCall) {
            return smokeTestSkipped(request, provider, "Google smoke test 会触发外部调用，请显式确认");
        }
        if (!provider.isAvailable()) {
            String missingConfig = resolveMissingConfig(provider);
            return smokeTestSkipped(request, provider,
                    missingConfig != null ? missingConfig : "Provider 当前不可用，无法运行 smoke test");
        }

        long startTime = System.currentTimeMillis();
        AigcTask task = createSmokeTestTask(request, provider);
        taskRepository.save(task);

        GenerationResult result;
        try {
            result = provider.generate(task);
        } catch (Exception e) {
            result = GenerationResult.failure(
                    task.getTaskId(),
                    AigcErrorCode.PROVIDER_CALL_FAILED.getCode(),
                    e.getMessage()
            );
        }

        long durationMs = System.currentTimeMillis() - startTime;
        if (result != null && result.isSuccess()) {
            AigcAsset asset = createAssetFromSmokeTest(task, result);
            assetRepository.save(asset);
            task.setStatus(TaskStatus.COMPLETED);
            task.setProgress(100);
            task.setAssetId(asset.getAssetId());
            task.setResultUrl(result.getUrl());
            task.setThumbnailUrl(result.getThumbnailUrl());
            task.setModel(result.getModel());
            task.setDurationMs(durationMs);
            applyCostEstimate(task);
            task.setUpdatedAt(DateUtils.nowLocalDateTime());
            taskRepository.save(task);

            recordProviderAudit(AigcProviderAuditLogService.ACTION_SMOKE_TEST,
                    contentType, request.getProvider(), provider,
                    Map.of(),
                    auditSummary(
                            "status", "COMPLETED",
                            "taskId", task.getTaskId(),
                            "assetId", asset.getAssetId(),
                            "model", task.getModel(),
                            "durationMs", durationMs
                    ));

            return ProviderSmokeTestResponse.builder()
                    .contentType(contentType)
                    .providerName(provider.getProviderName())
                    .providerType(provider.getProviderType().name())
                    .taskId(task.getTaskId())
                    .assetId(asset.getAssetId())
                    .success(true)
                    .status(TaskStatus.COMPLETED.name())
                    .model(task.getModel())
                    .prompt(task.getPrompt())
                    .url(result.getUrl())
                    .thumbnailUrl(result.getThumbnailUrl())
                    .durationMs(durationMs)
                    .providerExecution(resolveProviderExecution(task))
                    .message("Smoke test 通过，已保存测试资产")
                    .checkedAt(DateUtils.nowIso())
                    .build();
        }

        String errorCode = result == null || result.getErrorCode() == null
                ? AigcErrorCode.PROVIDER_CALL_FAILED.getCode()
                : result.getErrorCode();
        String errorMessage = result == null || result.getErrorMessage() == null
                ? "Provider 未返回有效结果"
                : result.getErrorMessage();
        task.setStatus(TaskStatus.FAILED);
        task.setProgress(100);
        task.setDurationMs(durationMs);
        task.setErrorCode(errorCode);
        task.setErrorMessage(errorMessage);
        applyCostEstimate(task);
        task.setUpdatedAt(DateUtils.nowLocalDateTime());
        taskRepository.save(task);

        recordProviderAudit(AigcProviderAuditLogService.ACTION_SMOKE_TEST,
                contentType, request.getProvider(), provider,
                Map.of(),
                auditSummary(
                        "status", "FAILED",
                        "taskId", task.getTaskId(),
                        "errorCode", errorCode,
                        "durationMs", durationMs
                ));

        return ProviderSmokeTestResponse.builder()
                .contentType(contentType)
                .providerName(provider.getProviderName())
                .providerType(provider.getProviderType().name())
                .taskId(task.getTaskId())
                .success(false)
                .status(TaskStatus.FAILED.name())
                .model(task.getModel())
                .prompt(task.getPrompt())
                .durationMs(durationMs)
                .providerExecution(resolveProviderExecution(task))
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .message("Smoke test 失败，请查看错误信息")
                .checkedAt(DateUtils.nowIso())
                .build();
    }

    @Override
    public PageResult<ProviderAuditLogResponse> getProviderAuditLogs(
            Integer current, Integer size, String contentType, String action) {
        return auditLogService.getAuditLogs(current, size, parseContentType(contentType), action);
    }

    private AigcTask createSmokeTestTask(ProviderSmokeTestRequest request, ContentProvider provider) {
        String prompt = normalizeSmokeTestPrompt(request.getPrompt());
        String model = resolveConfiguredModel(provider, request.getContentType());
        AigcTask task = new AigcTask();
        task.setTaskId(IdUtils.uuid());
        task.setPrompt(prompt);
        task.setOptimizedPrompt(prompt);
        task.setContentType(request.getContentType());
        task.setIntent("provider_smoke_test");
        task.setModel(model);
        task.setProviderName(provider.getProviderName());
        task.setProviderType(provider.getProviderType().name());
        task.setStatus(TaskStatus.PROCESSING);
        task.setProgress(10);
        task.setCostStatus(AigcProviderCostEstimator.STATUS_PENDING);
        ownershipService.applyOwnership(task);
        task.setAgentAnalysis(AgentAnalysis.builder()
                .intent("provider_smoke_test")
                .contentType(request.getContentType())
                .selectedModel(model)
                .originalPrompt(prompt)
                .cleanPrompt(prompt)
                .optimizedPrompt(prompt)
                .confidence(1.0)
                .build());
        task.setCreatedAt(DateUtils.nowLocalDateTime());
        task.setUpdatedAt(DateUtils.nowLocalDateTime());
        return task;
    }

    private AigcAsset createAssetFromSmokeTest(AigcTask task, GenerationResult result) {
        AigcAsset asset = new AigcAsset();
        asset.setAssetId(IdUtils.uuid());
        asset.setContentType(ContentType.IMAGE);
        asset.setUrl(result.getUrl());
        asset.setThumbnailUrl(result.getThumbnailUrl());
        asset.setPrompt(task.getPrompt());
        asset.setModel(result.getModel());
        asset.setOwnerId(task.getUserId());
        asset.setTenantId(task.getTenantId());
        asset.setIsPublished(false);
        asset.setCreatedAt(DateUtils.nowLocalDateTime());
        return asset;
    }

    private String normalizeSmokeTestPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "A tiny clean smoke test image for agent-aigc provider validation";
        }
        return prompt.trim();
    }

    private ProviderSmokeTestResponse smokeTestSkipped(ProviderSmokeTestRequest request, ContentProvider provider,
            String message) {
        return ProviderSmokeTestResponse.builder()
                .contentType(request.getContentType())
                .providerName(provider == null ? request.getProviderName() : provider.getProviderName())
                .providerType(provider == null ? request.getProvider() : provider.getProviderType().name())
                .success(false)
                .status("SKIPPED")
                .prompt(normalizeSmokeTestPrompt(request.getPrompt()))
                .errorCode(AigcErrorCode.PROVIDER_UNAVAILABLE.getCode())
                .errorMessage(message)
                .message(message)
                .checkedAt(DateUtils.nowIso())
                .build();
    }

    private void applyCostEstimate(AigcTask task) {
        ProviderCostEstimate estimate = costEstimator.estimate(task);
        task.setCostStatus(estimate.getCostStatus());
        task.setEstimatedCostAmount(estimate.getEstimatedCostAmount());
        task.setEstimatedCostCurrency(estimate.getEstimatedCostCurrency());
        task.setCostUnit(estimate.getCostUnit());
        task.setCostDescription(estimate.getCostDescription());
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

    private List<ProviderDiagnosticCheck> buildMissingProviderChecks(String requestedProvider) {
        return List.of(check(
                "registered",
                "Provider 注册",
                "FAIL",
                "未找到已注册 Provider: " + (requestedProvider == null ? "-" : requestedProvider)
        ));
    }

    private List<ProviderDiagnosticCheck> buildProviderChecks(ContentProvider provider, ContentType contentType,
            boolean registered, boolean active, boolean available, String missingConfig,
            ProviderCostEstimate costEstimate) {
        String configuredModel = resolveConfiguredModel(provider, contentType);
        String paramConfigSource = resolveParamConfigSource(provider, contentType);
        boolean configurationComplete = missingConfig == null;
        boolean routable = registered && active && available && configurationComplete;

        return List.of(
                check("registered", "Provider 注册", registered ? "PASS" : "FAIL",
                        registered ? "Provider 已注册到 Spring 容器" : "Provider 未注册到 Spring 容器"),
                check("route", "运行路由", active ? "PASS" : "WARN",
                        active ? "当前内容类型会路由到此 Provider" : "已注册，但不是当前内容类型的激活路由"),
                check("credential", "凭证配置", configurationComplete ? "PASS" : "FAIL",
                        configurationComplete ? "凭证或配置已满足运行要求" : missingConfig),
                check("availability", "可用性", available ? "PASS" : "FAIL",
                        available ? "Provider 当前可用" : "Provider 当前不可用"),
                check("model", "模型配置", configuredModel == null || configuredModel.isBlank() ? "FAIL" : "PASS",
                        configuredModel == null || configuredModel.isBlank()
                                ? "缺少内容类型对应的模型配置"
                                : "模型: " + configuredModel),
                check("params", "参数模板", "missing".equals(paramConfigSource) ? "WARN" : "PASS",
                        "参数来源: " + resolveConfigSourceLabel(paramConfigSource)),
                check("cost", "成本估算", isCostEstimateConfigured(costEstimate) ? "PASS" : "WARN",
                        resolveCostProbeMessage(costEstimate)),
                check("routable", "生成就绪", routable ? "PASS" : "WARN",
                        routable ? "当前 Provider 可执行生成任务" : "生成前仍需处理上方检查项")
        );
    }

    private ProviderDiagnosticCheck check(String id, String label, String status, String message) {
        return ProviderDiagnosticCheck.builder()
                .id(id)
                .label(label)
                .status(status)
                .message(message)
                .build();
    }

    private String resolveConfigSourceLabel(String source) {
        if ("database".equals(source)) {
            return "页面保存";
        }
        if ("configuration".equals(source)) {
            return "环境配置";
        }
        if ("not-required".equals(source)) {
            return "无需配置";
        }
        if ("missing".equals(source)) {
            return "未配置";
        }
        return "-";
    }

    private ProviderCostEstimate resolveCostProbe(ContentProvider provider, ContentType contentType) {
        AigcTask task = new AigcTask();
        task.setContentType(contentType);
        task.setProviderName(provider.getProviderName());
        task.setProviderType(provider.getProviderType().name());
        task.setDurationMs(1L);
        return costEstimator.estimate(task);
    }

    private boolean isCostEstimateConfigured(ProviderCostEstimate costEstimate) {
        return AigcProviderCostEstimator.STATUS_MOCK_FREE.equals(costEstimate.getCostStatus())
                || AigcProviderCostEstimator.STATUS_ESTIMATED.equals(costEstimate.getCostStatus());
    }

    private String resolveCostProbeMessage(ProviderCostEstimate costEstimate) {
        if (AigcProviderCostEstimator.STATUS_MOCK_FREE.equals(costEstimate.getCostStatus())) {
            return "本地 mock provider 不产生外部模型成本";
        }
        if (AigcProviderCostEstimator.STATUS_ESTIMATED.equals(costEstimate.getCostStatus())) {
            return costEstimate.getCostDescription();
        }
        if (AigcProviderCostEstimator.STATUS_ESTIMATE_NOT_CONFIGURED.equals(costEstimate.getCostStatus())) {
            return "已接入成本字段，单价待通过 aigc.cost.google.* 配置";
        }
        return "暂未接入此 Provider 的成本估算";
    }

    private String getActiveProvider(ContentType contentType) {
        return switch (contentType) {
            case IMAGE, VIDEO, AUDIO -> routeConfigService.getActiveProvider(contentType);
            case TEXT -> routeConfigService.getConfiguredActiveProvider(contentType);
        };
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
        return paramConfigService.getDefaultParams(provider, contentType);
    }

    private String resolveParamConfigSource(ContentProvider provider, ContentType contentType) {
        return paramConfigService.getParamConfigSource(provider, contentType);
    }

    private String resolveParamConfigUpdatedAt(ContentProvider provider, ContentType contentType) {
        return paramConfigService.getParamConfigUpdatedAt(provider, contentType) == null
                ? null
                : paramConfigService.getParamConfigUpdatedAt(provider, contentType).toString();
    }

    private String resolveMissingConfig(ContentProvider provider) {
        if (provider.getProviderType() == ContentProvider.ProviderType.GOOGLE
                && !credentialConfigService.isGoogleConfigured()) {
            return "缺少 Google Provider 凭证";
        }
        return null;
    }

    private String resolveCredentialSource(ContentProvider provider) {
        if (provider.getProviderType() == ContentProvider.ProviderType.GOOGLE) {
            return credentialConfigService.getGoogleCredentialSource();
        }
        if (provider.getProviderType() == ContentProvider.ProviderType.OTHER) {
            return "not-required";
        }
        return "missing";
    }

    private String resolveCredentialStorageMode(ContentProvider provider) {
        if (provider.getProviderType() == ContentProvider.ProviderType.GOOGLE) {
            return credentialConfigService.getGoogleCredentialStorageMode();
        }
        if (provider.getProviderType() == ContentProvider.ProviderType.OTHER) {
            return "not-required";
        }
        return "missing";
    }

    private String resolveCredentialUpdatedAt(ContentProvider provider) {
        if (provider.getProviderType() != ContentProvider.ProviderType.GOOGLE) {
            return null;
        }
        return credentialConfigService.getGoogleCredentialUpdatedAt() == null
                ? null
                : credentialConfigService.getGoogleCredentialUpdatedAt().toString();
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

    private String resolveCredentialUpdateMessage(boolean configurationComplete, boolean available) {
        if (configurationComplete && available) {
            return "Provider 凭证已保存，当前配置可用";
        }
        if (configurationComplete) {
            return "Provider 凭证已保存，请继续检查 Provider 启用状态";
        }
        return "Provider 凭证已保存，但配置仍不完整";
    }

    private void recordProviderAudit(String action, ContentType contentType, String providerKey,
            ContentProvider provider, Map<String, Object> beforeSummary, Map<String, Object> afterSummary) {
        auditLogService.record(
                action,
                contentType,
                providerKey,
                provider.getProviderName(),
                provider.getProviderType().name(),
                beforeSummary,
                afterSummary
        );
    }

    private void assertProviderManagementPermission(String action, ContentType contentType, String providerKey) {
        permissionService.assertCanManageProvider(action, contentType, providerKey);
    }

    private Map<String, Object> auditSummary(Object... values) {
        Map<String, Object> summary = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            Object value = values[index + 1];
            if (value != null) {
                summary.put(String.valueOf(values[index]), value);
            }
        }
        return summary;
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
        AigcAsset asset = findVisibleAsset(assetId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.ASSET_NOT_FOUND));
        
        asset.setIsPublished(true);
        AigcAsset savedAsset = assetRepository.save(asset);
        galleryAuditLogService.recordSuccess(AigcGalleryAuditLogService.ACTION_PUBLISH, savedAsset);
    }

    @Override
    @Transactional
    public void removeFromGallery(String assetId) {
        AigcAsset asset = findVisibleAsset(assetId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.ASSET_NOT_FOUND));

        asset.setIsPublished(false);
        AigcAsset savedAsset = assetRepository.save(asset);
        galleryAuditLogService.recordSuccess(AigcGalleryAuditLogService.ACTION_UNPUBLISH, savedAsset);
    }

    @Override
    @Transactional
    public GalleryDTO likeGalleryAsset(String assetId) {
        AigcAsset asset = findPublishedAsset(assetId);
        asset.setLikeCount(resolveLikeCount(asset) + 1);
        AigcAsset savedAsset = assetRepository.save(asset);
        galleryAuditLogService.recordSuccess(AigcGalleryAuditLogService.ACTION_LIKE, savedAsset);
        return toGalleryDTO(savedAsset);
    }

    @Override
    @Transactional
    public GalleryDTO unlikeGalleryAsset(String assetId) {
        AigcAsset asset = findPublishedAsset(assetId);
        asset.setLikeCount(Math.max(0, resolveLikeCount(asset) - 1));
        AigcAsset savedAsset = assetRepository.save(asset);
        galleryAuditLogService.recordSuccess(AigcGalleryAuditLogService.ACTION_UNLIKE, savedAsset);
        return toGalleryDTO(savedAsset);
    }

    @Override
    @Transactional
    public GalleryDTO favoriteGalleryAsset(String assetId) {
        AigcAsset asset = findPublishedAsset(assetId);
        asset.setFavoriteCount(resolveFavoriteCount(asset) + 1);
        AigcAsset savedAsset = assetRepository.save(asset);
        galleryAuditLogService.recordSuccess(AigcGalleryAuditLogService.ACTION_FAVORITE, savedAsset);
        return toGalleryDTO(savedAsset);
    }

    @Override
    @Transactional
    public GalleryDTO unfavoriteGalleryAsset(String assetId) {
        AigcAsset asset = findPublishedAsset(assetId);
        asset.setFavoriteCount(Math.max(0, resolveFavoriteCount(asset) - 1));
        AigcAsset savedAsset = assetRepository.save(asset);
        galleryAuditLogService.recordSuccess(AigcGalleryAuditLogService.ACTION_UNFAVORITE, savedAsset);
        return toGalleryDTO(savedAsset);
    }

    @Override
    public PageResult<AssetDTO> getAssetList(Integer current, Integer size, String contentType) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<AigcAsset> page = assetRepository.findVisibleAssets(
                ownershipService.currentOwnerId(),
                ownershipService.currentTenantId(),
                parseContentType(contentType),
                pageRequest
        );
        
        List<AssetDTO> records = page.getContent().stream()
                .map(this::toAssetDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotalElements(), current, size);
    }

    @Override
    public AssetDetailResponse getAssetDetail(String assetId) {
        AigcAsset asset = findVisibleAsset(assetId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.ASSET_NOT_FOUND));

        return AssetDetailResponse.builder()
                .asset(toAssetDTO(asset))
                .task(taskRepository.findVisibleByAssetId(
                                asset.getAssetId(),
                                ownershipService.currentOwnerId(),
                                ownershipService.currentTenantId())
                        .map(this::toTaskStatusResponse)
                        .orElse(null))
                .build();
    }

    @Override
    @Transactional
    public void deleteAsset(String assetId) {
        AigcAsset asset = findVisibleAsset(assetId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.ASSET_NOT_FOUND));
        deleteAssetFiles(asset);
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

    private void deleteAssetFiles(AigcAsset asset) {
        deleteAssetFile(asset.getUrl(), asset.getAssetId(), "url");
        if (asset.getThumbnailUrl() != null && !asset.getThumbnailUrl().equals(asset.getUrl())) {
            deleteAssetFile(asset.getThumbnailUrl(), asset.getAssetId(), "thumbnailUrl");
        }
    }

    private void deleteAssetFile(String url, String assetId, String fieldName) {
        try {
            aigcStorageService.deleteByUrl(url);
        } catch (IOException e) {
            log.warn("资产文件删除失败，继续删除资产记录: assetId={}, field={}, url={}",
                    assetId, fieldName, url, e);
        }
    }

    private Optional<AigcAsset> findVisibleAsset(String assetId) {
        return assetRepository.findVisibleByAssetId(
                assetId,
                ownershipService.currentOwnerId(),
                ownershipService.currentTenantId()
        );
    }

    private Optional<AigcTask> findVisibleTask(String taskId) {
        return taskRepository.findVisibleByTaskId(
                taskId,
                ownershipService.currentOwnerId(),
                ownershipService.currentTenantId()
        );
    }

    private AigcAsset findPublishedAsset(String assetId) {
        return assetRepository.findByAssetIdAndIsPublishedTrue(assetId)
                .orElseThrow(() -> new AigcException(AigcErrorCode.ASSET_NOT_FOUND));
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
        ProviderCostEstimate costEstimate = resolveCostEstimate(task);
        return ProviderExecutionSummary.builder()
                .providerName(task.getProviderName())
                .providerType(task.getProviderType())
                .model(task.getModel())
                .durationMs(task.getDurationMs())
                .costStatus(costEstimate.getCostStatus())
                .estimatedCostAmount(costEstimate.getEstimatedCostAmount())
                .estimatedCostCurrency(costEstimate.getEstimatedCostCurrency())
                .costUnit(costEstimate.getCostUnit())
                .costDescription(costEstimate.getCostDescription())
                .build();
    }

    private ProviderCostEstimate resolveCostEstimate(AigcTask task) {
        if (task.getCostStatus() == null) {
            return costEstimator.estimate(task);
        }
        return ProviderCostEstimate.builder()
                .costStatus(task.getCostStatus())
                .estimatedCostAmount(task.getEstimatedCostAmount())
                .estimatedCostCurrency(task.getEstimatedCostCurrency())
                .costUnit(task.getCostUnit())
                .costDescription(task.getCostDescription())
                .build();
    }

    private List<MaterialDTO> getReferenceMaterials(List<String> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }
        return materialRepository.findVisibleByMaterialIdIn(
                        materialIds,
                        ownershipService.currentOwnerId(),
                        ownershipService.currentTenantId()).stream()
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

        List<AigcMaterial> materials = materialRepository.findVisibleByMaterialIdIn(
                expectedIds,
                ownershipService.currentOwnerId(),
                ownershipService.currentTenantId()
        );
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
        String previewUrl = buildGalleryPreviewUrl(asset);
        return GalleryDTO.builder()
                .id(asset.getAssetId())
                .contentType(asset.getContentType())
                .url(previewUrl)
                .thumbnailUrl(asset.getContentType() == ContentType.IMAGE ? previewUrl : null)
                .previewUrl(previewUrl)
                .publicAccessMode("published-preview")
                .prompt(asset.getPrompt())
                .model(asset.getModel())
                .isPublished(asset.getIsPublished())
                .createdAt(asset.getCreatedAt())
                .authorName(null) // TODO: 关联用户
                .likeCount(resolveLikeCount(asset))
                .likedByCurrentUser(false)
                .favoriteCount(resolveFavoriteCount(asset))
                .favoritedByCurrentUser(false)
                .build();
    }

    private int resolveLikeCount(AigcAsset asset) {
        return asset.getLikeCount() == null ? 0 : asset.getLikeCount();
    }

    private int resolveFavoriteCount(AigcAsset asset) {
        return asset.getFavoriteCount() == null ? 0 : asset.getFavoriteCount();
    }

    private String buildGalleryPreviewUrl(AigcAsset asset) {
        return ApiConstants.Aigc.GALLERY_ASSET_PREVIEW_FULL
                .replace("{assetId}", asset.getAssetId());
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
