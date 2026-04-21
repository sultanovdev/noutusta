package com.noutusta.laptoprepair.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class LanguageQueryParamFilter extends OncePerRequestFilter {

    private final LocaleResolver localeResolver;

    public LanguageQueryParamFilter(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String language = request.getParameter("lang");
        if (language == null || language.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        localeResolver.setLocale(
                request,
                response,
                LocaleSupport.resolve(language).orElse(LocaleSupport.defaultLocale())
        );

        if (shouldNormalizeUrl(request)) {
            response.sendRedirect(buildUrlWithoutLang(request));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldNormalizeUrl(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod()) && !isApiRequest(request);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/api/");
    }

    private String buildUrlWithoutLang(HttpServletRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(request.getRequestURI());

        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if ("lang".equals(entry.getKey())) {
                continue;
            }
            String[] values = entry.getValue();
            if (values == null || values.length == 0) {
                builder.queryParam(entry.getKey());
                continue;
            }
            for (String value : values) {
                builder.queryParam(entry.getKey(), value);
            }
        }

        String normalizedUrl = builder.build().toUriString();
        return normalizedUrl.isBlank() ? "/" : normalizedUrl;
    }
}
