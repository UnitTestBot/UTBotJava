

package org.utbot.quickcheck.internal.generator;

import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.internal.GeometricDistribution;
import org.utbot.quickcheck.random.SourceOfRandomness;

abstract class AbstractGenerationStatus implements GenerationStatus {
    private final GeometricDistribution distro;
    private final SourceOfRandomness random;

    AbstractGenerationStatus(
        GeometricDistribution distro,
        SourceOfRandomness random) {

        this.distro = distro;
        this.random = random;
    }

    @Override public int size() {
        return distro.sampleWithMean(attempts() + 1, random);
    }

    protected final SourceOfRandomness random() {
        return random;
    }
}
