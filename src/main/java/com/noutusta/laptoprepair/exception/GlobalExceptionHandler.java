package com.noutusta.laptoprepair.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * FIX C: NoHandlerFoundException + NoResourceFoundException birlashtirildi.
     * /api/** yoki Accept: application/json  → JSON
     * Boshqa barcha so'rovlar (browser)       → Thymeleaf error/404 view
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public Object handle404(Exception ex, HttpServletRequest request) {
        if (ex instanceof NoResourceFoundException nrfe) {
            log.warn("event=resource_not_found path={}", nrfe.getResourcePath());
        } else {
            log.warn("event=no_handler_found path={}", request.getRequestURI());
        }

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "error", "So'ralgan resurs topilmadi.",
                            "timestamp", Instant.now().toString()
                    ));
        }

        // Browser → Thymeleaf 404 sahifasi
        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(e -> e.getDefaultMessage())
                .orElse("Ma'lumot tekshiruvi xatosi");
        return ResponseEntity.badRequest().body(Map.of(
                "error", message,
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("event=unhandled_error message={}", ex.getMessage(), ex);

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "error", "Kutilmagan server xatosi. Qayta urinib ko'ring.",
                            "timestamp", Instant.now().toString()
                    ));
        }

        // Browser → generic error view (agar mavjud bo'lsa, yo'q bo'lsa 500.html)
        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }

    // /api/ path yoki Accept: application/json (text/html ustunligi yo'q)
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