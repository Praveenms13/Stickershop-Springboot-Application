package com.praveen.ecommerce.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("!prod") // Active for all profiles except 'prod'
public class LocalDataSourceConfig {

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(System.getProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/stickershop"))
                .username(System.getProperty("spring.datasource.username", "root"))
                .password(System.getProperty("spring.datasource.password", "root"))
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }
}

