package com.SocialPairly_Workflow_Manager.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTests {

    @Test
    void customExceptionsShouldCarryMessages() {
        BadRequestException badRequest = new BadRequestException("bad input");
        ResourceNotFoundException notFound = new ResourceNotFoundException("missing resource");

        assertEquals("bad input", badRequest.getMessage());
        assertEquals("missing resource", notFound.getMessage());
    }

    @Test
    void globalExceptionHandlerShouldTranslateKnownExceptions() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, Object>> badRequest = handler.handleBadRequest(new BadRequestException("bad input"));
        ResponseEntity<Map<String, Object>> notFound = handler.handleNotFound(new ResourceNotFoundException("missing resource"));
        ResponseEntity<Map<String, Object>> unauthorized = handler.handleBadCredentials(new BadCredentialsException("bad"));
        ResponseEntity<Map<String, Object>> generic = handler.handleGeneric(new RuntimeException("boom"));

        assertEquals(HttpStatus.BAD_REQUEST, badRequest.getStatusCode());
        assertEquals("bad input", badRequest.getBody().get("message"));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, unauthorized.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, generic.getStatusCode());
    }
}
