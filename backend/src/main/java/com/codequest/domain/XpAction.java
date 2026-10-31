package com.codequest.domain;

/**
 * Every way to earn XP, with its amount. Rewards PROCESS (showing up, recalling,
 * explaining, attempting before a hint), never raw volume or speed.
 * dailyCap = max times per day this action pays XP (0 = no cap). Stops "farming".
 * Gold = XP / 5, spent later in the cosmetic shop.
 */
public enum XpAction {
    LESSON_COMPLETE(20, 0),
    QUIZ_PASS(25, 0),
    RECALL_SESSION(15, 3),
    FIRST_ATTEMPT_CORRECT(5, 20),
    ATTEMPT_BEFORE_HINT(10, 10),
    PROBLEM_WITH_APPROACH(15, 10),
    REVISIT_RESOLVED(25, 10),
    MISTAKE_RECORDED(10, 5),
    SPEAKING_SESSION(20, 3),
    MOCK_INTERVIEW(60, 2),
    SPRING_MILESTONE(40, 0),
    FOCUS_SESSION_25(10, 8),
    DAILY_COMMITMENT(10, 1),
    EXPLAIN_IT_BACK(20, 5),
    PROVE_IT_PASSED(30, 0),
    // Additions beyond the original table (same "reward the process" spirit):
    MOCK_TEST_COMPLETED(40, 2),      // finishing a full timed mock test
    PRACTICE_SET(15, 3),             // 10+ aptitude or interview practice questions
    SPRING_TOPIC_UNDERSTOOD(25, 0);  // passing a Spring concept quiz

    private final int xp;
    private final int dailyCap;

    XpAction(int xp, int dailyCap) {
        this.xp = xp;
        this.dailyCap = dailyCap;
    }

    public int xp() { return xp; }
    public int dailyCap() { return dailyCap; }
    public int gold() { return xp / 5; }
}
