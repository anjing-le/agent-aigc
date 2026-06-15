package com.anjing.aigc.service;

import com.anjing.aigc.agent.RoutingAgent;
import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.TaskStatus;
import com.anjing.aigc.model.response.ProviderCostEstimate;
import com.anjing.aigc.model.response.GenerationResult;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.provider.ProviderRouter;
import com.anjing.aigc.repository.AigcAssetRepository;
import com.anjing.aigc.repository.AigcTaskRepository;
import com.anjing.model.errorcode.AigcErrorCode;
import com.anjing.model.exception.BizException;
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
    private final ProviderRouter providerRouter;
    private final AigcTaskRepository taskRepository;
    private final AigcAssetRepository assetRepository;
    private final AigcProviderCostEstimator costEstimator;

    @Async
    @Transactional
    public void executeGeneration(String taskId) {
        long startTime = System.currentTimeMillis();
        try {
            AigcTask task = taskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new AigcException(AigcErrorCode.TASK_NOT_FOUND));

            task.setStatus(TaskStatus.PROCESSING);
            task.setProgress(10);
            recordProviderExecutionStart(task);
            task.setUpdatedAt(DateUtils.nowLocalDateTime());
            taskRepository.save(task);

            GenerationResult result = routingAgent.executeGeneration(task);
            long durationMs = System.currentTimeMillis() - startTime;

            if (!result.isSuccess()) {
                log.warn("生成失败，不保存资产: taskId={}, errorCode={}, errorMessage={}",
                        taskId, result.getErrorCode(), result.getErrorMessage());
                task.setStatus(TaskStatus.FAILED);
                task.setDurationMs(durationMs);
                applyCostEstimate(task);
                task.setErrorMessage(result.getErrorMessage());
                task.setErrorCode(result.getErrorCode());
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
            applyCostEstimate(task);
            task.setUpdatedAt(DateUtils.nowLocalDateTime());
            taskRepository.save(task);

            log.info("任务完成: taskId={}, assetId={}, durationMs={}", taskId, asset.getAssetId(), durationMs);
        } catch (Exception e) {
            log.error("任务执行失败: taskId={}", taskId, e);
            taskRepository.findByTaskId(taskId).ifPresent(task -> {
                task.setStatus(TaskStatus.FAILED);
                task.setDurationMs(System.currentTimeMillis() - startTime);
                applyCostEstimate(task);
                task.setErrorMessage(e.getMessage());
                task.setErrorCode(resolveErrorCode(e));
                task.setUpdatedAt(DateUtils.nowLocalDateTime());
                taskRepository.save(task);
            });
        }
    }

    private String resolveErrorCode(Exception e) {
        if (e instanceof BizException bizException && bizException.getErrorCode() != null) {
            return bizException.getErrorCode().getCode();
        }
        return AigcErrorCode.PROVIDER_CALL_FAILED.getCode();
    }

    private void recordProviderExecutionStart(AigcTask task) {
        ContentProvider provider = providerRouter.getProvider(task.getContentType());
        task.setProviderName(provider.getProviderName());
        task.setProviderType(provider.getProviderType().name());
        task.setCostStatus(AigcProviderCostEstimator.STATUS_PENDING);
        task.setEstimatedCostAmount(null);
        task.setEstimatedCostCurrency(null);
        task.setCostUnit(null);
        task.setCostDescription(null);
    }

    private void applyCostEstimate(AigcTask task) {
        ProviderCostEstimate estimate = costEstimator.estimate(task);
        task.setCostStatus(estimate.getCostStatus());
        task.setEstimatedCostAmount(estimate.getEstimatedCostAmount());
        task.setEstimatedCostCurrency(estimate.getEstimatedCostCurrency());
        task.setCostUnit(estimate.getCostUnit());
        task.setCostDescription(estimate.getCostDescription());
    }
}
