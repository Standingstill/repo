package com.ensureback.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({EnsurebackProperties.class, EnsurebackEmailProperties.class})
public class ConfigPropertiesConfiguration {
}

