package org.utbot.examples.spring.config.pure;

import org.springframework.context.annotation.Bean;
import org.utbot.examples.spring.config.utils.SafetyUtils;

public class ExamplePureSpringConfig {
    @Bean(name = "exampleService0")
    public ExamplePureSpringService exampleService() {
        SafetyUtils.shouldNeverBeCalled();
        return null;
    }
}
