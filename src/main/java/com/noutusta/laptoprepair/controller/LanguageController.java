package com.noutusta.laptoprepair.controller;

import com.noutusta.laptoprepair.config.LocaleSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

@Controller
public class LanguageController {

    private final LocaleResolver localeResolver;

    public LanguageController(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @GetMapping("/lang/{languageCode}")
    public String switchLanguage(@PathVariable String languageCode,
                                 @RequestParam(value = "redirect", required = false) String redirect,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        localeResolver.setLocale(
                request,
                response,
                LocaleSupport.resolve(languageCode).orElse(LocaleSupport.defaultLocale())
        );
        return "redirect:" + sanitizeRedirectPath(redirect);
    }

    private String sanitizeRedirectPath(String redirect) {
        if (redirect == null || redirect.isBlank()) {
            return "/";
        }

        String trimmed = redirect.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return "/";
        }
        if (trimmed.contains("\r") || trimmed.contains("\n") || trimmed.contains("\\")) {
            return "/";
        }

        return trimmed;
    }
}
