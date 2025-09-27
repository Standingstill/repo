package com.ensureback.shipping;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AfterShipProperties.class)
public class ShippingConfiguration {

    @Bean
    public WebClient afterShipWebClient(AfterShipProperties properties, WebClient.Builder builder) {
        return builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("aftership-api-key", properties.getApiKey())
                .build();
    }
}
