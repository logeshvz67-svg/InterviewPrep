package com.codequest.entity;

/** Small enums used by the Problem-Solving Gym and Focus tools, kept together for easy reading. */
public final class GymEnums {
    private GymEnums() {}

    public enum Platform { LEETCODE, HACKERRANK, GEEKSFORGEEKS, CODECHEF, CODEFORCES, INTERVIEWBIT, OTHER }
    public enum Difficulty { EASY, MEDIUM, HARD }
    /** NONE = solved alone, SMALL = small nudge, FULL_SOLUTION = read the answer. */
    public enum HintLevel { NONE, SMALL, FULL_SOLUTION }
    public enum FocusMode { POMODORO, MVD }
    public enum FocusStatus { RUNNING, COMPLETED, CANCELLED }
    public enum DistractionCategory { PHONE, SOCIAL_MEDIA, VIDEO, MESSAGES, NOISE, TIRED, HUNGRY, OVERTHINKING, WORK, OTHER }
}
