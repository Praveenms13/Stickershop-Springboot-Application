package com.praveen.ecommerce.config;

import com.praveen.ecommerce.model.DbSecret;
import com.praveen.ecommerce.service.AwsSecretManagerService;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    private final AwsSecretManagerService secretManagerService;

    public ProdDataSourceConfig(AwsSecretManagerService secretManagerService) {
        this.secretManagerService = secretManagerService;
    }

    @Bean
    public DataSource dataSource() {
        DbSecret secret = secretManagerService.getDbSecret();
        String url = String.format(
                "jdbc:mysql://%s:%s/%s",
                secret.getHost(),
                secret.getPort(),
                secret.getDbname()
        );

        return DataSourceBuilder.create()
                .url(url)
                .username(secret.getUsername())
                .password(secret.getPassword())
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }
}