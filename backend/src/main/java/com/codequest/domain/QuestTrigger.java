package com.codequest.domain;

/** User actions that can advance a quest. Services call QuestService.advance(user, trigger). */
public enum QuestTrigger {
    FOCUS_25, FOCUS_ANY, TOPIC_PROGRESS, PROBLEM_LOGGED, MISTAKE_RECORDED, REVISIT_DONE, DAILY_COMMITMENT, MVD_DONE,
    LESSON_DONE, RECALL_DONE, SPEAKING_DONE,
    /** Weekly/story quests whose progress is measured from data (see quests.metric). */
    COMPUTED
}
