
package org.utbot.quickcheck.generator;

import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.random.SourceOfRandomness;

/**
 * Represents a strategy for generating random values.
 *
 * @param <T> type of values generated
 */
@FunctionalInterface
public interface Gen<T> {
    /**
     * Generates a value, possibly influenced by a source of randomness and
     * metadata about the generation.
     *
     * @param random source of randomness to be used when generating the value
     * @param status an object that can be used to influence the generated
     * value. For example, generating lists can use the {@link
     * GenerationStatus#size() size} method to generate lists with a given
     * number of elements.
     * @return the generated value
     */
    UtModel generate(SourceOfRandomness random, GenerationStatus status);
}
