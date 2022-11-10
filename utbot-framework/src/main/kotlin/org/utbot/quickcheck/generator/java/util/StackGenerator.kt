

package org.utbot.quickcheck.generator.java.util;

import org.utbot.quickcheck.generator.java.util.ListGenerator;

import java.util.Stack;

/**
 * Produces values of type {@link Stack}.
 */
public class StackGenerator extends ListGenerator<Stack> {
    public StackGenerator() {
        super(Stack.class);
    }
}
