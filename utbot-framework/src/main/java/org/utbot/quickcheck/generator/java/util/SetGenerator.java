

package org.utbot.quickcheck.generator.java.util;

import org.utbot.quickcheck.generator.Size;
import org.utbot.quickcheck.generator.java.util.CollectionGenerator;

import java.util.Set;

/**
 * Base class for generators of {@link Set}s.
 *
 * @param <T> the type of set generated
 */
public abstract class SetGenerator<T extends Set>
    extends CollectionGenerator<T> {

    protected SetGenerator(Class<T> type) {
        super(type);
    }

    @Override public void configure(Size size) {
        super.configure(size);

        setDistinct(true);
    }
}
