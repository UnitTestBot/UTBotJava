package org.utbot.examples.arrays;


import org.utbot.api.mock.UtMock;

public class ArrayOfObjects {
    public ObjectWithPrimitivesClass[] defaultValues() {
        ObjectWithPrimitivesClass[] array = new ObjectWithPrimitivesClass[1];
        //noinspection IfStatementWithIdenticalBranches
        if (array[0] != null) {
            return array;
        }
        return array;
    }

    public ObjectWithPrimitivesClass[] createArray(int x, int y, int length) {
        if (length < 3) {
            throw new IllegalArgumentException();
        }

        ObjectWithPrimitivesClass[] array = new ObjectWithPrimitivesClass[length];

        for (int i = 0; i < array.length; i++) {
            array[i] = new ObjectWithPrimitivesClass();
            array[i].x = x + i;
            array[i].y = y + i;
        }

        return array;
    }

    @SuppressWarnings({"ManualArrayCopy", "ForLoopReplaceableByForEach"})
    public ObjectWithPrimitivesClass[] copyArray(ObjectWithPrimitivesClass[] array) {
        if (array.length < 3) {
            throw new IllegalArgumentException();
        }

        ObjectWithPrimitivesClass[] copy = new ObjectWithPrimitivesClass[array.length];
        for (int i = 0; i < array.length; i++) {
            copy[i] = array[i];
        }

        for (int i = 0; i < array.length; i++) {
            array[i].x = -1;
            array[i].y = 1;
        }

        return copy;
    }

    public ObjectWithPrimitivesClass[] arrayWithSucc(int length) {
        ObjectWithPrimitivesClass[] array = new ObjectWithPrimitivesClass[length];

        if (length < 2) {
            return array;
        }

        array[0] = new ObjectWithPrimitivesClass();
        array[0].x = 2;
        array[0].y = array[0].x * 2;

        array[1] = new ObjectWithPrimitivesClassSucc();
        array[1].x = 3;

        return array;
    }

    public int objectArray(ObjectWithPrimitivesClass[] array, ObjectWithPrimitivesClass objectExample) {
        if (array.length != 2) {
            return -1;
        }
        array[0] = new ObjectWithPrimitivesClass();
        array[0].x = 5;
        array[1] = objectExample;
        if (array[0].x + array[1].x > 20) {
            return 1;
        }
        return 0;
    }

    public int arrayOfArrays(Object[] array) {
        UtMock.assume(array != null && array.length > 0);

        int sum = 0;

        for (Object o : array) {
            for (int j : (int[]) o) {
                UtMock.assume(((int[]) o).length > 2);
                UtMock.assume(j != 0);
                sum += j;
            }
        }

        return sum;
    }
}
