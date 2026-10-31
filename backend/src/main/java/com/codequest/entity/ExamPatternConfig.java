package com.codequest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/** An exam pattern YOU typed in after checking the official careers page. Nothing is hardcoded. */
@Entity
@Table(name = "exam_pattern_config")
public class ExamPatternConfig extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "company_code", nullable = false) private String companyCode;
    @Column(nullable = false) private String name;
    /** [{name, kind: APTITUDE|TECH_MCQ|CODING, questions, minutes, cutoffPercent}] */
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private JsonNode sections;
    @Column(name = "source_note") private String sourceNote;
    @Column(name = "verified_on") private LocalDate verifiedOn;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String v) { companyCode = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public JsonNode getSections() { return sections; }
    public void setSections(JsonNode v) { sections = v; }
    public String getSourceNote() { return sourceNote; }
    public void setSourceNote(String v) { sourceNote = v; }
    public LocalDate getVerifiedOn() { return verifiedOn; }
    public void setVerifiedOn(LocalDate v) { verifiedOn = v; }
}
