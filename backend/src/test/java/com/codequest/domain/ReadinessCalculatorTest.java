package com.codequest.domain;

import com.codequest.domain.ReadinessCalculator.Component;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReadinessCalculatorTest {

    @Test
    void weightsAddUpTo100Percent() {
        for (String c : new String[]{"INFOSYS_SP", "INFOSYS_DSE", "TCS", "PRODUCT"}) {
            BigDecimal sum = ReadinessCalculator.weightsFor(c).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo("1.00");
        }
    }

    @Test
    void untrackedComponentsCountAsZeroAndAreReported() {
        Map<Component, BigDecimal> v = new EnumMap<>(Component.class);
        v.put(Component.JAVA, new BigDecimal("0.5"));
        v.put(Component.DSA, new BigDecimal("0.5"));
        var r = ReadinessCalculator.compute("INFOSYS_SP", v);
        // SP: Java 0.20*0.5 + DSA 0.30*0.5 = 0.25 -> 25.0
        assertThat(r.score()).isEqualByComparingTo("25.0");
        assertThat(r.trackedWeight()).isEqualByComparingTo("50");
        assertThat(r.improveNext()).isEqualTo(Component.DSA); // biggest missing points among tracked
    }

    @Test
    void valuesAreClampedTo0to1() {
        Map<Component, BigDecimal> v = new EnumMap<>(Component.class);
        v.put(Component.JAVA, new BigDecimal("7"));
        v.put(Component.DSA, new BigDecimal("-3"));
        var r = ReadinessCalculator.compute("INFOSYS_DSE", v);
        assertThat(r.score()).isEqualByComparingTo("25.0"); // DSE Java weight 0.25 * 1
    }
}
