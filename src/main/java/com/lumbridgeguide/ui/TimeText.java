package com.lumbridgeguide.ui;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Short relative times, such as "12m ago" or "3w ago". */
public final class TimeText {

    public static String ago(String isoInstant) {
        try {
            Duration age = Duration.between(Instant.parse(isoInstant), Instant.now());
            if (age.toDays() >= 30) {
                return age.toDays() / 30 + "mo ago";
            }
            if (age.toDays() >= 7) {
                return age.toDays() / 7 + "w ago";
            }
            if (age.toDays() >= 1) {
                return age.toDays() + "d ago";
            }
            if (age.toHours() >= 1) {
                return age.toHours() + "h ago";
            }
            if (age.toMinutes() >= 1) {
                return age.toMinutes() + "m ago";
            }
            return "just now";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    /**
     * When something happened, as on the website's claim list: the time for today, the weekday for this week, and the
     * date before that.
     */
    public static String clock(String isoInstant) {
        try {
            ZonedDateTime when = Instant.parse(isoInstant).atZone(ZoneId.systemDefault());
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            if (when.toLocalDate().equals(today)) {
                return when.format(DateTimeFormatter.ofPattern("HH:mm"));
            }
            if (when.toLocalDate().isAfter(today.minusDays(7))) {
                return when.format(DateTimeFormatter.ofPattern("EEE", Locale.UK));
            }
            return when.format(DateTimeFormatter.ofPattern("d MMM", Locale.UK));
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private TimeText() {
    }
}
