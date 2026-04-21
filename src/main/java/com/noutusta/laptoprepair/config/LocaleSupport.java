package com.noutusta.laptoprepair.config;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class LocaleSupport {

    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("uz");
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("uz", "ru", "en");

    private LocaleSupport() {
    }

    public static Locale defaultLocale() {
        return DEFAULT_LOCALE;
    }

    public static Optional<Locale> resolve(String rawLanguage) {
        if (rawLanguage == null || rawLanguage.isBlank()) {
            return Optional.empty();
        }

        Locale candidate = Locale.forLanguageTag(rawLanguage.trim().replace('_', '-'));
        String language = candidate.getLanguage();
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            return Optional.empty();
        }

        return Optional.of(Locale.forLanguageTag(language));
    }
}
