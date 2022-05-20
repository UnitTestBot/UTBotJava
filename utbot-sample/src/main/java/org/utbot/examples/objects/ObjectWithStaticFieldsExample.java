package org.utbot.examples.objects;


import org.utbot.api.mock.UtMock;
import org.utbot.examples.primitives.IntExamples;

public class ObjectWithStaticFieldsExample {
    public ObjectWithStaticFieldsClass readFromStaticArray(ObjectWithStaticFieldsClass objectExample) {
        if (ObjectWithStaticFieldsClass.staticArrayValue.length < 5) {
            throw new IllegalArgumentException();
        }
        if (ObjectWithStaticFieldsClass.staticArrayValue[1] != 1 || ObjectWithStaticFieldsClass.staticArrayValue[2] != 2) {
            throw new IllegalArgumentException();
        }

        objectExample.x = ObjectWithStaticFieldsClass.staticArrayValue[1];
        objectExample.y = ObjectWithStaticFieldsClass.staticArrayValue[2];
        return objectExample;
    }

    public ObjectWithStaticFieldsClass setStaticField(ObjectWithStaticFieldsClass objectExample) {
        if (objectExample.x < 100 || objectExample.y < 150) {
            throw new IllegalArgumentException();
        }
        ObjectWithStaticFieldsClass.staticValue = objectExample.x * objectExample.y;
        objectExample.x = ObjectWithStaticFieldsClass.staticValue;
        return objectExample;
    }

    public ObjectWithStaticFieldsClass getStaticField(ObjectWithStaticFieldsClass objectExample) {
        objectExample.x = ObjectWithStaticFieldsClass.staticValue;
        if (objectExample.x != 3) {
            throw new RuntimeException();
        }
        return objectExample;
    }

    public int getStaticFieldWithDefaultValue() {
        return ObjectWithStaticFieldsClass.defaultValue;
    }

    public int staticFieldInInvoke() {
        ObjectWithStaticFieldsClass.staticValue = ObjectWithStaticFieldsClass.defaultValue;
        return new IntExamples().max(ObjectWithStaticFieldsClass.defaultValue, ObjectWithStaticFieldsClass.defaultValue);
    }

    public int staticFieldArrayMax() {
        ObjectWithStaticFieldsClass.staticArrayValue = new int[ObjectWithStaticFieldsClass.defaultValue];
        for (int i = 0; i < ObjectWithStaticFieldsClass.defaultValue; i++) {
            ObjectWithStaticFieldsClass.staticArrayValue[i] = ObjectWithStaticFieldsClass.defaultValue + i;
        }
        int max = ObjectWithStaticFieldsClass.staticArrayValue[0];
        for (int value : ObjectWithStaticFieldsClass.staticArrayValue) {
            if (max < value) {
                max = value;
            }
        }
        return max;
    }

    public double initializedArrayWithCycle(int n) {
        if (n < 0) {
            return Double.NEGATIVE_INFINITY;
        } else {
            double accum = 1.0;
            for (int i = 1; i < n; i++) {
                accum *= i;
            }
            return accum * ObjectWithStaticFieldsClass.initializedArray[n];
        }
    }

    public int bigStaticArray() {
        return ObjectWithStaticFieldsClass.elevenElements[10];
    }

    public static void modifyStatic() {
        if (ObjectWithStaticFieldsClass.staticValue == 41) {
            ObjectWithStaticFieldsClass.staticValue = ObjectWithStaticFieldsClass.staticValue + 1;
        }
    }

    public int resetNonFinalFields() {
        if (ObjectWithStaticFieldsClass.defaultValue == 42) {
            ObjectWithStaticFieldsClass.defaultValue++;
            return ObjectWithStaticFieldsClass.defaultValue;
        }
        UtMock.assume(ObjectWithStaticFieldsClass.defaultValue != 43);
        return ObjectWithStaticFieldsClass.defaultValue;
    }
}
