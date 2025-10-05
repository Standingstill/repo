package com.ensureback.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@DataJpaTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Import({WebhookIdempotencyService.class, WebhookIdempotencyServiceTest.TestConfig.class})
class WebhookIdempotencyServiceTest {

    @Autowired
    private WebhookIdempotencyService service;

    @Test
    void registerInvocationIsIdempotentPerSourceAndKey() {
        boolean first = service.registerInvocation("stripe", "evt_123", "{\"id\":\"evt_123\"}");
        boolean second = service.registerInvocation("stripe", "evt_123", "{\"id\":\"evt_123\"}");

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void fallsBackToPayloadHashWhenKeyMissing() {
        String payload = "{\"tracking\":\"abc\"}";
        boolean first = service.registerInvocation("aftership", null, payload);
        boolean second = service.registerInvocation("aftership", null, payload);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void differentSourcesCanReuseSameKey() {
        boolean stripe = service.registerInvocation("stripe", "shared-key", "stripe");
        boolean aftership = service.registerInvocation("aftership", "shared-key", "aftership");

        assertThat(stripe).isTrue();
        assertThat(aftership).isTrue();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return NoOpPasswordEncoder.getInstance();
        }

        @Bean
        @Order(0)
        CommandLineRunner demoUserLoader() {
            return args -> {
                // override application seed runner during slice tests
            };
        }
    }
}
