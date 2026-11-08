package com.petlis.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class SchedulingConfigTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(SchedulingConfig.class);
    @Test void disabledByDefault() {
        context.run(c -> assertThat(c.containsBean("org.springframework.context.annotation.internalScheduledAnnotationProcessor")).isFalse());
    }
    @Test void explicitlyDisabled() {
        context.withPropertyValues("app.scheduling.enabled=false").run(c -> assertThat(c.containsBean("org.springframework.context.annotation.internalScheduledAnnotationProcessor")).isFalse());
    }
}
