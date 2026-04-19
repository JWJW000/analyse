package com.ethicssra.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "minio")
public class MinioConfig {

    @Bean
    public S3Client s3Client(StorageProperties storageProperties) {
        StorageProperties.Minio m = storageProperties.getMinio();
        return S3Client.builder()
                .endpointOverride(URI.create(m.getEndpoint()))
                .region(Region.of(m.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(m.getAccessKey(), m.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean
    public ApplicationRunner minioEnsureBucket(S3Client s3Client, StorageProperties storageProperties) {
        return args -> {
            String bucket = storageProperties.getMinio().getBucket();
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            } catch (S3Exception e) {
                if (e.statusCode() == 404) {
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                } else {
                    throw e;
                }
            }
        };
    }
}
