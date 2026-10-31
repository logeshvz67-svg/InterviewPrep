package com.codequest.service;

import com.codequest.config.AppProperties;
import com.codequest.repository.ProfileRepository;
import org.springframework.stereotype.Service;

import java.time.*;

/**
 * Converts "now" (UTC) into the user's local date/time.
 * WHY: a streak day must end at the user's midnight (e.g. India, UTC+5:30), not at UTC midnight,
 * otherwise studying at 11 pm IST would count for the wrong day.
 */
@Service
public class TimeService {
    private final Clock clock;
    private final ProfileRepository profiles;
    private final ZoneId defaultZone;

    public TimeService(Clock clock, ProfileRepository profiles, AppProperties props) {
        this.clock = clock;
        this.profiles = profiles;
        this.defaultZone = ZoneId.of(props.defaultTimezone());
    }

    public Instant now() { return clock.instant(); }

    public ZoneId zoneOf(Long userId) {
        return profiles.findByUserId(userId).map(p -> safeZone(p.getTimezone())).orElse(defaultZone);
    }

    public LocalDate today(Long userId) { return LocalDate.ofInstant(now(), zoneOf(userId)); }

    public ZonedDateTime localNow(Long userId) { return now().atZone(zoneOf(userId)); }

    public ZoneId safeZone(String id) {
        try {
            return ZoneId.of(id);
        } catch (DateTimeException e) {
            return defaultZone;
        }
    }

    public static boolean isValidZone(String id) {
        try {
            ZoneId.of(id);
            return true;
        } catch (DateTimeException | NullPointerException e) {
            return false;
        }
    }
}
