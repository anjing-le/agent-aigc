package com.anjing.util;

import com.anjing.model.constants.PlatformContractConstants;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Time zone helper based on the platform contract.
 */
public final class TimeZoneUtils {

    private static final String DEFAULT_TIME_ZONE = PlatformContractConstants.Time.DEFAULT_TIME_ZONE;
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of(DEFAULT_TIME_ZONE);

    private TimeZoneUtils() {
    }

    public static String defaultTimeZoneId() {
        return DEFAULT_ZONE_ID.getId();
    }

    public static ZoneId defaultZoneId() {
        return DEFAULT_ZONE_ID;
    }

    public static boolean isValidTimeZone(String timeZone) {
        if (timeZone == null || timeZone.trim().isEmpty()) {
            return false;
        }

        try {
            ZoneId.of(timeZone.trim());
            return true;
        } catch (DateTimeException ignored) {
            return false;
        }
    }

    public static ZoneId parseOrDefault(String timeZone) {
        return ZoneId.of(normalizeTimeZone(timeZone));
    }

    public static String normalizeTimeZone(String timeZone) {
        if (timeZone == null || timeZone.trim().isEmpty()) {
            return defaultTimeZoneId();
        }

        try {
            return ZoneId.of(timeZone.trim()).getId();
        } catch (DateTimeException ignored) {
            return defaultTimeZoneId();
        }
    }
}
