package com.gotham.cricket.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_migrations")
public class SystemMigration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "migration_key", nullable = false, unique = true)
    private String migrationKey;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    public SystemMigration() {}

    public SystemMigration(String migrationKey) {
        this.migrationKey = migrationKey;
        this.executedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getMigrationKey() {
        return migrationKey;
    }

    public void setMigrationKey(String migrationKey) {
        this.migrationKey = migrationKey;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}