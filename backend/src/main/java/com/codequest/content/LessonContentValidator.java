package com.codequest.content;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.*;

/** Java twin of scripts/validate_content.py (core rules), used for imports and admin edits. */
@Component
public class LessonContentValidator {
    private static final Set<String> TYPES = Set.of("MCQ", "TRUE_FALSE", "PREDICT_OUTPUT", "SPOT_THE_BUG", "FILL_THE_CODE", "MATCH");

    public List<String> validate(JsonNode d) {
        List<String> e = new ArrayList<>();
        if (d == null || !d.isObject()) return List.of("lesson must be an object");
        if (!d.path("slug").asText("").matches("[a-z0-9]+(-[a-z0-9]+)*")) e.add("slug must be kebab-case");
        if (!Set.of("JAVA_CORE", "DSA", "SQL", "CS_CORE", "SPRING", "INTERVIEW", "APTITUDE", "SYSTEM_DESIGN").contains(d.path("world").asText())) e.add("unknown world");
        if (d.path("order").asInt(0) < 1) e.add("order must be >= 1");
        for (String k : List.of("section", "title", "why", "analogy", "explanation", "infosysRelevance")) {
            if (d.path(k).asText("").isBlank()) e.add(k + " is required");
        }
        if (!d.path("companies").isArray() || d.path("companies").isEmpty()) e.add("companies required");
        JsonNode quiz = d.path("quiz");
        if (!quiz.isArray() || quiz.size() < 5) e.add("quiz needs at least 5 questions");
        Set<String> ids = new HashSet<>();
        for (JsonNode q : quiz) {
            String id = q.path("id").asText("");
            String type = q.path("type").asText("");
            if (id.isBlank() || !ids.add(id)) e.add("quiz id missing/duplicate: " + id);
            if (!TYPES.contains(type)) { e.add(id + ": bad type " + type); continue; }
            if (q.path("prompt").asText("").isBlank()) e.add(id + ": prompt required");
            switch (type) {
                case "FILL_THE_CODE" -> {
                    if (!q.path("code").asText("").contains("____")) e.add(id + ": code must contain ____");
                    if (!q.path("acceptedAnswers").isArray() || q.path("acceptedAnswers").isEmpty()) e.add(id + ": acceptedAnswers required");
                }
                case "MATCH" -> {
                    if (!q.path("pairs").isArray() || q.path("pairs").size() < 2) e.add(id + ": pairs required");
                }
                default -> {
                    int n = q.path("options").size();
                    int a = q.path("answer").asInt(-1);
                    if (n < 2) e.add(id + ": options required");
                    if (a < 0 || a >= n) e.add(id + ": answer index out of range");
                }
            }
        }
        return e;
    }
}
