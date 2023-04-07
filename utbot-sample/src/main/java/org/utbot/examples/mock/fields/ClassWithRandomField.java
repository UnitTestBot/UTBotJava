package org.utbot.examples.mock.fields;

import java.util.Random;

public class ClassWithRandomField {
    public Random random = new Random();

    public int nextInt() {
        return random.nextInt();
    }
}
