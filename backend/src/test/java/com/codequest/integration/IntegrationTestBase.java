package com.codequest.integration;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.*;

/**
 * Starts a REAL Postgres (embedded binary, no Docker) once for all integration tests,
 * runs the Flyway migrations on it, and gives tests a clock they can move.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(IntegrationTestBase.TestClockConfig.class)
public abstract class IntegrationTestBase {
    static final EmbeddedPostgres PG;

    static {
        try {
            PG = EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> PG.getJdbcUrl("postgres", "postgres"));
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "");
        // Tests jump the clock forward by hours; keep access tokens valid across those jumps.
        r.add("app.jwt.access-token-minutes", () -> "100000");
    }

    /** A Clock we can set to any moment, to test midnight and timezone behaviour. */
    public static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-05T06:00:00Z");
        public void set(Instant i) { now = i; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() { return new MutableClock(); }
    }
}
