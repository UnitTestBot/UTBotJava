package org.utbot.examples.assemble.constructors;

/**
 * A class with a constructor that seems to be complex
 * but actually does not modify fields in models.
 */
public class PseudoComplexConstructor {
    public int a;
    private int nonModelField;

    public PseudoComplexConstructor() {
        nonModelField = 10;
    }
}

