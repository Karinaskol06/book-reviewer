package com.project.bookreviewer.infrastructure.storage;

import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
    ObjectStoragePort localObjectStorage(StorageProperties properties) {
        LocalObjectStorageAdapter adapter = new LocalObjectStorageAdapter(properties);
        // Gets root path
        adapter.init();
        return adapter;
    }

    // If storage type is s3, then these beans are created instead of local
    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
    ObjectStoragePort s3ObjectStorage(S3Client s3Client, StorageProperties properties) {
        return new S3ObjectStorageAdapter(s3Client, properties);
    }

    // Configuration that help to talk to S3
    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
    S3Client s3Client(StorageProperties properties) {
        StorageProperties.S3 s3 = properties.getS3();
        var builder = S3Client.builder().region(Region.of(s3.getRegion()));

        if (s3.getAccessKey() != null && !s3.getAccessKey().isBlank()
                && s3.getSecretKey() != null && !s3.getSecretKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (s3.getEndpoint() != null && !s3.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3.getEndpoint()));
        }

        builder.serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(s3.isPathStyleAccess())
                .build());

        return builder.build();
    }
}
