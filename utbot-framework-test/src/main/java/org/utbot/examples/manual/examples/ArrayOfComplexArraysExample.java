package org.utbot.examples.manual.examples;

import org.utbot.examples.assemble.arrays.ArrayOfComplexArrays;

public class ArrayOfComplexArraysExample {
    public int getValue(ArrayOfComplexArrays a) {
        return a.array[0].array[0].getB();
    }
}
