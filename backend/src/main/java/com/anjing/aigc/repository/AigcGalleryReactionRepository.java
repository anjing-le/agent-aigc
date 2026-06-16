package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcGalleryReaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AigcGalleryReactionRepository extends JpaRepository<AigcGalleryReaction, Long> {

    Optional<AigcGalleryReaction> findByAssetIdAndReactionTypeAndActorIdAndTenantKey(
            String assetId, String reactionType, String actorId, String tenantKey);

    long countByAssetIdAndReactionType(String assetId, String reactionType);

    Page<AigcGalleryReaction> findByReactionTypeAndActorIdAndTenantKey(
            String reactionType, String actorId, String tenantKey, Pageable pageable);
}
