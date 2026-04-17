package com.roastlens;

import com.roastlens.config.RoastLensProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RoastLensProperties.class)
public class RoastLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoastLensApplication.class, args);
    }
}
