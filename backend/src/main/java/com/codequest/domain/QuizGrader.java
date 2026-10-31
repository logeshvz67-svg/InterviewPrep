package com.codequest.domain;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Grades one answer on the SERVER. The browser only sends what the learner chose;
 * the correct answer never leaves the server before submission.
 */
public final class QuizGrader {
    private QuizGrader() {}

    public static boolean isOptionType(String type) {
        return switch (type) {
            case "MCQ", "TRUE_FALSE", "PREDICT_OUTPUT", "SPOT_THE_BUG" -> true;
            default -> false;
        };
    }

    /**
     * @param type   question type
     * @param answer stored answer JSON ({index}, {accepted:[...]}, {pairs:[...]})
     * @param given  what the learner sent: an int index, a text, or an array of right-hand strings for MATCH
     */
    public static boolean isCorrect(String type, JsonNode answer, JsonNode given) {
        if (given == null || given.isNull() || answer == null) return false;
        if (isOptionType(type)) {
            return given.canConvertToInt() && given.asInt() == answer.path("index").asInt(-1);
        }
        if ("FILL_THE_CODE".equals(type)) {
            if (!given.isTextual()) return false;
            String g = normalize(given.asText());
            if (g.isEmpty()) return false;
            for (JsonNode acc : answer.path("accepted")) {
                if (normalize(acc.asText()).equals(g)) return true;
            }
            return false;
        }
        if ("MATCH".equals(type)) {
            JsonNode pairs = answer.path("pairs");
            if (!given.isArray() || given.size() != pairs.size()) return false;
            for (int i = 0; i < pairs.size(); i++) {
                if (!pairs.get(i).path("right").asText().equals(given.get(i).asText())) return false;
            }
            return true;
        }
        return false;
    }

    /** Ignore spaces and a trailing semicolon; Java is case-sensitive, so case is kept. */
    static String normalize(String s) {
        String n = s.replaceAll("\\s+", "");
        if (n.endsWith(";")) n = n.substring(0, n.length() - 1);
        return n;
    }

    /** Percent with 2 decimals, e.g. 6 of 7 -> 85.71. */
    public static java.math.BigDecimal percent(int correct, int total) {
        if (total == 0) return java.math.BigDecimal.ZERO;
        return java.math.BigDecimal.valueOf(correct * 100.0 / total).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
