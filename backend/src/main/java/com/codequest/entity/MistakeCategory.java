package com.codequest.entity;

import jakarta.persistence.*;

/** Shared catalog row (same for every user). Read-only for normal users. */
@Entity
@Table(name = "mistake_categories")
public class MistakeCategory extends BaseEntity {
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String tip;

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getTip() { return tip; }
}
