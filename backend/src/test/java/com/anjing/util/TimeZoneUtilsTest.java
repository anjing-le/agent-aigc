package com.anjing.util;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeZoneUtilsTest {

    @Test
    void normalizeTimeZoneShouldUsePlatformDefaultWhenMissingOrInvalid() {
        assertEquals("UTC", TimeZoneUtils.normalizeTimeZone(null));
        assertEquals("UTC", TimeZoneUtils.normalizeTimeZone(""));
        assertEquals("UTC", TimeZoneUtils.normalizeTimeZone("not-a-zone"));
    }

    @Test
    void normalizeTimeZoneShouldKeepValidZoneId() {
        assertEquals("Asia/Shanghai", TimeZoneUtils.normalizeTimeZone("Asia/Shanghai"));
        assertEquals("Europe/Paris", TimeZoneUtils.normalizeTimeZone(" Europe/Paris "));
    }

    @Test
    void parseOrDefaultShouldReturnZoneId() {
        assertEquals(ZoneId.of("Asia/Shanghai"), TimeZoneUtils.parseOrDefault("Asia/Shanghai"));
        assertEquals(ZoneId.of("UTC"), TimeZoneUtils.parseOrDefault("bad-zone"));
    }

    @Test
    void defaultZoneShouldComeFromPlatformContract() {
        assertEquals("UTC", TimeZoneUtils.defaultTimeZoneId());
        assertSame(TimeZoneUtils.defaultZoneId(), DateUtils.UTC_ZONE);
        assertTrue(TimeZoneUtils.isValidTimeZone("UTC"));
        assertFalse(TimeZoneUtils.isValidTimeZone("bad-zone"));
    }
}
