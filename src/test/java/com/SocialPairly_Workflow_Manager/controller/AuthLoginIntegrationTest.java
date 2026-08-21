package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.security.AuthCookieService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filters-on HTTP integration coverage for password login (US-008 / DEV-009).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthLoginIntegrationTest {

    private static final String PASSWORD = "LoginIt@12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String email;

    @BeforeEach
    void seedUser() {
        email = "login.it." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        User user = new User();
        user.setFirstName("Login");
        user.setLastName("Tester");
        user.setEmail(email);
        user.setPhoneNumber("+1555" + String.format("%07d", Math.floorMod(Math.abs(email.hashCode()), 10_000_000)));
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setAddress("Test");
        user.setRole(Role.USER);
        user.setProfileCompleted(false);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Test
    @DisplayName("POST /api/auth/login success returns JWT, user without password, and SP_AUTH cookie")
    void loginSuccessReturnsTokenCookieAndSafeUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s","rememberMe":false}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies)
                .isNotEmpty()
                .anyMatch(c -> c.startsWith(AuthCookieService.DEFAULT_COOKIE_NAME + "=")
                        && !c.contains("Max-Age=0"));

        String body = result.getResponse().getContentAsString();
        assertThat(body.toLowerCase()).doesNotContain(PASSWORD.toLowerCase());
        assertThat(body).doesNotContain("\"password\"");

        JsonNode json = objectMapper.readTree(body);
        String token = json.get("token").asText();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/login failure returns 401 and does not set SP_AUTH cookie")
    void loginFailureDoesNotIssueSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"WrongPassword!1","rememberMe":false}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies == null ? List.of() : setCookies)
                .noneMatch(c -> c.startsWith(AuthCookieService.DEFAULT_COOKIE_NAME + "=")
                        && !c.contains("Max-Age=0"));
        assertThat(result.getResponse().getContentAsString()).doesNotContain("token");
    }

    @Test
    @DisplayName("Duplicate successful logins each return a consistent session for the same user")
    void duplicateSuccessfulLoginsRemainConsistent() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s","rememberMe":true}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s","rememberMe":true}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();

        String token1 = objectMapper.readTree(first.getResponse().getContentAsString()).get("token").asText();
        String token2 = objectMapper.readTree(second.getResponse().getContentAsString()).get("token").asText();
        assertThat(token1).isNotBlank();
        assertThat(token2).isNotBlank();

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }
}
