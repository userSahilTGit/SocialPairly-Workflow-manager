package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.entity.Question;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.repository.QuestionRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import com.SocialPairly_Workflow_Manager.security.JwtAuthenticationFilter;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void dataInitializerShouldSeedAdminAndQuestionsWhenMissing() {
        ReflectionTestUtils.setField(dataInitializer, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(dataInitializer, "adminPassword", "secret123");

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(questionRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dataInitializer.run();

        verify(userRepository).save(any(User.class));
        verify(questionRepository, times(3)).save(any(Question.class));
    }

    @Test
    void dataSourceConfigShouldCreateDataSourcePropertiesAndHikariDataSource() {
        DataSourceConfig config = new DataSourceConfig();

        DataSourceProperties properties = config.mysqlDataSourceProperties();
        properties.setUrl("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        properties.setDriverClassName("org.h2.Driver");
        properties.setUsername("sa");
        properties.setPassword("");
        DataSource dataSource = config.mysqlDataSource(properties);

        assertNotNull(properties);
        assertNotNull(dataSource);
        assertInstanceOf(HikariDataSource.class, dataSource);
    }

    @Test
    void securityConfigShouldExposeCorsAndPasswordEncoderBeans() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                "SP_AUTH",
                false,
                false,
                31_536_000L,
                true,
                false);

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);
        assertInstanceOf(UrlBasedCorsConfigurationSource.class, source);

        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfiguration(new MockHttpServletRequest());
        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3001"));
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(config.getAllowedOrigins().contains("http://localhost:8081"));
        assertTrue(config.getAllowedOrigins().contains("http://3.151.77.90"));
        assertTrue(config.getAllowedMethods().contains("GET"));

        assertNotNull(securityConfig.passwordEncoder());
    }

    @Test
    void webConfigShouldRegisterUploadsResourceHandler() {
        WebConfig webConfig = new WebConfig();
        ReflectionTestUtils.setField(webConfig, "uploadDir", "uploads");

        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);
        when(registry.addResourceHandler("/uploads/**")).thenReturn(registration);

        webConfig.addResourceHandlers(registry);

        verify(registry).addResourceHandler("/uploads/**");
        verify(registration).addResourceLocations(anyString());
    }
}
