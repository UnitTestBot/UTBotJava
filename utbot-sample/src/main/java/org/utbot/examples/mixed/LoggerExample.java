package org.utbot.examples.mixed;

import org.utbot.examples.objects.ObjectWithPrimitivesClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerExample {
    private static final Logger logger = LoggerFactory.getLogger(ObjectWithPrimitivesClass.class);

    public int example() {
        int a = 5;
        int b = 10;

        int sum = a + b;

        logger.debug("Debug info");
        logger.error("An error");
        logger.info("Info");

        return sum;
    }

    public int loggerUsage() {
        if (logger.isDebugEnabled()) {
            return 1;
        } else {
            return 2;
        }
    }
}
