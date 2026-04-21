package com.noutusta.laptoprepair.controller;

import com.noutusta.laptoprepair.config.WebConfig;
import com.noutusta.laptoprepair.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LanguageController.class)
@Import(WebConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class LanguageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void switchLanguageSetsLocaleAndRedirectsBack() throws Exception {
        mockMvc.perform(get("/lang/ru").param("redirect", "/contact"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contact"))
                .andExpect(request().sessionAttribute(
                        SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME,
                        Locale.forLanguageTag("ru")
                ));
    }

    @Test
    void unsupportedLanguageFallsBackToDefaultLocale() throws Exception {
        mockMvc.perform(get("/lang/de").param("redirect", "/services"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/services"))
                .andExpect(request().sessionAttribute(
                        SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME,
                        Locale.forLanguageTag("uz")
                ));
    }

    @Test
    void externalRedirectIsBlocked() throws Exception {
        mockMvc.perform(get("/lang/en").param("redirect", "https://example.com/phishing"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(request().sessionAttribute(
                        SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME,
                        Locale.forLanguageTag("en")
                ));
    }
}
