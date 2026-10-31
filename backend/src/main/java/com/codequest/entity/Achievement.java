package com.codequest.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "achievements")
public class Achievement extends BaseEntity {
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String description;
    @Column(nullable = false) private String icon;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public int getSortOrder() { return sortOrder; }
}
