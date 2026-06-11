package com.anjing.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocaleUtilsTest {

    @Test
    void normalizeAcceptLanguageShouldUsePlatformDefaultWhenMissingOrInvalid() {
        assertEquals("zh-CN", LocaleUtils.normalizeAcceptLanguage(null));
        assertEquals("zh-CN", LocaleUtils.normalizeAcceptLanguage(""));
        assertEquals("zh-CN", LocaleUtils.normalizeAcceptLanguage("not a valid language range"));
    }

    @Test
    void normalizeAcceptLanguageShouldChooseSupportedLocaleByWeightAndLanguage() {
        assertEquals("en-US", LocaleUtils.normalizeAcceptLanguage("fr-FR, en-US;q=0.9, zh-CN;q=0.5"));
        assertEquals("zh-CN", LocaleUtils.normalizeAcceptLanguage("zh"));
        assertEquals("en-US", LocaleUtils.normalizeAcceptLanguage("en-GB"));
    }

    @Test
    void supportedLocaleTagsShouldComeFromPlatformContract() {
        assertEquals(List.of("zh-CN", "en-US"), LocaleUtils.supportedLocaleTags());
        assertTrue(LocaleUtils.isSupportedLocale("zh-CN"));
        assertTrue(LocaleUtils.isSupportedLocale("en-US"));
        assertFalse(LocaleUtils.isSupportedLocale("de-DE"));
    }
}
