

package org.utbot.quickcheck.generator.java.util;

import org.utbot.quickcheck.generator.java.util.MapGenerator;

import java.util.LinkedHashMap;

/**
 * Produces values of type {@link LinkedHashMap}.
 */
public class LinkedHashMapGenerator extends MapGenerator<LinkedHashMap> {
    public LinkedHashMapGenerator() {
        super(LinkedHashMap.class);
    }
}
