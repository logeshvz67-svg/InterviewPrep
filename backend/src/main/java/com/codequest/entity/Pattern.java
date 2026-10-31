package com.codequest.entity;

import jakarta.persistence.*;

/** Shared catalog row (same for every user). Read-only for normal users. */
@Entity
@Table(name = "patterns")
public class Pattern extends BaseEntity {
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String description;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    /** Patterns we treat as core for Infosys SP-style prep (our judgment, not an official list). */
    @Column(name = "sp_core", nullable = false) private boolean spCore;

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getSortOrder() { return sortOrder; }
    public boolean isSpCore() { return spCore; }
}
