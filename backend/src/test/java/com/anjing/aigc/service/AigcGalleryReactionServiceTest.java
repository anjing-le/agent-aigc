package com.anjing.aigc.service;

import com.anjing.aigc.model.entity.AigcGalleryReaction;
import com.anjing.aigc.repository.AigcGalleryReactionRepository;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.request.GlobalRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AigcGalleryReactionServiceTest {

    private final AigcGalleryReactionRepository repository = mock(AigcGalleryReactionRepository.class);
    private final AigcOwnershipService ownershipService = new AigcOwnershipService();
    private final AigcGalleryReactionService service =
            new AigcGalleryReactionService(repository, ownershipService);

    @AfterEach
    void tearDown() {
        GlobalRequestContextHolder.clear();
    }

    @Test
    void addReactionCopiesActorFromRequestContext() {
        GlobalRequestContextHolder.set(GlobalRequestContext.builder()
                .userId("user-1")
                .userName("安静")
                .tenantId("tenant-1")
                .callerId("frontend")
                .ip("127.0.0.1")
                .build());
        when(repository.findByAssetIdAndReactionTypeAndActorIdAndTenantKey(
                "asset-1", AigcGalleryReactionService.REACTION_LIKE, "user-1", "tenant-1"))
                .thenReturn(Optional.empty());

        assertTrue(service.addReaction(AigcGalleryReactionService.REACTION_LIKE, "asset-1"));

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(reaction ->
                "asset-1".equals(reaction.getAssetId())
                        && AigcGalleryReactionService.REACTION_LIKE.equals(reaction.getReactionType())
                        && "user-1".equals(reaction.getActorId())
                        && "安静".equals(reaction.getActorName())
                        && "tenant-1".equals(reaction.getTenantId())
                        && "tenant-1".equals(reaction.getTenantKey())
                        && "frontend".equals(reaction.getCallerId())
                        && "127.0.0.1".equals(reaction.getClientIp())
        ));
    }

    @Test
    void addReactionSkipsExistingRelation() {
        AigcGalleryReaction existing = new AigcGalleryReaction();
        existing.setAssetId("asset-1");
        existing.setReactionType(AigcGalleryReactionService.REACTION_FAVORITE);
        existing.setActorId("anonymous");
        existing.setTenantKey("default");
        when(repository.findByAssetIdAndReactionTypeAndActorIdAndTenantKey(
                "asset-1", AigcGalleryReactionService.REACTION_FAVORITE, "anonymous", "default"))
                .thenReturn(Optional.of(existing));

        assertFalse(service.addReaction(AigcGalleryReactionService.REACTION_FAVORITE, "asset-1"));

        verify(repository, never()).save(any(AigcGalleryReaction.class));
    }
}
