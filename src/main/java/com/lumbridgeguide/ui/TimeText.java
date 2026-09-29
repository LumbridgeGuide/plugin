package com.lumbridgeguide.ui;

import java.time.Duration;
import java.time.Instant;

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

    private TimeText() {
    }
}
