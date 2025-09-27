package com.ensureback.email;

import com.ensureback.config.EnsurebackEmailProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "ensureback.email", name = "enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final EnsurebackEmailProperties properties;
    private final EmailTemplateRenderer templateRenderer;

    public SmtpEmailService(JavaMailSender mailSender,
                            EnsurebackEmailProperties properties,
                            EmailTemplateRenderer templateRenderer) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public void sendEmail(String to, String subject, String templateName, Map<String, ?> variables) {
        String body = templateRenderer.render(templateName, variables);
        if (body == null) {
            log.warn("Skipping email to {} because template {} could not be rendered", to, templateName);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}