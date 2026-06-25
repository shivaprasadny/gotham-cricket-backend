package com.gotham.cricket.repository;

import com.gotham.cricket.entity.SystemMigration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemMigrationRepository extends JpaRepository<SystemMigration, Long> {

    boolean existsByMigrationKey(String migrationKey);
}