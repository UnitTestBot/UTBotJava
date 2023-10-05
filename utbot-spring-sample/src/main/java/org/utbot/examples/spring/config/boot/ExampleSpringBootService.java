package org.utbot.examples.spring.config.boot;

import org.springframework.stereotype.Service;
import org.utbot.examples.spring.config.utils.SafetyUtils;

@Service
public class ExampleSpringBootService {
    public ExampleSpringBootService() {
        SafetyUtils.shouldNeverBeCalled();
    }
}
