

package org.utbot.quickcheck.generator.java.util;

import org.utbot.quickcheck.generator.java.util.CollectionGenerator;

import java.util.List;

/**
 * Base class for generators of {@link List}s.
 *
 * @param <T> the type of list generated
 */
public abstract class ListGenerator<T extends List>
    extends CollectionGenerator<T> {

    protected ListGenerator(Class<T> type) {
        super(type);
    }
}
