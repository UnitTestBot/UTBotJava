

package org.utbot.quickcheck.generator;

/**
 * {@link Generator}s are fed instances of this interface on each generation
 * so that, if they choose, they can use these instances to influence the
 * results of a generation for a particular property parameter.
 */
public interface GenerationStatus {
    /**
     * @return an arbitrary "size" parameter; this value (probabilistically)
     * increases for every successful generation
     */
    int size();

    /**
     * @return how many attempts have been made to generate a value for a
     * property parameter
     */
    int attempts();

}
