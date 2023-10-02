package org.utbot.examples.spring.config.pure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.utbot.examples.spring.config.utils.SafetyUtils;

public class ExamplePureSpringConfig {
    @Bean(name = "exampleService0")
    public ExamplePureSpringService exampleService() {
        SafetyUtils.shouldNeverBeCalled();
        return null;
    }

    @Bean(name = "exampleServiceTest1")
    @Profile("test1")
    public ExamplePureSpringService exampleServiceTest1() {
        SafetyUtils.shouldNeverBeCalled();
        return null;
    }

    @Bean(name = "exampleServiceTest2")
    @Profile("test2")
    public ExamplePureSpringService exampleServiceTest2() {
        SafetyUtils.shouldNeverBeCalled();
        return null;
    }

    @Bean(name = "exampleServiceTest3")
    @Profile("test3")
    public ExamplePureSpringService exampleServiceTest3() {
        SafetyUtils.shouldNeverBeCalled();
        return null;
    }
}
