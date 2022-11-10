

package org.utbot.quickcheck.generator.java.util;

import org.utbot.quickcheck.generator.java.util.SetGenerator;

import java.util.HashSet;

/**
 * Produces values of type {@link HashSet}.
 */
public class HashSetGenerator extends SetGenerator<HashSet> {
    public HashSetGenerator() {
        super(HashSet.class);
    }
}
