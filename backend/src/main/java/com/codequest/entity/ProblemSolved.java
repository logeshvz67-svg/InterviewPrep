package com.codequest.entity;

import com.codequest.entity.GymEnums.Difficulty;
import com.codequest.entity.GymEnums.HintLevel;
import com.codequest.entity.GymEnums.Platform;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "problems_solved")
public class ProblemSolved extends BaseEntity {
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String title;
    private String link;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Platform platform;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Difficulty difficulty;
    @Column(name = "time_taken_minutes", nullable = false) private int timeTakenMinutes;
    @Column(nullable = false) private int attempts = 1;
    @Enumerated(EnumType.STRING) @Column(name = "hint_level", nullable = false) private HintLevel hintLevel;
    @Column(name = "attempted_before_hint", nullable = false) private boolean attemptedBeforeHint = true;
    @Column(nullable = false) private int confidence;
    @Column(nullable = false) private String approach;
    private String code;
    @Column(name = "solved_on", nullable = false) private LocalDate solvedOn;
    /** Set when the problem came from the curated problem bank (suggestions). */
    @Column(name = "bank_problem_id") private Long bankProblemId;

    /**
     * Many-to-many through the problem_patterns join table.
     * LAZY (the default for collections): patterns load only when accessed inside a transaction.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "problem_patterns",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "pattern_id"))
    private Set<Pattern> patterns = new HashSet<>();

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public int getTimeTakenMinutes() { return timeTakenMinutes; }
    public void setTimeTakenMinutes(int timeTakenMinutes) { this.timeTakenMinutes = timeTakenMinutes; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public HintLevel getHintLevel() { return hintLevel; }
    public void setHintLevel(HintLevel hintLevel) { this.hintLevel = hintLevel; }
    public boolean isAttemptedBeforeHint() { return attemptedBeforeHint; }
    public void setAttemptedBeforeHint(boolean attemptedBeforeHint) { this.attemptedBeforeHint = attemptedBeforeHint; }
    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }
    public String getApproach() { return approach; }
    public void setApproach(String approach) { this.approach = approach; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public LocalDate getSolvedOn() { return solvedOn; }
    public void setSolvedOn(LocalDate solvedOn) { this.solvedOn = solvedOn; }
    public Long getBankProblemId() { return bankProblemId; }
    public void setBankProblemId(Long v) { bankProblemId = v; }
    public Set<Pattern> getPatterns() { return patterns; }
    public void setPatterns(Set<Pattern> patterns) { this.patterns = patterns; }
}
