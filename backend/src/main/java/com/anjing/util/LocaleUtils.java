package com.anjing.util;

import com.anjing.model.constants.PlatformContractConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Locale helper based on the platform contract.
 */
public final class LocaleUtils {

    private static final String DEFAULT_LOCALE = PlatformContractConstants.Locale.DEFAULT_LOCALE;
    private static final List<Locale> SUPPORTED_LOCALES = Arrays.stream(PlatformContractConstants.Locale.SUPPORTED_LOCALES)
            .map(Locale::forLanguageTag)
            .toList();
    private static final List<String> SUPPORTED_LOCALE_TAGS = SUPPORTED_LOCALES.stream()
            .map(Locale::toLanguageTag)
            .toList();

    private LocaleUtils() {
    }

    public static String defaultLocaleTag() {
        return DEFAULT_LOCALE;
    }

    public static List<String> supportedLocaleTags() {
        return SUPPORTED_LOCALE_TAGS;
    }

    public static boolean isSupportedLocale(String localeTag) {
        if (localeTag == null || localeTag.trim().isEmpty()) {
            return false;
        }
        return SUPPORTED_LOCALE_TAGS.contains(Locale.forLanguageTag(localeTag.trim()).toLanguageTag());
    }

    public static String normalizeAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.trim().isEmpty()) {
            return DEFAULT_LOCALE;
        }

        try {
            Locale matched = Locale.lookup(Locale.LanguageRange.parse(acceptLanguage), SUPPORTED_LOCALES);
            if (matched != null) {
                return matched.toLanguageTag();
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to direct tag parsing below.
        }

        Locale locale = Locale.forLanguageTag(acceptLanguage.trim());
        if (locale.getLanguage().isEmpty()) {
            return DEFAULT_LOCALE;
        }

        String normalizedTag = locale.toLanguageTag();
        if (SUPPORTED_LOCALE_TAGS.contains(normalizedTag)) {
            return normalizedTag;
        }

        for (Locale supportedLocale : SUPPORTED_LOCALES) {
            if (supportedLocale.getLanguage().equals(locale.getLanguage())) {
                return supportedLocale.toLanguageTag();
            }
        }

        return DEFAULT_LOCALE;
    }
}
