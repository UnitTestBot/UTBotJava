package org.utbot.examples.mock;

import org.utbot.examples.mock.others.Generator;

public class MockStaticFieldExample {
    @SuppressWarnings("unused")
    private static Generator privateGenerator;
    public static Generator publicGenerator;

    public int calculate(int threshold) {
        int a = privateGenerator.generateInt();
        int b = publicGenerator.generateInt();
        if (threshold < a + b) {
            return threshold;
        }
        return a + b + 1;
    }
}