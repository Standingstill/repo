package com.ensureback.email;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmailTemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateRenderer.class);
    private static final String TEMPLATE_LOCATION = "templates/email/";

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String render(String templateName, Map<String, ?> variables) {
        String template = loadTemplate(templateName);
        if (template == null) {
            return null;
        }
        String rendered = template;
        if (variables != null) {
            for (Map.Entry<String, ?> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                rendered = rendered.replace(placeholder, value);
            }
        }
        return rendered;
    }

    private String loadTemplate(String templateName) {
        if (!StringUtils.hasText(templateName)) {
            return null;
        }
        return cache.computeIfAbsent(templateName, name -> {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_LOCATION + name + ".txt");
            if (!resource.exists()) {
                log.warn("Email template {} not found", name);
                return null;
            }
            try {
                byte[] bytes = resource.getInputStream().readAllBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                log.warn("Failed to load email template {}", name, ex);
                return null;
            }
        });
    }
}