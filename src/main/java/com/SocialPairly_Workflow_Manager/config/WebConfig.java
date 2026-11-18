package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.security.DashboardAccessInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final DashboardAccessInterceptor dashboardAccessInterceptor;

    public WebConfig(DashboardAccessInterceptor dashboardAccessInterceptor) {
        this.dashboardAccessInterceptor = dashboardAccessInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(dashboardAccessInterceptor)
                .addPathPatterns("/api/admin/**", "/api/plans/admin/**");
    }
}