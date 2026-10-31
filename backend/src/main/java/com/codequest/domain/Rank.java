package com.codequest.domain;

/**
 * Story ranks and the total XP needed for each. The display names/emoji live in the
 * frontend theme config; the server only decides WHICH rank you are.
 * Rough pace: ~100-150 XP on a solid day, so Trainee in ~1 week, Architect in ~4 months.
 */
public enum Rank {
    INTERN(0),
    TRAINEE(500),
    DEVELOPER(1500),
    SENIOR_DEV(3500),
    TECH_LEAD(7000),
    ARCHITECT(12000);

    private final int minXp;

    Rank(int minXp) { this.minXp = minXp; }

    public int minXp() { return minXp; }

    public static Rank forXp(int xp) {
        Rank result = INTERN;
        for (Rank r : values()) {
            if (xp >= r.minXp) result = r;
        }
        return result;
    }

    /** Next rank, or null at the top. */
    public Rank next() {
        int i = ordinal() + 1;
        return i < values().length ? values()[i] : null;
    }
}
