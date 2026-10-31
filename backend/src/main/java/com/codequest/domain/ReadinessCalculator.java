package com.codequest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Readiness score (0-100) for a target company = weighted average of components.
 * A component that isn't tracked yet (null) counts as 0, and we say so openly,
 * so the score never pretends to know more than it does.
 * Weights are our prep judgment, not an official formula.
 */
public final class ReadinessCalculator {
    private ReadinessCalculator() {}

    public enum Component { JAVA, DSA, SQL, APTITUDE, MOCKS, COMMUNICATION }

    public record ComponentScore(Component component, BigDecimal weight, BigDecimal value, boolean tracked) {}

    public record Result(BigDecimal score, BigDecimal trackedWeight, List<ComponentScore> components, Component improveNext) {}

    public static Map<Component, BigDecimal> weightsFor(String companyCode) {
        Map<Component, BigDecimal> w = new LinkedHashMap<>();
        boolean dsaHeavy = "INFOSYS_SP".equals(companyCode) || "PRODUCT".equals(companyCode);
        w.put(Component.JAVA, bd(dsaHeavy ? "0.20" : "0.25"));
        w.put(Component.DSA, bd(dsaHeavy ? "0.30" : "0.20"));
        w.put(Component.SQL, bd(dsaHeavy ? "0.10" : "0.15"));
        w.put(Component.APTITUDE, bd("0.15"));
        w.put(Component.MOCKS, bd("0.15"));
        w.put(Component.COMMUNICATION, bd("0.10"));
        return w;
    }

    /** values: 0..1 per component, or null when not tracked yet. */
    public static Result compute(String companyCode, Map<Component, BigDecimal> values) {
        Map<Component, BigDecimal> weights = weightsFor(companyCode);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal trackedWeight = BigDecimal.ZERO;
        List<ComponentScore> parts = new ArrayList<>();
        for (var e : weights.entrySet()) {
            BigDecimal v = values.get(e.getKey());
            boolean tracked = v != null;
            BigDecimal clamped = tracked ? v.max(BigDecimal.ZERO).min(BigDecimal.ONE) : BigDecimal.ZERO;
            total = total.add(e.getValue().multiply(clamped));
            if (tracked) trackedWeight = trackedWeight.add(e.getValue());
            parts.add(new ComponentScore(e.getKey(), e.getValue(), clamped.setScale(2, RoundingMode.HALF_UP), tracked));
        }
        // "Improve next" = tracked component with the biggest missing points (weight * gap).
        Component next = parts.stream().filter(ComponentScore::tracked)
                .max(Comparator.comparing(p -> p.weight().multiply(BigDecimal.ONE.subtract(p.value()))))
                .map(ComponentScore::component).orElse(Component.JAVA);
        return new Result(total.multiply(bd("100")).setScale(1, RoundingMode.HALF_UP),
                trackedWeight.multiply(bd("100")).setScale(0, RoundingMode.HALF_UP), parts, next);
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }
}
