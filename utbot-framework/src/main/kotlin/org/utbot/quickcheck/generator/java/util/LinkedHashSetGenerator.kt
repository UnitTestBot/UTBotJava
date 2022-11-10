

package org.utbot.quickcheck.generator.java.util;

import org.utbot.quickcheck.generator.java.util.SetGenerator;

import java.util.LinkedHashSet;

/**
 * Produces values of type {@link LinkedHashSet}.
 */
public class LinkedHashSetGenerator extends SetGenerator<LinkedHashSet> {
    public LinkedHashSetGenerator() {
        super(LinkedHashSet.class);
    }
}
