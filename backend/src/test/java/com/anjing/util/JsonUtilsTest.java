package com.anjing.util;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilsTest {

    @Test
    void staticJsonHelperShouldWorkWithoutSpringContext() {
        String json = JsonUtils.toJson(Map.of(
                "name", "alpha",
                "time", OffsetDateTime.parse("2026-06-06T00:00:00Z")
        ));

        assertTrue(json.contains("\"name\":\"alpha\""));
        assertTrue(json.contains("\"time\":\"2026-06-06T00:00:00Z\""));
    }
}
