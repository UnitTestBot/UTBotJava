

package org.utbot.quickcheck.generator.java.util;

import org.utbot.quickcheck.generator.java.util.ListGenerator;

import java.util.ArrayList;

/**
 * Produces values of type {@link ArrayList}.
 */
public class ArrayListGenerator extends ListGenerator<ArrayList> {
    public ArrayListGenerator() {
        super(ArrayList.class);
    }
}
