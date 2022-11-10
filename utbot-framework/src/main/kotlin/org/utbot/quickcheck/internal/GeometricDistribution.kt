

package org.utbot.quickcheck.internal;

import org.utbot.quickcheck.random.SourceOfRandomness;

import static java.lang.Math.ceil;
import static java.lang.Math.log;

public class GeometricDistribution {
    public int sampleWithMean(double mean, SourceOfRandomness random) {
        return sample(probabilityOfMean(mean), random);
    }

    int sample(double p, SourceOfRandomness random) {
        ensureProbability(p);

        if (p == 1)
            return 0;

        double uniform = random.nextDouble();
        return (int) ceil(log(1 - uniform) / log(1 - p));
    }

    double probabilityOfMean(double mean) {
        if (mean <= 0) {
            throw new IllegalArgumentException(
                "Need a positive mean, got " + mean);
        }

        return 1 / mean;
    }

    private void ensureProbability(double p) {
        if (p <= 0 || p > 1) {
            throw new IllegalArgumentException(
                "Need a probability in (0, 1], got " + p);
        }
    }
}
