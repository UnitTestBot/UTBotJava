package org.utbot.examples.spring.config.xml;

import org.utbot.examples.spring.config.utils.SafetyUtils;

public class ExampleXmlService {
    public ExampleXmlService() {
        SafetyUtils.shouldNeverBeCalled();
    }
}
