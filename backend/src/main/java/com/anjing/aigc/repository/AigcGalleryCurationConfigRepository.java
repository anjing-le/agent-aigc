package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcGalleryCurationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AigcGalleryCurationConfigRepository extends JpaRepository<AigcGalleryCurationConfig, Long> {

    Optional<AigcGalleryCurationConfig> findByRuleId(String ruleId);

    List<AigcGalleryCurationConfig> findByRuleIdIn(Collection<String> ruleIds);
}
