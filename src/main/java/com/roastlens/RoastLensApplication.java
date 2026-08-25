package com.roastlens;

import com.roastlens.config.RoastLensProperties;
import com.roastlens.connector.finstream.FinStreamProperties;
import com.roastlens.polling.PollingProperties;
import com.roastlens.roastability.RoastabilityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({RoastLensProperties.class, FinStreamProperties.class, RoastabilityProperties.class,
        PollingProperties.class})
public class RoastLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoastLensApplication.class, args);
    }
}
