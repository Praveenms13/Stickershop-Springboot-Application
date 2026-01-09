package com.praveen.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
@Configuration
public class AwsConfig {
    @Bean
    Region awsRegion(@Value("${aws.region}") String region) {
        return Region.of(region);
    }
    @Bean
    S3Client s3Client(Region region) {
        return S3Client.builder().region(region).build();
    }
}
