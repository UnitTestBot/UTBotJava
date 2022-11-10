

package org.utbot.quickcheck.generator.java.util;

import org.utbot.quickcheck.generator.java.util.ListGenerator;

import java.util.LinkedList;

/**
 * Produces values of type {@link LinkedList}.
 */
public class LinkedListGenerator extends ListGenerator<LinkedList> {
    public LinkedListGenerator() {
        super(LinkedList.class);
    }
}
