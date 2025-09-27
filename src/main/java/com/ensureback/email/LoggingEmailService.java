package com.ensureback.email;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "ensureback.email", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);
    private final EmailTemplateRenderer templateRenderer;

    public LoggingEmailService(EmailTemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }

    @Override
    public void sendEmail(String to, String subject, String templateName, Map<String, ?> variables) {
        String body = templateRenderer.render(templateName, variables);
        log.info("[EMAIL:LOG] to={} subject={} template={} body={}", to, subject, templateName, body);
    }
}