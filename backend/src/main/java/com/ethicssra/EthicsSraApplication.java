package com.ethicssra;

import com.ethicssra.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class EthicsSraApplication {

    public static void main(String[] args) {
        SpringApplication.run(EthicsSraApplication.class, args);
    }
}
