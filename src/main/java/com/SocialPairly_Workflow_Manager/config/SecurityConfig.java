package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.security.CookieSessionCsrfRequestMatcher;
import com.SocialPairly_Workflow_Manager.security.JwtAuthenticationFilter;
import com.SocialPairly_Workflow_Manager.security.SpaCsrfTokenRequestHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final String authCookieName;
    private final boolean requireHttps;
    private final boolean hstsEnabled;
    private final long hstsMaxAgeSeconds;
    private final boolean csrfEnabled;
    private final boolean authCookieSecure;
    private final String additionalCorsOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${app.auth.cookie.name:SP_AUTH}") String authCookieName,
            @Value("${app.security.require-https:false}") boolean requireHttps,
            @Value("${app.security.hsts-enabled:false}") boolean hstsEnabled,
            @Value("${app.security.hsts-max-age-seconds:31536000}") long hstsMaxAgeSeconds,
            @Value("${app.security.csrf.enabled:true}") boolean csrfEnabled,
            @Value("${app.auth.cookie.secure:false}") boolean authCookieSecure,
            @Value("${app.security.cors.additional-origins:}") String additionalCorsOrigins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authCookieName = authCookieName;
        this.requireHttps = requireHttps;
        this.hstsEnabled = hstsEnabled;
        this.hstsMaxAgeSeconds = hstsMaxAgeSeconds;
        this.csrfEnabled = csrfEnabled;
        this.authCookieSecure = authCookieSecure;
        this.additionalCorsOrigins = additionalCorsOrigins == null ? "" : additionalCorsOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/phone-verification/**").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/payment/webhook").permitAll()
                        .requestMatchers("/api/plans/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/uploads/**", "/error").permitAll()
                        .requestMatchers("/api/media/*/stream").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        configureCsrf(http);
        configureTransportSecurity(http);

        return http.build();
    }

    private void configureCsrf(HttpSecurity http) throws Exception {
        if (!csrfEnabled) {
            http.csrf(csrf -> csrf.disable());
            return;
        }
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        // When HTTPS is required, CSRF cookie must be Secure as well.
        repository.setSecure(authCookieSecure || requireHttps);
        http.csrf(csrf -> csrf
                .csrfTokenRepository(repository)
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                // Protect cookie-only sessions; Bearer clients keep working unchanged
                .requireCsrfProtectionMatcher(new CookieSessionCsrfRequestMatcher(authCookieName))
                .ignoringRequestMatchers("/api/payment/webhook")
        );
    }

    private void configureTransportSecurity(HttpSecurity http) throws Exception {
        // HSTS is meaningful with HTTPS; enable when either flag is on for prod safety.
        boolean applyHsts = hstsEnabled || requireHttps;
        http.headers(headers -> {
            if (applyHsts) {
                headers.httpStrictTransportSecurity(hsts -> hsts
                        .maxAgeInSeconds(Math.max(0L, hstsMaxAgeSeconds))
                        .includeSubDomains(true));
            } else {
                headers.httpStrictTransportSecurity(hsts -> hsts.disable());
            }
        });
        if (requireHttps) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:3002",
                "http://localhost:8081",
                "http://3.151.77.90"));
        origins.addAll(parseAdditionalOrigins(additionalCorsOrigins));
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization", "X-XSRF-TOKEN"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    static List<String> parseAdditionalOrigins(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
