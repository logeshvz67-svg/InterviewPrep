package com.codequest.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the week-by-week plan. Pure logic, no database.
 *
 * Default (no test date) = 12 core weeks + 4 "Later lane" weeks:
 *   Phase 1 (weeks 1-4)  Java A-E + aptitude basics + SQL basics review
 *   Phase 2 (weeks 5-9)  DSA patterns, Java 8 + threads, SQL advanced, CS fundamentals
 *   Phase 3 (weeks 10-12) Infosys-style timed tests, mocks, communication
 *   Later   (weeks 13+)  other companies + Spring project
 * With a test date, the three core phases are squeezed or stretched (4:5:3 ratio) to end
 * the week before the test, and the Later lane starts after it.
 */
public final class RoadmapGenerator {
    public static final int DEFAULT_CORE_WEEKS = 12;
    public static final int LATER_WEEKS = 4;
    public static final int MIN_CORE_WEEKS = 3;
    public static final int MAX_CORE_WEEKS = 30;

    private RoadmapGenerator() {}

    public enum Track { SP, DSE }

    private record Chunk(String title, List<String> focus) {}

    public static List<RoadmapWeek> generate(LocalDate start, LocalDate testDate, Track track) {
        int core = DEFAULT_CORE_WEEKS;
        if (testDate != null && testDate.isAfter(start)) {
            long weeks = ChronoUnit.WEEKS.between(start, testDate);
            core = (int) Math.max(MIN_CORE_WEEKS, Math.min(MAX_CORE_WEEKS, weeks));
        }
        // Split core weeks 4:5:3 (rounded), every phase gets at least 1 week.
        int p1 = Math.max(1, Math.round(core * 4f / 12));
        int p3 = Math.max(1, Math.round(core * 3f / 12));
        int p2 = Math.max(1, core - p1 - p3);

        List<RoadmapWeek> out = new ArrayList<>();
        int week = 1;
        week = addPhase(out, week, start, "Phase 1: Java foundations", phase1(track), p1, false);
        week = addPhase(out, week, start, "Phase 2: DSA and depth", phase2(track), p2, false);
        week = addPhase(out, week, start, "Phase 3: Test and interview mode", phase3(track), p3, false);
        addPhase(out, week, start, "Later lane: other companies + Spring", later(), LATER_WEEKS, true);
        return out;
    }

    /** Spread the chunks evenly over the given number of weeks. Extra weeks become review weeks. */
    private static int addPhase(List<RoadmapWeek> out, int firstWeek, LocalDate start, String phase,
                                List<Chunk> chunks, int weeks, boolean later) {
        int n = chunks.size();
        for (int i = 0; i < weeks; i++) {
            int from = (int) Math.floor((double) i * n / weeks);
            int to = (int) Math.floor((double) (i + 1) * n / weeks);
            List<String> focus = new ArrayList<>();
            List<String> titles = new ArrayList<>();
            if (from >= to) {
                // More weeks than chunks: revisit the chunk this week falls in.
                Chunk c = chunks.get(Math.min(n - 1, from));
                titles.add("Review + practice: " + c.title());
                focus.add("Re-solve revisit-queue problems for: " + c.title());
                focus.add("Explain each topic out loud in 3 sentences");
            } else {
                for (int c = from; c < to; c++) {
                    titles.add(chunks.get(c).title());
                    focus.addAll(chunks.get(c).focus());
                }
            }
            int weekNo = firstWeek + i;
            out.add(new RoadmapWeek(weekNo, start.plusWeeks(weekNo - 1L), phase, String.join(" + ", titles), focus, later));
        }
        return firstWeek + weeks;
    }

    private static List<Chunk> phase1(Track t) {
        return List.of(
                new Chunk("Java A: Foundations", List.of("Java section A: how Java works, types, operators, loops, methods, arrays",
                        "Aptitude: percentages, ratios, averages", "Type every example yourself")),
                new Chunk("Java B + OOP part 1", List.of("Java section B: Strings and the String pool",
                        "Java section C: classes, constructors, static, inheritance", "Logical reasoning: series and coding-decoding")),
                new Chunk("OOP part 2 + Exceptions + SQL basics", List.of("Java section C: polymorphism, interfaces, Object class, immutability",
                        "Java section D: exceptions", "SQL basics review: SELECT, WHERE, JOIN, GROUP BY")),
                new Chunk("Generics + Collections", List.of("Java section E: generics, List/Set/Map, HashMap internals",
                        t == Track.SP ? "Solve 8-10 easy array/string problems and log them" : "Solve 5-6 easy array/string problems and log them",
                        "Aptitude: time and work, speed and distance")));
    }

    private static List<Chunk> phase2(Track t) {
        List<Chunk> c = new ArrayList<>();
        c.add(new Chunk("Arrays, strings, hashing patterns", List.of("Two pointers, sliding window, prefix sum, hashing", "Mistake notebook review")));
        c.add(new Chunk("Java 8 + recursion", List.of("Java section F: lambdas and streams", "Recursion and backtracking basics")));
        c.add(new Chunk("Linked list, stack, queue + SQL advanced", List.of("Linked list and stack/queue patterns", "SQL: subqueries, window functions, Nth highest salary")));
        c.add(new Chunk("Multithreading + sorting/searching", List.of("Java section G: threads, synchronized, executors", "Sorting and binary search patterns")));
        if (t == Track.SP) {
            c.add(new Chunk("Trees, graphs, DP basics (SP depth)", List.of("Trees/BST, BFS/DFS, intro DP", "CS fundamentals: OS + DBMS basics", "Optimization: always state Big-O")));
        } else {
            c.add(new Chunk("Trees basics + CS fundamentals (DSE)", List.of("Trees basics, heaps intro", "CS fundamentals: OS + DBMS + networking basics", "MCQ practice on Java/OOP/SQL")));
        }
        return c;
    }

    private static List<Chunk> phase3(Track t) {
        return List.of(
                new Chunk("Timed coding tests", List.of(t == Track.SP
                        ? "Infosys SP-style timed multi-problem sessions (use your Exam pattern settings)"
                        : "Infosys DSE-style MCQ + 1-2 coding problems (use your Exam pattern settings)", "Java sections H-J: JVM, design basics, interview layer")),
                new Chunk("Mock interviews", List.of("Technical mock interview boss fight", "Project explanation practice (Concept -> Example -> Use case)")),
                new Chunk("Communication + HR", List.of("Self-introduction (Present -> Past -> Future)", "HR answers with STAR", "Verbal/English practice")));
    }

    private static List<Chunk> later() {
        return List.of(
                new Chunk("TCS / Wipro / Cognizant packs", List.of("Company-specific practice packs", "Spring Boot: IoC, beans, REST")),
                new Chunk("Accenture / Capgemini packs", List.of("Company-specific practice packs", "Spring Boot: JPA and validation")),
                new Chunk("Product company prep", List.of("Medium-level DSA", "Spring Security + JWT project milestone")),
                new Chunk("Capstone", List.of("Polish your Spring project and README", "Full mock interview")));
    }
}
