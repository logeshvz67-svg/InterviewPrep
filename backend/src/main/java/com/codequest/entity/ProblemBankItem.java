package com.codequest.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/** A curated practice problem on a free public site (LeetCode / GfG / HackerRank). */
@Entity
@Table(name = "problem_bank")
public class ProblemBankItem extends BaseEntity {
    @Column(nullable = false, unique = true) private String slug;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String url;
    @Column(nullable = false) private String platform;
    @Column(nullable = false) private String difficulty;
    @Column(name = "pattern_code", nullable = false) private String patternCode;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private List<String> companies = new ArrayList<>();
    @Column(nullable = false) private String why;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    public boolean relevantTo(String company) {
        return company == null || company.isBlank() || companies.contains("ALL") || companies.contains(company);
    }

    public String getSlug() { return slug; }
    public void setSlug(String v) { slug = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getUrl() { return url; }
    public void setUrl(String v) { url = v; }
    public String getPlatform() { return platform; }
    public void setPlatform(String v) { platform = v; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String v) { difficulty = v; }
    public String getPatternCode() { return patternCode; }
    public void setPatternCode(String v) { patternCode = v; }
    public List<String> getCompanies() { return companies; }
    public void setCompanies(List<String> v) { companies = v; }
    public String getWhy() { return why; }
    public void setWhy(String v) { why = v; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int v) { sortOrder = v; }
}
