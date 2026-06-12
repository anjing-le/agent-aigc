package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AigcMaterialRepository extends JpaRepository<AigcMaterial, Long> {

    Optional<AigcMaterial> findByMaterialId(String materialId);

    List<AigcMaterial> findByMaterialIdIn(Collection<String> materialIds);

    void deleteByMaterialId(String materialId);

    Page<AigcMaterial> findByContentTypeStartingWith(String contentTypePrefix, Pageable pageable);
}
