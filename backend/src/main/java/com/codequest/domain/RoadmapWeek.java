package com.codequest.domain;

import java.time.LocalDate;
import java.util.List;

/** One week of the roadmap. Stored as JSON inside roadmaps.weeks. */
public record RoadmapWeek(int week, LocalDate startDate, String phase, String title, List<String> focus, boolean laterLane) {}
