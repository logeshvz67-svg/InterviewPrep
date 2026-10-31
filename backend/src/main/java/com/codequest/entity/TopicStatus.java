package com.codequest.entity;

/**
 * Learning status of a Java topic. Order matters (each step is "further along").
 * MASTERED can only be reached by passing a "Prove it" test (90%+), never by clicking.
 */
public enum TopicStatus {
    NOT_STARTED(0.0), LEARNING(0.25), UNDERSTOOD(0.6), CAN_EXPLAIN(0.8), MASTERED(1.0);

    private final double weight;
    TopicStatus(double weight) { this.weight = weight; }
    /** How much this status counts toward readiness (0..1). */
    public double weight() { return weight; }
}
