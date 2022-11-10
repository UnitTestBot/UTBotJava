

package org.utbot.quickcheck.generator.java.util;

import org.utbot.quickcheck.generator.java.util.MapGenerator;

import java.util.HashMap;

/**
 * Produces values of type {@link HashMap}.
 */
public class HashMapGenerator extends MapGenerator<HashMap> {
    public HashMapGenerator() {
        super(HashMap.class);
    }
}
