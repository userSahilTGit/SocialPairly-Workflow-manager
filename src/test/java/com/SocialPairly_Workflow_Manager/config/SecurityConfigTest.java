package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private HttpSecurity httpSecurity; // <--- Inject HttpSecurity here

    @Test
    void testSecurityFilterChain() throws Exception {
        // Pass the injected HttpSecurity instance
        SecurityFilterChain filterChain = securityConfig.filterChain(httpSecurity);
        assertNotNull(filterChain);
    }

    @Test
    void testPasswordEncoder() {
        assertNotNull(securityConfig.passwordEncoder());
        assertTrue(securityConfig.passwordEncoder() instanceof org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder);
    }

    @Test
    void testAuthenticationManager() throws Exception {
        assertNotNull(securityConfig.authenticationManager(null));
    }

    @Test
    void testCorsConfigurationSource() {
        assertNotNull(securityConfig.corsConfigurationSource());
    }
}