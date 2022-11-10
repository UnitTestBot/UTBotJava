

package org.utbot.quickcheck.generator.java.lang;

import org.utbot.quickcheck.random.SourceOfRandomness;

/**
 * <p>Produces {@link String}s whose characters are in the interval
 * {@code [0x0000, 0xD7FF]}.</p>
 */
public class StringGenerator extends AbstractStringGenerator {
    @Override protected int nextCodePoint(SourceOfRandomness random) {
        return random.nextInt(0, Character.MIN_SURROGATE - 1);
    }

}
