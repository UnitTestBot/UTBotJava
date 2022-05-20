package org.utbot.examples.mock;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Random = Random or any successors
 * Cases:
 * 1. Random as parameter;
 * 2. Random as a field;
 * 3. Random as a local variable;
 */
public class MockRandomExamples {
    @SuppressWarnings("unused")
    private Random random;

    public int randomAsParameter(Random random, int threshold) {
        int nextInt = random.nextInt();
        if (nextInt > threshold) {
            return threshold + 1;
        }
        return nextInt;
    }

    public int randomAsField(int threshold) {
        int nextInt = random.nextInt();
        if (nextInt > threshold) {
            return threshold + 1;
        }
        return nextInt;
    }

    public int randomAsLocalVariable() {
        Random first = new Random(123);
        Random second = new Random(123);
        if (first.nextInt() + first.nextInt() + second.nextInt() > 1000) {
            return second.nextInt();
        }
        return new Random(123).nextInt();
    }

    public int useSecureRandom() {
        Random random = new SecureRandom();
        if (random.nextInt() > 1000) {
            return 1;
        }
        return random.nextInt();
    }
}