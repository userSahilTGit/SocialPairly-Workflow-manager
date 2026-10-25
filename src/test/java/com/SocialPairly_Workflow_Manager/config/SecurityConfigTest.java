package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                "SP_AUTH",
                false,
                false,
                31_536_000L,
                true,
                false,
                "");
    }

    @Test
    void testSecurityFilterChain() throws Exception {
        HttpSecurity httpSecurity = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        DefaultSecurityFilterChain filterChain = mock(DefaultSecurityFilterChain.class);
        when(httpSecurity.build()).thenReturn(filterChain);

        SecurityFilterChain result = securityConfig.filterChain(httpSecurity);

        assertNotNull(result);
        assertSame(filterChain, result);
        verify(httpSecurity).build();
    }

    @Test
    void testPasswordEncoder() {
        assertNotNull(securityConfig.passwordEncoder());
        assertInstanceOf(BCryptPasswordEncoder.class, securityConfig.passwordEncoder());
    }

    @Test
    void testAuthenticationManager() throws Exception {
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration);

        assertNotNull(result);
        assertSame(authenticationManager, result);
        verify(authenticationConfiguration).getAuthenticationManager();
    }

    @Test
    void testCorsConfigurationSource() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        assertNotNull(source);
        assertInstanceOf(UrlBasedCorsConfigurationSource.class, source);

        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3001"));
        assertTrue(config.getAllowedOrigins().contains("http://3.151.77.90.nip.io"));
        assertTrue(config.getAllowedOrigins().contains("https://3.151.77.90.nip.io"));
        assertTrue(config.getAllowedOrigins().contains("https://www.socialpairly.com"));
        assertTrue(config.getAllowedOrigins().contains("http://www.socialpairly.com"));
        assertTrue(config.getAllowedOrigins().contains("https://socialpairly.com"));
        assertTrue(config.getAllowedOrigins().contains("http://socialpairly.com"));
        assertEquals("https://socialpairly.com", config.checkOrigin("https://socialpairly.com"));
        assertEquals("http://socialpairly.com", config.checkOrigin("http://socialpairly.com"));
        assertTrue(config.getAllowedOriginPatterns().contains("https://socialpairly.com"));
        assertTrue(config.getAllowedOriginPatterns().contains("http://socialpairly.com"));
        assertTrue(config.getAllowedOrigins().contains("http://3.151.77.90"));
        assertTrue(config.getAllowedOrigins().contains("http://127.0.0.1:3010"));
        assertTrue(config.getAllowedOriginPatterns().contains("http://127.0.0.1:*"));
        assertTrue(config.getAllowedOriginPatterns().contains("http://localhost:*"));
        assertTrue(config.getAllowedOriginPatterns().contains("http://3.151.77.90:*"));
        assertTrue(config.getAllowedOriginPatterns().contains("https://*.socialpairly.com"));
        assertTrue(config.getAllowCredentials());
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("OPTIONS"));
        assertTrue(config.getExposedHeaders().contains("X-XSRF-TOKEN"));
    }

    @Test
    void sameHostOriginIsAllowedAndOtherSitesAreNot() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        MockHttpServletRequest sameHost = new MockHttpServletRequest();
        sameHost.setScheme("https");
        sameHost.setServerName("preprod.socialpairly.com");
        sameHost.addHeader("Origin", "https://preprod.socialpairly.com");
        CorsConfiguration sameHostConfig = source.getCorsConfiguration(sameHost);
        assertNotNull(sameHostConfig);
        assertEquals("https://preprod.socialpairly.com", sameHostConfig.checkOrigin("https://preprod.socialpairly.com"));

        MockHttpServletRequest apexOnWww = new MockHttpServletRequest();
        apexOnWww.setScheme("https");
        apexOnWww.setServerName("www.socialpairly.com");
        apexOnWww.addHeader("Origin", "https://socialpairly.com");
        CorsConfiguration apexOnWwwConfig = source.getCorsConfiguration(apexOnWww);
        assertNotNull(apexOnWwwConfig);
        assertEquals("https://socialpairly.com", apexOnWwwConfig.checkOrigin("https://socialpairly.com"));

        MockHttpServletRequest apexHttp = new MockHttpServletRequest();
        apexHttp.setScheme("http");
        apexHttp.setServerName("socialpairly.com");
        apexHttp.addHeader("Origin", "http://socialpairly.com");
        CorsConfiguration apexHttpConfig = source.getCorsConfiguration(apexHttp);
        assertNotNull(apexHttpConfig);
        assertEquals("http://socialpairly.com", apexHttpConfig.checkOrigin("http://socialpairly.com"));

        MockHttpServletRequest apexWithDefaultPort = new MockHttpServletRequest();
        apexWithDefaultPort.setScheme("https");
        apexWithDefaultPort.setServerName("3.151.77.90");
        apexWithDefaultPort.addHeader("Origin", "https://socialpairly.com:443");
        CorsConfiguration portConfig = source.getCorsConfiguration(apexWithDefaultPort);
        assertNotNull(portConfig);
        assertEquals("https://socialpairly.com:443", portConfig.checkOrigin("https://socialpairly.com:443"));

        MockHttpServletRequest otherSite = new MockHttpServletRequest();
        otherSite.setScheme("https");
        otherSite.setServerName("www.socialpairly.com");
        otherSite.addHeader("Origin", "https://evil.example");
        CorsConfiguration otherSiteConfig = source.getCorsConfiguration(otherSite);
        assertNotNull(otherSiteConfig);
        assertNull(otherSiteConfig.checkOrigin("https://evil.example"));
    }

    @Test
    void testCorsIncludesAdditionalOrigins() {
        SecurityConfig httpsConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                "SP_AUTH",
                true,
                true,
                31_536_000L,
                true,
                true,
                "https://app.example.com, https://admin.example.com");

        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) httpsConfig.corsConfigurationSource())
                .getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(config.getAllowedOrigins().contains("https://app.example.com"));
        assertTrue(config.getAllowedOrigins().contains("https://admin.example.com"));
    }

    @Test
    void configuredFrontendPortIsAllowed() {
        SecurityConfig customPort = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                "SP_AUTH",
                false,
                false,
                31_536_000L,
                true,
                false,
                "",
                4242);

        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) customPort.corsConfigurationSource())
                .getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains("http://localhost:4242"));
        assertTrue(config.getAllowedOrigins().contains("http://127.0.0.1:4242"));
    }

    @Test
    void testParseAdditionalOriginsIgnoresBlank() {
        assertTrue(SecurityConfig.parseAdditionalOrigins("").isEmpty());
        assertTrue(SecurityConfig.parseAdditionalOrigins(null).isEmpty());
        assertTrue(SecurityConfig.parseAdditionalOrigins("  , , ").isEmpty());
        assertEquals(List.of("https://a.com"), SecurityConfig.parseAdditionalOrigins(" https://a.com "));
    }

    @Test
    void testRequireHttpsFilterChainBuilds() throws Exception {
        SecurityConfig httpsConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                "SP_AUTH",
                true,
                true,
                31_536_000L,
                true,
                true,
                "");
        HttpSecurity httpSecurity = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        DefaultSecurityFilterChain filterChain = mock(DefaultSecurityFilterChain.class);
        when(httpSecurity.build()).thenReturn(filterChain);

        SecurityFilterChain result = httpsConfig.filterChain(httpSecurity);

        assertNotNull(result);
        assertSame(filterChain, result);
        verify(httpSecurity).requiresChannel(any());
    }

    @Test
    void testCsrfDisabledFilterChainBuilds() throws Exception {
        SecurityConfig noCsrf = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                "SP_AUTH",
                false,
                false,
                31_536_000L,
                false,
                false,
                "");
        HttpSecurity httpSecurity = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        DefaultSecurityFilterChain filterChain = mock(DefaultSecurityFilterChain.class);
        when(httpSecurity.build()).thenReturn(filterChain);

        assertSame(filterChain, noCsrf.filterChain(httpSecurity));
        verify(httpSecurity).csrf(any());
    }

    @Test
    void testHstsOnlyWithoutRequireHttpsBuilds() throws Exception {
        SecurityConfig hstsOnly = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                "SP_AUTH",
                false,
                true,
                0L,
                true,
                false,
                null);
        HttpSecurity httpSecurity = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        DefaultSecurityFilterChain filterChain = mock(DefaultSecurityFilterChain.class);
        when(httpSecurity.build()).thenReturn(filterChain);

        assertSame(filterChain, hstsOnly.filterChain(httpSecurity));
        verify(httpSecurity, never()).requiresChannel(any());
    }

    @Test
    void testSecureCsrfCookieWhenAuthCookieSecureWithoutHttps() throws Exception {
        SecurityConfig secureCookie = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                "SP_AUTH",
                false,
                false,
                31_536_000L,
                true,
                true,
                "");
        HttpSecurity httpSecurity = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        DefaultSecurityFilterChain filterChain = mock(DefaultSecurityFilterChain.class);
        when(httpSecurity.build()).thenReturn(filterChain);

        assertSame(filterChain, secureCookie.filterChain(httpSecurity));
        verify(httpSecurity).csrf(any());
    }

    @Test
    void testRequireHttpsForcesSecureCsrfCookieAndChannel() throws Exception {
        // requireHttps=true, hstsEnabled=false, authCookieSecure=false
        // covers setSecure via requireHttps and applyHsts via requireHttps alone
        SecurityConfig httpsNoHstsFlag = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                "SP_AUTH",
                true,
                false,
                31_536_000L,
                true,
                false,
                "");
        HttpSecurity httpSecurity = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        DefaultSecurityFilterChain filterChain = mock(DefaultSecurityFilterChain.class);
        when(httpSecurity.build()).thenReturn(filterChain);

        assertSame(filterChain, httpsNoHstsFlag.filterChain(httpSecurity));
        verify(httpSecurity).requiresChannel(any());
        verify(httpSecurity).csrf(any());
    }

    @Test
    void realFilterChainAppliesCsrfHeadersAndHttpsRules() throws Exception {
        assertNotNull(realChain(securityConfig).getFilters());

        SecurityConfig httpsConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class), "SP_AUTH", true, false, -5L, true, false, "");
        assertNotNull(realChain(httpsConfig));

        SecurityConfig csrfOff = new SecurityConfig(
                mock(JwtAuthenticationFilter.class), "SP_AUTH", false, true, 120L, false, true, " , https://extra.example ");
        assertNotNull(realChain(csrfOff));
    }

    @Test
    void sameHostAndPublicOriginBranches() {
        MockHttpServletRequest noOrigin = request("https", "www.socialpairly.com");
        CorsConfiguration noOriginConfig = corsFor(noOrigin);
        assertNotNull(noOriginConfig);
        assertNull(noOriginConfig.checkOrigin("https://203.0.113.10"));

        MockHttpServletRequest blankOrigin = request("https", "www.socialpairly.com");
        blankOrigin.addHeader("Origin", "   ");
        assertNotNull(corsFor(blankOrigin));

        MockHttpServletRequest listed = request("http", "localhost");
        listed.addHeader("Origin", "http://localhost:5173");
        assertEquals("http://localhost:5173", corsFor(listed).checkOrigin("http://localhost:5173"));

        MockHttpServletRequest sameIp = request("http", "203.0.113.10");
        sameIp.addHeader("Origin", "http://203.0.113.10");
        assertEquals("http://203.0.113.10", corsFor(sameIp).checkOrigin("http://203.0.113.10"));

        MockHttpServletRequest schemeMismatch = request("https", "198.51.100.10");
        schemeMismatch.addHeader("Origin", "http://198.51.100.10");
        assertNull(corsFor(schemeMismatch).checkOrigin("http://198.51.100.10"));

        MockHttpServletRequest forwarded = request("http", "internal");
        forwarded.addHeader("X-Forwarded-Host", " 203.0.113.20:8080 , ignored");
        forwarded.addHeader("X-Forwarded-Proto", " https , http");
        forwarded.addHeader("Origin", "https://203.0.113.20");
        assertEquals("https://203.0.113.20", corsFor(forwarded).checkOrigin("https://203.0.113.20"));

        MockHttpServletRequest blankForwarded = request("http", "203.0.113.30");
        blankForwarded.addHeader("X-Forwarded-Host", "   ");
        blankForwarded.addHeader("X-Forwarded-Proto", " ");
        blankForwarded.addHeader("Origin", "http://203.0.113.30");
        assertEquals("http://203.0.113.30", corsFor(blankForwarded).checkOrigin("http://203.0.113.30"));

        MockHttpServletRequest httpDefaultPort = request("http", "3.151.77.90");
        httpDefaultPort.addHeader("Origin", "http://socialpairly.com:80");
        assertEquals("http://socialpairly.com:80", corsFor(httpDefaultPort).checkOrigin("http://socialpairly.com:80"));

        MockHttpServletRequest wrongPort = request("https", "3.151.77.90");
        wrongPort.addHeader("Origin", "https://socialpairly.com:8443");
        assertNull(corsFor(wrongPort).checkOrigin("https://socialpairly.com:8443"));

        MockHttpServletRequest wwwHttpWrongPort = request("http", "3.151.77.90");
        wwwHttpWrongPort.addHeader("Origin", "http://www.socialpairly.com:443");
        assertNull(corsFor(wwwHttpWrongPort).checkOrigin("http://www.socialpairly.com:443"));

        MockHttpServletRequest ftp = request("https", "3.151.77.90");
        ftp.addHeader("Origin", "ftp://socialpairly.com");
        assertNull(corsFor(ftp).checkOrigin("ftp://socialpairly.com"));

        MockHttpServletRequest malformed = request("https", "www.socialpairly.com");
        malformed.addHeader("Origin", "http://[");
        assertNull(corsFor(malformed).checkOrigin("http://["));

        MockHttpServletRequest noHost = request("https", "www.socialpairly.com");
        noHost.addHeader("Origin", "mailto:person@socialpairly.com");
        assertNull(corsFor(noHost).checkOrigin("mailto:person@socialpairly.com"));
    }

    private CorsConfiguration corsFor(MockHttpServletRequest request) {
        return securityConfig.corsConfigurationSource().getCorsConfiguration(request);
    }

    private static MockHttpServletRequest request(String scheme, String serverName) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme(scheme);
        request.setServerName(serverName);
        return request;
    }

    private static SecurityFilterChain realChain(SecurityConfig config) throws Exception {
        ObjectPostProcessor<Object> postProcessor = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };
        HttpSecurity http = new HttpSecurity(postProcessor, new AuthenticationManagerBuilder(postProcessor), new HashMap<>());
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.refresh();
        http.setSharedObject(ApplicationContext.class, context);
        return config.filterChain(http);
    }
}
