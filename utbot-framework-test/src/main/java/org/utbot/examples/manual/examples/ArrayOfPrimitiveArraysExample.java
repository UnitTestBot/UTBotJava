package org.utbot.examples.manual.examples;

import org.utbot.examples.assemble.arrays.ArrayOfPrimitiveArrays;

public class ArrayOfPrimitiveArraysExample {
    int assign10(ArrayOfPrimitiveArrays a) {
        a.array[0][0] = 10;
        return a.array[0][0];
    }
}
