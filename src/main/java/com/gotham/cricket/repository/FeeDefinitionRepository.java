package com.gotham.cricket.repository;

import com.gotham.cricket.entity.FeeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for master fee definitions.
 */
public interface FeeDefinitionRepository extends JpaRepository<FeeDefinition, Long> {

    /**
     * Get newest fee definitions first.
     */
    List<FeeDefinition> findAllByOrderByCreatedAtDesc();
}