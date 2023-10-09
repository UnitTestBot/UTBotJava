package org.utbot.examples.spring.config.utils;

public class SafetyUtils {

    private static final int UNEXPECTED_CALL_EXIT_STATUS = -1182;

    /**
     * Bean constructors and factory methods should never be executed during bean analysis,
     * hence call to this method is added into them to ensure they are actually never called.
     */
    public static void shouldNeverBeCalled() {
        System.err.println("shouldNeverBeCalled() is unexpectedly called");
        System.exit(UNEXPECTED_CALL_EXIT_STATUS);
    }
}
