package com.anjing.aigc.service;

import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.request.GlobalRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AigcOwnershipServiceTest {

    private final AigcOwnershipService ownershipService = new AigcOwnershipService();

    @AfterEach
    void tearDown() {
        GlobalRequestContextHolder.clear();
    }

    @Test
    void applyOwnershipCopiesScaffoldRequestContext() {
        GlobalRequestContextHolder.set(GlobalRequestContext.builder()
                .userId("user-1")
                .tenantId("tenant-1")
                .build());

        AigcAsset asset = new AigcAsset();
        ownershipService.applyOwnership(asset);

        assertEquals("user-1", asset.getOwnerId());
        assertEquals("tenant-1", asset.getTenantId());
    }

    @Test
    void canAccessAllowsLegacyEmptyOwnershipButRejectsOtherOwners() {
        GlobalRequestContextHolder.set(GlobalRequestContext.builder()
                .userId("user-1")
                .tenantId("tenant-1")
                .build());

        assertTrue(ownershipService.canAccess(null, null));
        assertTrue(ownershipService.canAccess("user-1", "tenant-1"));
        assertFalse(ownershipService.canAccess("user-2", "tenant-1"));
        assertFalse(ownershipService.canAccess("user-1", "tenant-2"));
    }
}
