package com.noutusta.laptoprepair.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public Object handle404(Exception ex, HttpServletRequest request, Locale locale) {
        log.warn("event=http_404 path={} exception={}", request.getRequestURI(), ex.getClass().getSimpleName());

        if (isApiRequest(request)) {
            return apiError(HttpStatus.NOT_FOUND, message("error.api.not-found", locale));
        }

        return errorView("error/404", HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex,
                                   HttpServletRequest request,
                                   Locale locale) {
        String validationMessage = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(message("error.api.validation", locale));

        if (isApiRequest(request)) {
            return apiError(HttpStatus.BAD_REQUEST, validationMessage);
        }

        ModelAndView modelAndView = errorView("error/500", HttpStatus.BAD_REQUEST, request);
        modelAndView.addObject("errorMessage", validationMessage);
        return modelAndView;
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex, HttpServletRequest request, Locale locale) {
        log.error("event=unhandled_error path={} message={}", request.getRequestURI(), ex.getMessage(), ex);

        if (isApiRequest(request)) {
            return apiError(HttpStatus.INTERNAL_SERVER_ERROR, message("error.api.internal", locale));
        }

        return errorView("error/500", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<Map<String, Object>> apiError(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "error", message,
                        "timestamp", Instant.now().toString()
                ));
    }

    private ModelAndView errorView(String viewName, HttpStatus status, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(viewName);
        modelAndView.setStatus(status);
        modelAndView.addObject("requestPath", request.getRequestURI());
        return modelAndView;
    }

    private String message(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null
                && accept.contains("application/json")
                && !accept.contains("text/html");
    }
}
