package com.codequest.entity;

import jakarta.persistence.*;

/** Shared catalog row (same for every user). Read-only for normal users. */
@Entity
@Table(name = "company_tags")
public class CompanyTag extends BaseEntity {
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    public String getCode() { return code; }
    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }
}
