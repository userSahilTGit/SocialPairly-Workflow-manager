package com.SocialPairly_Workflow_Manager.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Binds the custom {@code spring.datasource.mysql.*} namespace (and its
 * {@code .hikari.*} pool settings) to the primary application DataSource.
 * Spring Boot's JPA auto-configuration then uses this DataSource.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.mysql")
    public DataSourceProperties mysqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.mysql.hikari")
    public DataSource mysqlDataSource(DataSourceProperties mysqlDataSourceProperties) {
        return mysqlDataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
}