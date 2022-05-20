package org.utbot.examples.invokes;

/**
 * Covers special and virtual invokes
 */
public class InvokeExample {
    private int mult(int a, int b) {
        return a * b;
    }

    private int half(int a) {
        return a / 2;
    }

    public int simpleFormula(int fst, int snd) {
        if (fst < 100 || snd < 100) {
            throw new IllegalArgumentException();
        }

        int x = fst + 5;
        int y = half(snd);

        return mult(x, y);
    }

    private InvokeClass initialize(int value) {
        InvokeClass objectValue = new InvokeClass();
        objectValue.value = value;
        return objectValue;
    }

    public InvokeClass createObjectFromValue(int value) {
        if (value == 0) {
            value = 1;
        }
        InvokeClass objectValue = initialize(value);
        return objectValue;
    }

    private void changeValue(InvokeClass objectValue, int value) {
        objectValue.value = value;
    }

    public InvokeClass changeObjectValueByMethod(InvokeClass objectValue) {
        objectValue.value = 1;
        changeValue(objectValue, 4);
        return objectValue;
    }

    private int getFive() {
        return 5;
    }

    private int getTwo() {
        return 2;
    }

    public InvokeClass particularValue(InvokeClass invokeObject) {
        if (invokeObject.value < 0) {
            throw new IllegalArgumentException();
        }
        int x = getFive() * getTwo();
        int y = getFive() / getTwo();

        invokeObject.value = x + y;
        return invokeObject;
    }

    private InvokeClass getNull() {
        return null;
    }

    public InvokeClass getNullOrValue(InvokeClass invokeObject) {
        if (invokeObject.value < 100) {
            return getNull();
        }
        invokeObject.value = getFive();
        return invokeObject;
    }

    private int abs(int value) {
        if (value < 0) {
            if (value == Integer.MIN_VALUE) {
                return 0;
            }
            return mult(-1, value);
        }
        return value;
    }


    public int constraintsFromOutside(int value) {
        if (abs(value) < 0) {
            throw new IllegalArgumentException();
        }
        return abs(value);
    }

    private int helper(int value) {
        if (value < 0) {
            return -1;
        }
        return 1;
    }

    public int constraintsFromInside(int value) {
        if (value < 0) {
            if (value == Integer.MIN_VALUE) {
                value = 0;
            } else {
                value = -value;
            }
        }
        return helper(value);
    }

    public InvokeClass alwaysNPE(InvokeClass invokeObject) {
        if (invokeObject.value == 0) {
            invokeObject = getNull();
            invokeObject.value = 0;
            return invokeObject;
        } else if (invokeObject.value > 0) {
            invokeObject = getNull();
            invokeObject.value = 1;
            return invokeObject;
        } else {
            invokeObject = getNull();
            invokeObject.value = -1;
            return invokeObject;
        }
    }

    private InvokeClass nestedMethodWithException(InvokeClass invokeObject) {
        if (invokeObject.value < 0) {
            throw new IllegalArgumentException();
        }
        return invokeObject;
    }

    public InvokeClass exceptionInNestedMethod(InvokeClass invokeObject, int value) {
        invokeObject.value = value;
        return nestedMethodWithException(invokeObject);
    }

    public InvokeClass fewNestedException(InvokeClass invokeObject, int value) {
        invokeObject.value = value;
        invokeObject = firstLevelWithException(invokeObject);
        return invokeObject;
    }

    private InvokeClass firstLevelWithException(InvokeClass invokeObject) {
        if (invokeObject.value < 10) {
            throw new IllegalArgumentException();
        }
        return secondLevelWithException(invokeObject);
    }

    private InvokeClass secondLevelWithException(InvokeClass invokeObject) {
        if (invokeObject.value < 100) {
            throw new IllegalArgumentException();
        }
        return thirdLevelWithException(invokeObject);
    }

    private InvokeClass thirdLevelWithException(InvokeClass invokeObject) {
        if (invokeObject.value < 10000) {
            throw new IllegalArgumentException();
        }
        return invokeObject;
    }

    public int divBy(InvokeClass invokeObject, int den) {
        if (invokeObject.value < 1000) {
            throw new IllegalArgumentException();
        }
        return invokeObject.divBy(den);
    }

    public InvokeClass updateValue(InvokeClass invokeObject, int value) {
        if (invokeObject.value > 0) {
            return invokeObject;
        }
        if (value > 0) {
            invokeObject.updateValue(value);
            if (invokeObject.value != value) {
                throw new RuntimeException(); // unreachable branch
            } else {
                return invokeObject;
            }
        }
        throw new IllegalArgumentException();
    }

    public int nullAsParameter(int den) {
        return divBy(null, den);
    }

    public int[] changeArrayWithAssignFromMethod(int[] array) {
        return changeAndReturnArray(array, 5);
    }

    private int[] changeAndReturnArray(int[] array, int diff) {
        int[] updatedArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            updatedArray[i] = array[i] + diff;
        }
        return updatedArray;
    }

    public int[] changeArrayByMethod(int[] array) {
        changeArrayValues(array, 5);
        return array;
    }

    private void changeArrayValues(int[] array, int diff) {
        for (int i = 0; i < array.length; i++) {
            array[i] += diff;
        }
    }

    public int[] arrayCopyExample(int[] array) {
        if (array.length < 3) {
            throw new IllegalArgumentException();
        }

        if (array[0] <= array[1] || array[1] <= array[2]) {
            return null;
        }

        int[] dst = new int[array.length];

        arrayCopy(array, 0, dst, 0, array.length);

        return dst;
    }


    private void arrayCopy(int[] src, int srcPos, int[] dst, int dstPos, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstPos + i] = src[srcPos + i];
        }
    }

    public int updateValues(InvokeClass fst, InvokeClass snd) {
        changeTwoObjects(fst, snd);
        if (fst.value == 1 && snd.value == 2) {
            return 1;
        }
        throw new RuntimeException();
    }

    private void changeTwoObjects(InvokeClass fst, InvokeClass snd) {
        fst.value = 1;
        snd.value = 2;
    }
}
