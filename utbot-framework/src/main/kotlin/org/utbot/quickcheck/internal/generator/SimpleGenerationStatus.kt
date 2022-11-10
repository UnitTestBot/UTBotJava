

package org.utbot.quickcheck.internal.generator;

import org.utbot.quickcheck.internal.GeometricDistribution;
import org.utbot.quickcheck.random.SourceOfRandomness;

public class SimpleGenerationStatus extends org.utbot.quickcheck.internal.generator.AbstractGenerationStatus {
    private final int attempts;

    public SimpleGenerationStatus(
        GeometricDistribution distro,
        SourceOfRandomness random,
        int attempts) {

        super(distro, random);
        this.attempts = attempts;
    }

    @Override public int attempts() {
        return attempts;
    }
}
