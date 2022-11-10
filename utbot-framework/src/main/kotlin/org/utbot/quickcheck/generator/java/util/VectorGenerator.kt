

package org.utbot.quickcheck.generator.java.util;

import org.utbot.quickcheck.generator.java.util.ListGenerator;

import java.util.Vector;

/**
 * Produces values of type {@link Vector}.
 */
public class VectorGenerator extends ListGenerator<Vector> {
    public VectorGenerator() {
        super(Vector.class);
    }
}
