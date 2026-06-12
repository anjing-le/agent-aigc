package com.anjing.aigc.service;

import com.anjing.aigc.agent.RoutingAgent;
import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.TaskStatus;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.repository.AigcAssetRepository;
import com.anjing.aigc.repository.AigcTaskRepository;
import com.anjing.util.DateUtils;
import com.anjing.util.IdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AigcTaskExecutor {

    private final RoutingAgent routingAgent;
    private final AigcTaskRepository taskRepository;
    private final AigcAssetRepository assetRepository;

    @Async
    @Transactional
    public void executeGeneration(String taskId) {
        long startTime = System.currentTimeMillis();
        try {
            AigcTask task = taskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new AigcException("TASK_NOT_FOUND", "任务不存在"));

            task.setStatus(TaskStatus.PROCESSING);
            task.setProgress(10);
            task.setUpdatedAt(DateUtils.nowLocalDateTime());
            taskRepository.save(task);

            GenerationResult result = routingAgent.executeGeneration(task);
            long durationMs = System.currentTimeMillis() - startTime;

            if (!result.isSuccess()) {
                log.warn("生成失败，不保存资产: taskId={}, errorCode={}, errorMessage={}",
                        taskId, result.getErrorCode(), result.getErrorMessage());
                task.setStatus(TaskStatus.FAILED);
                task.setDurationMs(durationMs);
                task.setErrorMessage(result.getErrorMessage());
                task.setUpdatedAt(DateUtils.nowLocalDateTime());
                taskRepository.save(task);
                return;
            }

            AigcAsset asset = new AigcAsset();
            asset.setAssetId(IdUtils.uuid());
            asset.setContentType(task.getContentType());
            asset.setUrl(result.getUrl());
            asset.setThumbnailUrl(result.getThumbnailUrl());
            asset.setPrompt(task.getPrompt());
            asset.setModel(result.getModel() != null ? result.getModel() : task.getModel());
            asset.setIsPublished(false);
            asset.setCreatedAt(DateUtils.nowLocalDateTime());
            assetRepository.save(asset);

            task.setStatus(TaskStatus.COMPLETED);
            task.setProgress(100);
            task.setAssetId(asset.getAssetId());
            task.setResultUrl(result.getUrl());
            task.setThumbnailUrl(result.getThumbnailUrl());
            task.setDurationMs(durationMs);
            task.setUpdatedAt(DateUtils.nowLocalDateTime());
            taskRepository.save(task);

            log.info("任务完成: taskId={}, assetId={}, durationMs={}", taskId, asset.getAssetId(), durationMs);
        } catch (Exception e) {
            log.error("任务执行失败: taskId={}", taskId, e);
            taskRepository.findByTaskId(taskId).ifPresent(task -> {
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                task.setUpdatedAt(DateUtils.nowLocalDateTime());
                taskRepository.save(task);
            });
        }
    }
}
