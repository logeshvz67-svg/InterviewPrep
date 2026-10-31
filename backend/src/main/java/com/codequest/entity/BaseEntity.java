package com.codequest.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Shared columns for every table: id, created_at, updated_at.
 * @MappedSuperclass = "copy these fields into each child entity's table" (no table of its own).
 * Instant is always UTC, so the DB never stores local times.
 */
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Postgres BIGSERIAL generates the id
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
