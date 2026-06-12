package com.anjing.aigc.repository;

import com.anjing.aigc.model.entity.AigcMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AigcMaterialRepository extends JpaRepository<AigcMaterial, Long> {

    Optional<AigcMaterial> findByMaterialId(String materialId);
}
