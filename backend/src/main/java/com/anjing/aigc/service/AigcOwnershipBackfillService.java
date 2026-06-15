package com.anjing.aigc.service;

import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.request.OwnershipBackfillRequest;
import com.anjing.aigc.model.response.OwnershipBackfillResponse;
import com.anjing.aigc.repository.AigcAssetRepository;
import com.anjing.aigc.repository.AigcMaterialRepository;
import com.anjing.aigc.repository.AigcTaskRepository;
import com.anjing.model.errorcode.AigcErrorCode;
import com.anjing.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Backfills scaffold ownership fields for legacy AIGC rows.
 */
@Service
@RequiredArgsConstructor
public class AigcOwnershipBackfillService {

    private static final String RESOURCE_KEY = "AIGC_OWNERSHIP";

    private final AigcAssetRepository assetRepository;
    private final AigcMaterialRepository materialRepository;
    private final AigcTaskRepository taskRepository;
    private final AigcOwnershipService ownershipService;
    private final AigcProviderManagementPermissionService permissionService;

    @Transactional
    public OwnershipBackfillResponse backfill(OwnershipBackfillRequest request) {
        permissionService.assertCanManageAigc(
                AigcProviderAuditLogService.ACTION_OWNERSHIP_BACKFILL,
                RESOURCE_KEY);

        boolean dryRun = request == null || request.getDryRun() == null || request.getDryRun();
        boolean confirmed = request != null && Boolean.TRUE.equals(request.getConfirmBackfill());
        if (!dryRun && !confirmed) {
            throw new AigcException(
                    AigcErrorCode.OWNERSHIP_BACKFILL_INVALID,
                    "执行归属回填前必须设置 confirmBackfill=true");
        }

        String ownerId = ownershipService.currentOwnerId();
        String tenantId = ownershipService.currentTenantId();
        if (!StringUtils.hasText(ownerId)) {
            throw new AigcException(
                    AigcErrorCode.OWNERSHIP_BACKFILL_INVALID,
                    "当前请求上下文缺少 userId，无法确定历史数据归属");
        }

        long assetCandidates = assetRepository.countMissingOwnership();
        long materialCandidates = materialRepository.countMissingOwnership();
        long taskCandidates = taskRepository.countMissingOwnership();

        int assetUpdated = 0;
        int materialUpdated = 0;
        int taskUpdated = 0;
        if (!dryRun) {
            assetUpdated = assetRepository.backfillMissingOwnership(ownerId, tenantId);
            materialUpdated = materialRepository.backfillMissingOwnership(ownerId, tenantId);
            taskUpdated = taskRepository.backfillMissingOwnership(ownerId, tenantId);
        }

        return OwnershipBackfillResponse.builder()
                .dryRun(dryRun)
                .confirmed(confirmed)
                .ownerId(ownerId)
                .tenantId(tenantId)
                .assetCandidates(assetCandidates)
                .materialCandidates(materialCandidates)
                .taskCandidates(taskCandidates)
                .assetUpdated(assetUpdated)
                .materialUpdated(materialUpdated)
                .taskUpdated(taskUpdated)
                .message(dryRun ? "dry-run only; no rows updated" : "ownership backfill applied")
                .checkedAt(DateUtils.nowIso())
                .build();
    }
}
