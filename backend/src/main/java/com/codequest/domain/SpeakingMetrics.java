package com.codequest.domain;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Speaking numbers computed from the transcript + duration. The browser shows them live, but the
 * server RE-COMPUTES them (never trust client-calculated scores).
 */
public final class SpeakingMetrics {
    private SpeakingMetrics() {}

    /** Filler words/phrases. Multi-word ones are matched first. */
    public static final List<String> FILLERS = List.of("you know", "i mean", "kind of", "sort of", "um", "uh", "umm", "uhh", "er", "ah",
            "like", "basically", "actually", "literally", "so yeah");

    public record Result(int words, int wpm, int fillerCount, Map<String, Integer> fillers, Map<String, Integer> scores, double overall) {}

    public static Result compute(String transcript, int durationSeconds, int longPauses, int timeLimitSeconds, String framework) {
        String text = transcript == null ? "" : transcript.toLowerCase(Locale.ROOT);
        int words = text.isBlank() ? 0 : text.trim().split("\\s+").length;
        int seconds = Math.max(1, durationSeconds);
        int wpm = (int) Math.round(words * 60.0 / seconds);

        Map<String, Integer> fillers = new LinkedHashMap<>();
        String scan = " " + text.replaceAll("[^a-z' ]", " ") + " ";
        int total = 0;
        for (String f : FILLERS) {
            Matcher m = Pattern.compile("(?<=\\s)" + Pattern.quote(f) + "(?=\\s)").matcher(scan);
            int n = 0;
            while (m.find()) n++;
            if (n > 0) {
                fillers.put(f, n);
                total += n;
                scan = scan.replaceAll("(?<=\\s)" + Pattern.quote(f) + "(?=\\s)", " ");
            }
        }

        // Local 0-10 scores (no AI needed).
        int pace = wpm >= 110 && wpm <= 160 ? 10 : wpm >= 90 && wpm <= 180 ? 7 : wpm >= 70 && wpm <= 200 ? 5 : 3;
        double fillerRate = words == 0 ? 1 : (double) total / words;
        int fillerScore = fillerRate <= 0.01 ? 10 : fillerRate <= 0.03 ? 8 : fillerRate <= 0.06 ? 6 : fillerRate <= 0.1 ? 4 : 2;
        int target = Math.max(30, timeLimitSeconds);
        double usage = (double) seconds / target;
        int length = usage >= 0.5 && usage <= 1.15 ? 10 : usage >= 0.3 && usage <= 1.4 ? 7 : 4;
        int pauses = longPauses <= 1 ? 10 : longPauses <= 3 ? 7 : longPauses <= 6 ? 5 : 3;
        int structure = structureScore(text, framework);

        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("pace", pace);
        scores.put("fillers", fillerScore);
        scores.put("length", length);
        scores.put("pauses", pauses);
        scores.put("structure", structure);
        double overall = words < 15 ? 20 : (pace * 0.2 + fillerScore * 0.25 + length * 0.15 + pauses * 0.15 + structure * 0.25) * 10;
        return new Result(words, wpm, total, fillers, scores, Math.round(overall * 10) / 10.0);
    }

    /** Rough check that the answer follows the taught framework (signal words). */
    static int structureScore(String t, String framework) {
        List<List<String>> parts = switch (framework == null ? "NONE" : framework) {
            case "STAR" -> List.of(List.of("situation", "when i", "once", "at my", "during"), List.of("task", "goal", "had to", "needed to", "responsible"),
                    List.of("i did", "i decided", "action", "so i", "i started", "i made"), List.of("result", "finally", "in the end", "learned", "outcome", "as a result"));
            case "PPF" -> List.of(List.of("currently", "right now", "at present", "i am", "i'm"), List.of("before", "previously", "graduated", "studied", "worked", "i was"),
                    List.of("future", "want to", "goal", "looking forward", "hope to", "aim"));
            case "CEU" -> List.of(List.of("is a", "means", "is when", "refers to", "simply"), List.of("for example", "example", "like when", "imagine", "suppose"),
                    List.of("use", "used", "useful", "in real", "in projects", "helps"));
            default -> List.of(List.of("first", "firstly", "to start"), List.of("second", "then", "next", "also"), List.of("finally", "so overall", "in short", "to sum"));
        };
        long hit = parts.stream().filter(p -> p.stream().anyMatch(t::contains)).count();
        return (int) Math.round(3 + 7.0 * hit / parts.size());
    }
}
