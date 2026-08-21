package com.SocialPairly_Workflow_Manager.controller;

import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.security.AuthCookieService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filters-on HTTP integration coverage for Google Sign-In (US-009 / DEV-008).
 * Google ID-token cryptographic verification is stubbed; session creation, user
 * link/create, cookies, and authenticated follow-up calls use the real stack.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthGoogleSignInIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Test
    @DisplayName("POST /api/auth/google links existing user, returns JWT + SP_AUTH, and authenticates /users/me")
    void googleSignInLinksExistingUserAndCreatesSession() throws Exception {
        String email = "google.it.existing." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        seedPasswordUser(email, "Existing", "User");

        stubValidGoogleToken("valid-existing-token", email, "Existing", "User", true);

        MvcResult result = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"valid-existing-token","rememberMe":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.firstName").value("Existing"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn();

        assertAuthCookieIssued(result);

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(userRepository.findByEmail(email)).isPresent();
        assertThat(userRepository.findAll().stream().filter(u -> email.equalsIgnoreCase(u.getEmail())).count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/auth/google creates a new user when email is unknown")
    void googleSignInCreatesNewUser() throws Exception {
        String email = "google.it.new." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        assertThat(userRepository.findByEmail(email)).isEmpty();

        stubValidGoogleToken("valid-new-token", email, "Nova", "Account", true);

        MvcResult result = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"valid-new-token","rememberMe":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.firstName").value("Nova"))
                .andExpect(jsonPath("$.user.lastName").value("Account"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn();

        assertAuthCookieIssued(result);

        User created = userRepository.findByEmail(email).orElseThrow();
        assertThat(created.getFirstName()).isEqualTo("Nova");
        assertThat(created.getLastName()).isEqualTo("Account");
        assertThat(created.getPhoneNumber()).startsWith("oauth:");
        assertThat(created.isEmailVerified()).isTrue();
        assertThat(created.isProfileCompleted()).isFalse();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("POST /api/auth/google with invalid token returns 401 and does not set SP_AUTH")
    void googleSignInRejectsInvalidToken() throws Exception {
        when(googleIdTokenVerifier.verify("invalid-google-token")).thenReturn(null);

        MvcResult result = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"invalid-google-token","rememberMe":false}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid Google Identity Token."))
                .andReturn();

        assertNoAuthCookie(result);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("\"token\"");
    }

    @Test
    @DisplayName("POST /api/auth/google with missing token returns 400 and does not set SP_AUTH")
    void googleSignInRejectsMissingToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"  ","rememberMe":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing Google identity token"))
                .andReturn();

        assertNoAuthCookie(result);
    }

    @Test
    @DisplayName("Google session can be invalidated via POST /api/auth/logout")
    void googleSignInSessionInvalidatedOnLogout() throws Exception {
        String email = "google.it.logout." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        stubValidGoogleToken("logout-google-token", email, "Log", "Out", true);

        MvcResult login = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"logout-google-token","rememberMe":false}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = body.get("token").asText();

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private void seedPasswordUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhoneNumber("+1555" + String.format("%07d", Math.floorMod(Math.abs(email.hashCode()), 10_000_000)));
        user.setPassword(passwordEncoder.encode("GoogleIt@12345"));
        user.setAddress("Test");
        user.setRole(Role.USER);
        user.setProfileCompleted(false);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    private void stubValidGoogleToken(
            String rawToken,
            String email,
            String givenName,
            String familyName,
            boolean emailVerified
    ) throws Exception {
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(googleIdTokenVerifier.verify(rawToken)).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getEmail()).thenReturn(email);
        when(payload.get("given_name")).thenReturn(givenName);
        when(payload.get("family_name")).thenReturn(familyName);
        when(payload.getEmailVerified()).thenReturn(emailVerified);
    }

    private static void assertAuthCookieIssued(MvcResult result) {
        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies)
                .isNotEmpty()
                .anyMatch(c -> c.startsWith(AuthCookieService.DEFAULT_COOKIE_NAME + "=")
                        && !c.contains("Max-Age=0"));
    }

    private static void assertNoAuthCookie(MvcResult result) {
        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookies == null ? List.of() : setCookies)
                .noneMatch(c -> c.startsWith(AuthCookieService.DEFAULT_COOKIE_NAME + "=")
                        && !c.contains("Max-Age=0"));
    }
}
