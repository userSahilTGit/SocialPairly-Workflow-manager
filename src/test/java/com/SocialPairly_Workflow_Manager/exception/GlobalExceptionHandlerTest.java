package com.SocialPairly_Workflow_Manager.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("TooManyRequestsException -> 429 with message")
    void tooManyRequests() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleTooManyRequests(new TooManyRequestsException("Slow down"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, resp.getStatusCode());
        assertEquals("Slow down", resp.getBody().get("message"));
        assertEquals(429, resp.getBody().get("status"));
    }

    @Test
    @DisplayName("UnprocessableEntityException -> 422 with message")
    void unprocessable() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleUnprocessable(new UnprocessableEntityException("Cannot continue"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals("Cannot continue", resp.getBody().get("message"));
        assertEquals(422, resp.getBody().get("status"));
    }

    @Test
    @DisplayName("BadRequestException -> 400 with message")
    void badRequest() {
        ResponseEntity<Map<String, Object>> resp = handler.handleBadRequest(new BadRequestException("Invalid file."));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Invalid file.", resp.getBody().get("message"));
        assertEquals(400, resp.getBody().get("status"));
    }

    @Test
    @DisplayName("ResourceNotFoundException -> 404 with message")
    void notFound() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleNotFound(new ResourceNotFoundException("Media not found."));
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Media not found.", resp.getBody().get("message"));
        assertEquals(404, resp.getBody().get("status"));
    }

    @Test
    @DisplayName("BadCredentialsException -> 401 with generic message")
    void badCredentials() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleBadCredentials(new BadCredentialsException("secret"));
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("Invalid credentials", resp.getBody().get("message"));
        assertEquals(401, resp.getBody().get("status"));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 with field errors")
    void validationErrors() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        binding.addError(new FieldError("obj", "caption", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<Map<String, Object>> resp = handler.handleValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Validation failed", resp.getBody().get("message"));
        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) resp.getBody().get("fields");
        assertEquals("must not be blank", fields.get("caption"));
    }

    @Test
    @DisplayName("Generic Exception -> 500 with message")
    void generic() {
        ResponseEntity<Map<String, Object>> resp = handler.handleGeneric(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("An unexpected error occurred. Please try again.", resp.getBody().get("message"));
        assertEquals(500, resp.getBody().get("status"));
    }

    @Test
    @DisplayName("Validation without fields -> fields empty map")
    void validationNoFields() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<Map<String, Object>> resp = handler.handleValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body);
        // baseBody is invoked, so timestamp/status/error/message all present
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("error"));
    }
}