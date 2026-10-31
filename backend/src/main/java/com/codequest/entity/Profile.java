package com.codequest.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "profiles")
public class Profile extends BaseEntity {
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "character_name")
    private String characterName;

    @Column(nullable = false)
    private String avatar = "knight";

    /** Company code, e.g. INFOSYS_SP or INFOSYS_DSE (Infosys is the default target). */
    @Column(name = "primary_company", nullable = false)
    private String primaryCompany = "INFOSYS_SP";

    /** JSONB column mapped to a Java List: Hibernate 6 converts it with Jackson. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "later_companies", nullable = false)
    private List<String> laterCompanies = new ArrayList<>();

    @Column(name = "test_date")
    private LocalDate testDate;

    @Column(nullable = false)
    private String timezone = "Asia/Kolkata";

    @Column(name = "daily_minutes", nullable = false)
    private int dailyMinutes = 60;

    @Column(name = "why_statement")
    private String whyStatement;

    /** Self-rating 1..5 per area (JAVA, DSA, SQL, SPRING, COMMUNICATION). Informational only: never skips content. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "self_assessment", nullable = false)
    private Map<String, Integer> selfAssessment = new HashMap<>();

    @Column(name = "onboarding_complete", nullable = false)
    private boolean onboardingComplete;

    @Column(name = "total_xp", nullable = false)
    private int totalXp;

    @Column(nullable = false)
    private int gold;

    @Column(name = "sound_enabled", nullable = false)
    private boolean soundEnabled = true;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCharacterName() { return characterName; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getPrimaryCompany() { return primaryCompany; }
    public void setPrimaryCompany(String primaryCompany) { this.primaryCompany = primaryCompany; }
    public List<String> getLaterCompanies() { return laterCompanies; }
    public void setLaterCompanies(List<String> laterCompanies) { this.laterCompanies = laterCompanies; }
    public LocalDate getTestDate() { return testDate; }
    public void setTestDate(LocalDate testDate) { this.testDate = testDate; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public int getDailyMinutes() { return dailyMinutes; }
    public void setDailyMinutes(int dailyMinutes) { this.dailyMinutes = dailyMinutes; }
    public String getWhyStatement() { return whyStatement; }
    public void setWhyStatement(String whyStatement) { this.whyStatement = whyStatement; }
    public Map<String, Integer> getSelfAssessment() { return selfAssessment; }
    public void setSelfAssessment(Map<String, Integer> selfAssessment) { this.selfAssessment = selfAssessment; }
    public boolean isOnboardingComplete() { return onboardingComplete; }
    public void setOnboardingComplete(boolean onboardingComplete) { this.onboardingComplete = onboardingComplete; }
    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }
    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }
    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }
}
