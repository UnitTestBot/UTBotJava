package org.utbot.examples.modificators;

/**
 * A class without fields.
 */
public class NoFields {
    public void print() {
        System.out.println("Hello!");
        printPrivately();
    }

    private void printPrivately() {
        System.out.println("Hello privately!");
    }
}
