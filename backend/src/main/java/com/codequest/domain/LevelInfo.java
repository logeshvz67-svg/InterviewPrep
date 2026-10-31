package com.codequest.domain;

/**
 * Levels are smaller steps inside ranks so progress always feels close.
 * Level n starts at 50 * n * (n - 1) XP: L1=0, L2=100, L3=300, L4=600, L5=1000, L10=4500 ...
 */
public record LevelInfo(int totalXp, int level, int levelStartXp, int nextLevelXp,
                        Rank rank, Rank nextRank, Integer nextRankXp) {

    public static int xpForLevel(int level) {
        return 50 * level * (level - 1);
    }

    public static LevelInfo of(int totalXp) {
        if (totalXp < 0) throw new IllegalArgumentException("XP can't be negative");
        int level = 1;
        while (xpForLevel(level + 1) <= totalXp) level++;
        Rank rank = Rank.forXp(totalXp);
        Rank next = rank.next();
        return new LevelInfo(totalXp, level, xpForLevel(level), xpForLevel(level + 1),
                rank, next, next == null ? null : next.minXp());
    }

    /** 0..100 progress inside the current level, for the XP bar. */
    public int levelProgressPercent() {
        int span = nextLevelXp - levelStartXp;
        return (int) Math.floor(100.0 * (totalXp - levelStartXp) / span);
    }
}
