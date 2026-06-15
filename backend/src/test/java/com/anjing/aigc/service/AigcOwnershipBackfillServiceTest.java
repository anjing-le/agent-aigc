package com.anjing.aigc.service;

import com.anjing.aigc.exception.AigcException;
import com.anjing.aigc.model.request.OwnershipBackfillRequest;
import com.anjing.aigc.model.response.OwnershipBackfillResponse;
import com.anjing.aigc.repository.AigcAssetRepository;
import com.anjing.aigc.repository.AigcMaterialRepository;
import com.anjing.aigc.repository.AigcTaskRepository;
import com.anjing.model.errorcode.AigcErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AigcOwnershipBackfillServiceTest {

    private final AigcAssetRepository assetRepository = mock(AigcAssetRepository.class);
    private final AigcMaterialRepository materialRepository = mock(AigcMaterialRepository.class);
    private final AigcTaskRepository taskRepository = mock(AigcTaskRepository.class);
    private final AigcOwnershipService ownershipService = mock(AigcOwnershipService.class);
    private final AigcProviderManagementPermissionService permissionService =
            mock(AigcProviderManagementPermissionService.class);
    private final AigcOwnershipBackfillService service = new AigcOwnershipBackfillService(
            assetRepository,
            materialRepository,
            taskRepository,
            ownershipService,
            permissionService);

    @Test
    void dryRunCountsCandidatesWithoutUpdatingRows() {
        when(ownershipService.currentOwnerId()).thenReturn("user-1");
        when(ownershipService.currentTenantId()).thenReturn("tenant-1");
        when(assetRepository.countMissingOwnership()).thenReturn(2L);
        when(materialRepository.countMissingOwnership()).thenReturn(3L);
        when(taskRepository.countMissingOwnership()).thenReturn(4L);

        OwnershipBackfillResponse response = service.backfill(new OwnershipBackfillRequest());

        assertTrue(response.getDryRun());
        assertEquals(2L, response.getAssetCandidates());
        assertEquals(3L, response.getMaterialCandidates());
        assertEquals(4L, response.getTaskCandidates());
        assertEquals(0, response.getAssetUpdated());
        assertEquals(0, response.getMaterialUpdated());
        assertEquals(0, response.getTaskUpdated());
        verify(assetRepository, never()).backfillMissingOwnership("user-1", "tenant-1");
        verify(materialRepository, never()).backfillMissingOwnership("user-1", "tenant-1");
        verify(taskRepository, never()).backfillMissingOwnership("user-1", "tenant-1");
        verify(permissionService).assertCanManageAigc(
                AigcProviderAuditLogService.ACTION_OWNERSHIP_BACKFILL,
                "AIGC_OWNERSHIP");
    }

    @Test
    void confirmedApplyBackfillsMissingOwnership() {
        OwnershipBackfillRequest request = new OwnershipBackfillRequest();
        request.setDryRun(false);
        request.setConfirmBackfill(true);
        when(ownershipService.currentOwnerId()).thenReturn("user-1");
        when(ownershipService.currentTenantId()).thenReturn("tenant-1");
        when(assetRepository.countMissingOwnership()).thenReturn(2L);
        when(materialRepository.countMissingOwnership()).thenReturn(3L);
        when(taskRepository.countMissingOwnership()).thenReturn(4L);
        when(assetRepository.backfillMissingOwnership("user-1", "tenant-1")).thenReturn(2);
        when(materialRepository.backfillMissingOwnership("user-1", "tenant-1")).thenReturn(3);
        when(taskRepository.backfillMissingOwnership("user-1", "tenant-1")).thenReturn(4);

        OwnershipBackfillResponse response = service.backfill(request);

        assertEquals(false, response.getDryRun());
        assertEquals(true, response.getConfirmed());
        assertEquals(2, response.getAssetUpdated());
        assertEquals(3, response.getMaterialUpdated());
        assertEquals(4, response.getTaskUpdated());
    }

    @Test
    void applyRequiresExplicitConfirmation() {
        OwnershipBackfillRequest request = new OwnershipBackfillRequest();
        request.setDryRun(false);

        AigcException error = assertThrows(AigcException.class, () -> service.backfill(request));

        assertEquals(AigcErrorCode.OWNERSHIP_BACKFILL_INVALID, error.getErrorCode());
        verify(assetRepository, never()).countMissingOwnership();
    }

    @Test
    void backfillRequiresCurrentOwner() {
        when(ownershipService.currentOwnerId()).thenReturn(null);

        AigcException error = assertThrows(AigcException.class,
                () -> service.backfill(new OwnershipBackfillRequest()));

        assertEquals(AigcErrorCode.OWNERSHIP_BACKFILL_INVALID, error.getErrorCode());
        verify(assetRepository, never()).countMissingOwnership();
    }
}
