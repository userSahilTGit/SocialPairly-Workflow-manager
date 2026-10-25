package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.security.CookieSessionCsrfRequestMatcher;
import com.SocialPairly_Workflow_Manager.security.JwtAuthenticationFilter;
import com.SocialPairly_Workflow_Manager.security.SpaCsrfTokenRequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
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
    private final int frontendPort;

    /**
     * Unit tests construct the shorter overload and do not load ports.properties.
     * The running app injects {@code app.frontend.port} from that file.
     */
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            String authCookieName,
            boolean requireHttps,
            boolean hstsEnabled,
            long hstsMaxAgeSeconds,
            boolean csrfEnabled,
            boolean authCookieSecure,
            String additionalCorsOrigins
    ) {
        this(jwtAuthenticationFilter, authCookieName, requireHttps, hstsEnabled, hstsMaxAgeSeconds,
                csrfEnabled, authCookieSecure, additionalCorsOrigins, 3001);
    }

    @Autowired
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${app.auth.cookie.name:SP_AUTH}") String authCookieName,
            @Value("${app.security.require-https:false}") boolean requireHttps,
            @Value("${app.security.hsts-enabled:false}") boolean hstsEnabled,
            @Value("${app.security.hsts-max-age-seconds:31536000}") long hstsMaxAgeSeconds,
            @Value("${app.security.csrf.enabled:true}") boolean csrfEnabled,
            @Value("${app.auth.cookie.secure:false}") boolean authCookieSecure,
            @Value("${app.security.cors.additional-origins:}") String additionalCorsOrigins,
            @Value("${app.frontend.port:3001}") int frontendPort
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authCookieName = authCookieName;
        this.requireHttps = requireHttps;
        this.hstsEnabled = hstsEnabled;
        this.hstsMaxAgeSeconds = hstsMaxAgeSeconds;
        this.csrfEnabled = csrfEnabled;
        this.authCookieSecure = authCookieSecure;
        this.additionalCorsOrigins = additionalCorsOrigins == null ? "" : additionalCorsOrigins;
        this.frontendPort = frontendPort;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/phone-verification/**").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/support/contact").permitAll()
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
                .ignoringRequestMatchers(
                        "/api/payment/webhook",
                        "/api/support/contact",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/google",
                        "/api/auth/apple",
                        "/api/auth/forgot-password/**",
                        "/api/auth/verify-email",
                        "/api/auth/verify-phone",
                        "/api/auth/resend-verification"
                )
        );
    }

    private void configureTransportSecurity(HttpSecurity http) throws Exception {
        // HSTS is meaningful with HTTPS; enable when either flag is on for prod safety.
        boolean applyHsts = hstsEnabled || requireHttps;
        http.headers(headers -> {
            // Google Identity Services uses window.postMessage from accounts.google.com;
            // default same-origin COOP blocks that handshake in some browsers.
            headers.crossOriginOpenerPolicy(coop -> coop.policy(
                    CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy.SAME_ORIGIN_ALLOW_POPUPS));
            headers.referrerPolicy(referrer -> referrer.policy(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
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
                "http://localhost:3010",
                "http://localhost:8081",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:3001",
                "http://127.0.0.1:3002",
                "http://127.0.0.1:3010",
                "http://3.151.77.90",
                "https://3.151.77.90",
                "http://3.151.77.90.nip.io",
                "https://3.151.77.90.nip.io",
                // Production domain — also keep IP origins above for direct access
                "https://www.socialpairly.com",
                "http://www.socialpairly.com",
                "https://socialpairly.com",
                "http://socialpairly.com"
        ));
        addConfiguredFrontendOrigin(origins, frontendPort);
        origins.addAll(parseAdditionalOrigins(additionalCorsOrigins));
        config.setAllowedOrigins(origins);
        // Local Vite any port; EC2 IP any port; production domain (http/https, www optional)
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://3.151.77.90:*",
                "https://3.151.77.90:*",
                "http://3.151.77.90.nip.io:*",
                "https://3.151.77.90.nip.io:*",
                "https://*.socialpairly.com",
                "http://*.socialpairly.com",
                "https://socialpairly.com",
                "http://socialpairly.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of(
                "Authorization",
                "X-XSRF-TOKEN",
                "X-Identity-Duration-Ms",
                "X-Identity-Op",
                "X-Identity-Within-Target"
        ));

        UrlBasedCorsConfigurationSource source = new SameHostCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Browsers send {@code Origin} on same-host POST requests such as
     * {@code https://www.socialpairly.com/api/auth/login}. Spring answers
     * {@code 403 Invalid CORS request} when that origin is not allowed, before
     * the password is checked. A request whose origin host matches the host
     * that received it is the site itself, so it is allowed. The apex hosts
     * {@code http://socialpairly.com} and {@code https://socialpairly.com}
     * are allowed when calling {@code www} as well. Other sites stay
     * limited to the list above.
     */
    private static final class SameHostCorsConfigurationSource extends UrlBasedCorsConfigurationSource {

        @Override
        public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
            CorsConfiguration base = super.getCorsConfiguration(request);
            if (base == null || request == null) {
                return base;
            }
            String origin = request.getHeader("Origin");
            if (origin == null || origin.isBlank() || base.checkOrigin(origin) != null) {
                return base;
            }
            // Apex and www are the same site. Also accept the default ports (:80 / :443),
            // which an exact allow-list entry does not match.
            if (!sameHost(request, origin) && !socialPairlyPublicOrigin(origin)) {
                return base;
            }
            CorsConfiguration copy = new CorsConfiguration(base);
            copy.addAllowedOrigin(origin.trim());
            return copy;
        }

        private static boolean socialPairlyPublicOrigin(String origin) {
            URI uri;
            try {
                uri = URI.create(origin.trim());
            } catch (IllegalArgumentException ex) {
                return false;
            }
            if (uri.getHost() == null || uri.getScheme() == null) {
                return false;
            }
            boolean site = "socialpairly.com".equalsIgnoreCase(uri.getHost())
                    || "www.socialpairly.com".equalsIgnoreCase(uri.getHost());
            if (!site) {
                return false;
            }
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPort() == -1 || uri.getPort() == 443;
            }
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPort() == -1 || uri.getPort() == 80;
            }
            return false;
        }

        private static boolean sameHost(HttpServletRequest request, String origin) {
            URI uri;
            try {
                uri = URI.create(origin.trim());
            } catch (IllegalArgumentException ex) {
                return false;
            }
            if (uri.getHost() == null || uri.getScheme() == null) {
                return false;
            }
            String forwardedHost = request.getHeader("X-Forwarded-Host");
            String host = (forwardedHost == null || forwardedHost.isBlank())
                    ? request.getServerName()
                    : forwardedHost.split(",")[0].trim();
            int colon = host.indexOf(':');
            if (colon > 0) {
                host = host.substring(0, colon);
            }
            String forwardedProto = request.getHeader("X-Forwarded-Proto");
            String scheme = (forwardedProto == null || forwardedProto.isBlank())
                    ? request.getScheme()
                    : forwardedProto.split(",")[0].trim();
            return uri.getHost().equalsIgnoreCase(host) && uri.getScheme().equalsIgnoreCase(scheme);
        }
    }

    /**
     * Always allow the UI port from ports.properties, including a port that is
     * not in the historical localhost list above. Wildcard patterns also allow
     * any localhost / 127.0.0.1 port.
     */
    private static void addConfiguredFrontendOrigin(List<String> origins, int port) {
        if (port <= 0 || port > 65535) {
            return;
        }
        for (String host : List.of("localhost", "127.0.0.1")) {
            String origin = "http://" + host + ":" + port;
            if (!origins.contains(origin)) {
                origins.add(origin);
            }
        }
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
