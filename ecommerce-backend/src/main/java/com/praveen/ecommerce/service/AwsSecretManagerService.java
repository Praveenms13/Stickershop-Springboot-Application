
package com.praveen.ecommerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.praveen.ecommerce.model.DbSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@Component
public class AwsSecretManagerService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.secrets.mysql.id}")
    private String secretId;

    public DbSecret getDbSecret() {
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(awsRegion))
                .build();
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretId)
                .build();
        GetSecretValueResponse response = client.getSecretValue(request);
        try {
            return objectMapper.readValue(response.secretString(), DbSecret.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse DB secret", e);
        }
    }
}
