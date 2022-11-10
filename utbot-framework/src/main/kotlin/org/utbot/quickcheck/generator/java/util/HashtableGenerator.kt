

package org.utbot.quickcheck.generator.java.util;

import org.utbot.quickcheck.generator.java.util.MapGenerator;

import java.util.Hashtable;

/**
 * Produces values of type {@link Hashtable}.
 */
public class HashtableGenerator extends MapGenerator<Hashtable> {
    public HashtableGenerator() {
        super(Hashtable.class);
    }

    @Override protected boolean okToAdd(Object key, Object value) {
        return key != null && value != null;
    }
}
