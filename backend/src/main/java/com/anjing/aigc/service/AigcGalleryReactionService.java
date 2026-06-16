package com.anjing.aigc.service;

import com.anjing.aigc.model.entity.AigcGalleryReaction;
import com.anjing.aigc.repository.AigcGalleryReactionRepository;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.request.GlobalRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * User/session reactions for published gallery assets.
 */
@Service
@RequiredArgsConstructor
public class AigcGalleryReactionService {

    public static final String REACTION_LIKE = "LIKE";
    public static final String REACTION_FAVORITE = "FAVORITE";

    private static final String DEFAULT_TENANT_KEY = "default";
    private static final String DEFAULT_ACTOR_ID = "anonymous";

    private final AigcGalleryReactionRepository reactionRepository;
    private final AigcOwnershipService ownershipService;

    public boolean addReaction(String reactionType, String assetId) {
        ReactionActor actor = currentActor();
        Optional<AigcGalleryReaction> existing = reactionRepository
                .findByAssetIdAndReactionTypeAndActorIdAndTenantKey(
                        assetId, reactionType, actor.actorId(), actor.tenantKey());
        if (existing.isPresent()) {
            return false;
        }

        AigcGalleryReaction reaction = new AigcGalleryReaction();
        reaction.setAssetId(assetId);
        reaction.setReactionType(reactionType);
        reaction.setActorId(actor.actorId());
        reaction.setActorName(actor.actorName());
        reaction.setTenantId(actor.tenantId());
        reaction.setTenantKey(actor.tenantKey());
        reaction.setCallerId(actor.callerId());
        reaction.setClientIp(actor.clientIp());
        reactionRepository.save(reaction);
        return true;
    }

    public boolean removeReaction(String reactionType, String assetId) {
        ReactionActor actor = currentActor();
        Optional<AigcGalleryReaction> existing = reactionRepository
                .findByAssetIdAndReactionTypeAndActorIdAndTenantKey(
                        assetId, reactionType, actor.actorId(), actor.tenantKey());
        if (existing.isEmpty()) {
            return false;
        }
        reactionRepository.delete(existing.get());
        return true;
    }

    public boolean hasReaction(String reactionType, String assetId) {
        ReactionActor actor = currentActor();
        return reactionRepository.findByAssetIdAndReactionTypeAndActorIdAndTenantKey(
                assetId, reactionType, actor.actorId(), actor.tenantKey()).isPresent();
    }

    public long countReactions(String reactionType, String assetId) {
        return reactionRepository.countByAssetIdAndReactionType(assetId, reactionType);
    }

    public Page<AigcGalleryReaction> getMyFavoriteReactions(Pageable pageable) {
        ReactionActor actor = currentActor();
        return reactionRepository.findByReactionTypeAndActorIdAndTenantKey(
                REACTION_FAVORITE, actor.actorId(), actor.tenantKey(), pageable);
    }

    ReactionActor currentActor() {
        GlobalRequestContext context = GlobalRequestContextHolder.current().orElse(null);
        String ownerId = ownershipService.currentOwnerId();
        String tenantId = ownershipService.currentTenantId();
        String callerId = context == null ? null : normalize(context.getCallerId());
        String clientIp = context == null ? null : normalize(context.getIp());
        String actorName = context == null ? null : normalize(context.getUserName());
        String actorId = firstNonBlank(ownerId, prefix("caller", callerId), prefix("ip", clientIp), DEFAULT_ACTOR_ID);
        String tenantKey = firstNonBlank(tenantId, DEFAULT_TENANT_KEY);
        return new ReactionActor(actorId, actorName, tenantId, tenantKey, callerId, clientIp);
    }

    private String prefix(String prefix, String value) {
        return value == null ? null : prefix + ":" + value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    record ReactionActor(String actorId, String actorName, String tenantId, String tenantKey,
            String callerId, String clientIp) {
    }
}
