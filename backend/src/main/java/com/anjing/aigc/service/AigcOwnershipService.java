package com.anjing.aigc.service;

import com.anjing.aigc.model.entity.AigcAsset;
import com.anjing.aigc.model.entity.AigcMaterial;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.request.GlobalRequestContext;
import org.springframework.stereotype.Service;

/**
 * AIGC resource ownership boundary backed by the scaffold request context.
 */
@Service
public class AigcOwnershipService {

    public String currentOwnerId() {
        return currentValue(GlobalRequestContext::getUserId);
    }

    public String currentTenantId() {
        return currentValue(GlobalRequestContext::getTenantId);
    }

    public void applyOwnership(AigcAsset asset) {
        if (asset == null) {
            return;
        }
        asset.setOwnerId(currentOwnerId());
        asset.setTenantId(currentTenantId());
    }

    public void applyOwnership(AigcMaterial material) {
        if (material == null) {
            return;
        }
        material.setOwnerId(currentOwnerId());
        material.setTenantId(currentTenantId());
    }

    public void applyOwnership(AigcTask task) {
        if (task == null) {
            return;
        }
        task.setUserId(currentOwnerId());
        task.setTenantId(currentTenantId());
    }

    public boolean canAccess(String ownerId, String tenantId) {
        return canAccessValue(ownerId, currentOwnerId()) && canAccessValue(tenantId, currentTenantId());
    }

    private boolean canAccessValue(String resourceValue, String currentValue) {
        return isBlank(resourceValue) || isBlank(currentValue) || resourceValue.equals(currentValue);
    }

    private String currentValue(java.util.function.Function<GlobalRequestContext, String> getter) {
        return GlobalRequestContextHolder.current()
                .map(getter)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElse(null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
