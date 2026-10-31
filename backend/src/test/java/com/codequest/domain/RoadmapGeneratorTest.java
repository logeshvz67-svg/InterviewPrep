package com.codequest.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoadmapGeneratorTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    @Test
    void defaultPlanIs12CoreWeeksPlus4Later() {
        List<RoadmapWeek> weeks = RoadmapGenerator.generate(MONDAY, null, RoadmapGenerator.Track.SP);
        assertThat(weeks).hasSize(16);
        assertThat(weeks.stream().filter(w -> !w.laterLane())).hasSize(12);
        assertThat(weeks.get(0).phase()).startsWith("Phase 1");
        assertThat(weeks.get(4).phase()).startsWith("Phase 2");
        assertThat(weeks.get(9).phase()).startsWith("Phase 3");
        assertThat(weeks.get(12).laterLane()).isTrue();
        assertThat(weeks.get(1).startDate()).isEqualTo(MONDAY.plusWeeks(1));
    }

    @Test
    void shortTestDateSqueezesPhasesButKeepsAllThree() {
        List<RoadmapWeek> weeks = RoadmapGenerator.generate(MONDAY, MONDAY.plusWeeks(5), RoadmapGenerator.Track.DSE);
        List<RoadmapWeek> core = weeks.stream().filter(w -> !w.laterLane()).toList();
        assertThat(core).hasSize(5);
        assertThat(core).extracting(RoadmapWeek::phase).anyMatch(p -> p.startsWith("Phase 1"))
                .anyMatch(p -> p.startsWith("Phase 2")).anyMatch(p -> p.startsWith("Phase 3"));
        // Nothing is dropped: all Phase 1 Java content still appears somewhere.
        String allFocus = String.join(" ", core.stream().flatMap(w -> w.focus().stream()).toList());
        assertThat(allFocus).contains("section A").contains("section B").contains("section D").contains("section E");
    }

    @Test
    void veryCloseTestDateStillGivesMinimumWeeks() {
        var weeks = RoadmapGenerator.generate(MONDAY, MONDAY.plusDays(4), RoadmapGenerator.Track.SP);
        assertThat(weeks.stream().filter(w -> !w.laterLane())).hasSize(RoadmapGenerator.MIN_CORE_WEEKS);
    }

    @Test
    void longTestDateAddsReviewWeeks() {
        var weeks = RoadmapGenerator.generate(MONDAY, MONDAY.plusWeeks(24), RoadmapGenerator.Track.SP);
        assertThat(weeks.stream().filter(w -> !w.laterLane())).hasSize(24);
        assertThat(weeks).anyMatch(w -> w.title().startsWith("Review + practice"));
    }

    @Test
    void spAndDseDiffer() {
        var sp = RoadmapGenerator.generate(MONDAY, null, RoadmapGenerator.Track.SP);
        var dse = RoadmapGenerator.generate(MONDAY, null, RoadmapGenerator.Track.DSE);
        assertThat(sp.get(8).title()).contains("SP");
        assertThat(dse.get(8).title()).contains("DSE");
    }
}
