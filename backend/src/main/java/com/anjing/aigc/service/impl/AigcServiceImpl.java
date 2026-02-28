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
import com.anjing.aigc.repository.AigcAssetRepository;
import com.anjing.aigc.repository.AigcTaskRepository;
import com.anjing.aigc.service.AigcService;
import com.anjing.aigc.exception.AigcException;
import com.anjing.model.response.PageResponse;
import com.anjing.util.IdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        task.setModel(analysis.getSelectedModel());
        task.setStatus(TaskStatus.PENDING);
        task.setProgress(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        // 3. 异步执行生成任务
        executeGenerationAsync(task.getTaskId());

        // 4. 返回响应
        return GenerateResponse.builder()
                .taskId(task.getTaskId())
                .status(TaskStatus.PENDING)
                .agentAnalysis(analysis)
                .estimatedTime(estimateTime(analysis.getContentType()))
                .build();
    }

    /**
     * 异步执行生成任务
     */
    @Async
    public void executeGenerationAsync(String taskId) {
        try {
            AigcTask task = taskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new AigcException("TASK_NOT_FOUND", "任务不存在"));

            // 更新状态为处理中
            task.setStatus(TaskStatus.PROCESSING);
            task.setProgress(10);
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);

            // 调用对应的模型生成
            GenerationResult result = routingAgent.executeGeneration(task);

            // 检查生成结果是否成功
            if (!result.isSuccess()) {
                // 生成失败，更新任务状态但不保存资产
                log.warn("生成失败，不保存资产: taskId={}, errorCode={}, errorMessage={}", 
                        taskId, result.getErrorCode(), result.getErrorMessage());
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage(result.getErrorMessage());
                task.setUpdatedAt(LocalDateTime.now());
                taskRepository.save(task);
                return;
            }

            // 只有成功时才保存资产
            AigcAsset asset = new AigcAsset();
            asset.setAssetId(IdUtils.uuid());
            asset.setContentType(task.getContentType());
            asset.setUrl(result.getUrl());
            asset.setThumbnailUrl(result.getThumbnailUrl());
            asset.setPrompt(task.getPrompt());
            asset.setModel(task.getModel());
            asset.setIsPublished(false);
            asset.setCreatedAt(LocalDateTime.now());
            assetRepository.save(asset);

            // 更新任务状态为完成
            task.setStatus(TaskStatus.COMPLETED);
            task.setProgress(100);
            task.setAssetId(asset.getAssetId());
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);

            log.info("任务完成: taskId={}, assetId={}", taskId, asset.getAssetId());

        } catch (Exception e) {
            log.error("任务执行失败: taskId={}", taskId, e);
            taskRepository.findByTaskId(taskId).ifPresent(task -> {
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                task.setUpdatedAt(LocalDateTime.now());
                taskRepository.save(task);
            });
        }
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
        // 图片生成模型
        List<ModelInfo> imageModels = List.of(
                ModelInfo.builder()
                        .id("nano-banana")
                        .name("Nano Banana")
                        .description("高质量图片生成模型，支持多种风格")
                        .contentType(ContentType.IMAGE)
                        .provider("Nano Banana")
                        .available(true)
                        .build()
        );

        // 视频生成模型
        List<ModelInfo> videoModels = List.of(
                ModelInfo.builder()
                        .id("sora-2")
                        .name("Sora 2")
                        .description("OpenAI视频生成模型，支持文生视频和图生视频")
                        .contentType(ContentType.VIDEO)
                        .provider("OpenAI")
                        .available(true)
                        .build()
        );

        // TODO: 音频生成模型预留
        List<ModelInfo> audioModels = List.of();

        return ModelListResponse.builder()
                .imageModels(imageModels)
                .videoModels(videoModels)
                .audioModels(audioModels)
                .build();
    }

    @Override
    public PageResponse<GalleryDTO> getGalleryList(Integer current, Integer size, String contentType, String model, String keyword) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<AigcAsset> page = assetRepository.findByIsPublishedTrue(pageRequest);
        
        List<GalleryDTO> records = page.getContent().stream()
                .map(this::toGalleryDTO)
                .collect(Collectors.toList());

        return PageResponse.<GalleryDTO>builder()
                .records(records)
                .current(current)
                .size(size)
                .total(page.getTotalElements())
                .build();
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
    public PageResponse<AssetDTO> getAssetList(Integer current, Integer size, String contentType) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<AigcAsset> page;
        if (contentType != null && !contentType.isEmpty()) {
            page = assetRepository.findByContentType(ContentType.valueOf(contentType.toUpperCase()), pageRequest);
        } else {
            page = assetRepository.findAll(pageRequest);
        }
        
        List<AssetDTO> records = page.getContent().stream()
                .map(this::toAssetDTO)
                .collect(Collectors.toList());

        return PageResponse.<AssetDTO>builder()
                .records(records)
                .current(current)
                .size(size)
                .total(page.getTotalElements())
                .build();
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

