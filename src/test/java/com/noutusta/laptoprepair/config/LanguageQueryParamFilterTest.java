package com.noutusta.laptoprepair.config;

import com.noutusta.laptoprepair.controller.WebController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LanguageQueryParamFilterTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(LocaleSupport.defaultLocale());

        mockMvc = MockMvcBuilders
                .standaloneSetup(new WebController())
                .addFilters(new LanguageQueryParamFilter(localeResolver))
                .build();
    }

    @Test
    void langParamIsNormalizedOutOfUrl() throws Exception {
        mockMvc.perform(get("/services").param("lang", "ru"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/services"))
                .andExpect(request().sessionAttribute(
                        SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME,
                        Locale.forLanguageTag("ru")
                ));
    }

    @Test
    void unsupportedLangFallsBackToDefaultLocale() throws Exception {
        mockMvc.perform(get("/contact").param("lang", "de"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contact"))
                .andExpect(request().sessionAttribute(
                        SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME,
                        Locale.forLanguageTag("uz")
                ));
    }
}
