package org.utbot.examples.postcondition;

public class ClassWithPrimitivesContainer {
    private PrimitivesContainer primitivesContainer;

    private int y = 0;
    private long x;

    public ClassWithPrimitivesContainer getPrimitivesContainer(int z) {
        return this;
    }
}
