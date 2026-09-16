package com.platform.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * AWS S3 Client Configuration
 * Connects to Floci Local AWS Emulator (http://localhost:4566) or real AWS S3 endpoint.
 */
@Slf4j
@Configuration
public class AwsS3Config {

    @Value("${aws.s3.endpoint-url:http://localhost:4566}")
    private String endpointUrl;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.credentials.access-key:test}")
    private String accessKey;

    @Value("${aws.credentials.secret-key:test}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        log.info("Configuring AWS S3 Client targeting endpoint: {} (Region: {})", endpointUrl, region);
        try {
            return S3Client.builder()
                    .endpointOverride(URI.create(endpointUrl))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .forcePathStyle(true) // Required for S3 emulators like Floci and LocalStack
                    .build();
        } catch (Exception e) {
            log.warn("Failed to initialize S3Client with endpoint {}. Using fallback.", endpointUrl, e);
            return null;
        }
    }
}
