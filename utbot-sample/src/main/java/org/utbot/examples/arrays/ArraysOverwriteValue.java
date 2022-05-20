package org.utbot.examples.arrays;

import org.utbot.examples.objects.ObjectWithPrimitivesClass;

public class ArraysOverwriteValue {
    public byte byteArray(byte[] a) {
        if (a.length == 0) {
            return 1;
        }
        if (a[0] != 0) {
            return 2;
        }
        a[0] = 1;
        return 3;
    }

    public byte shortArray(short[] a) {
        if (a.length == 0) {
            return 1;
        }
        if (a[0] != 0) {
            return 2;
        }
        a[0] = 1;
        return 3;
    }

    public char charArray(char[] a) {
        if (a.length == 0) {
            return 1;
        }
        if (a[0] != 0) {
            return 2;
        }
        a[0] = 1;
        return 3;
    }

    public byte intArray(int[] a) {
        if (a.length == 0) {
            return 1;
        }
        if (a[0] != 0) {
            return 2;
        }
        a[0] = 1;
        return 3;
    }

    public long longArray(long[] a) {
        if (a.length == 0) {
            return 1;
        }
        if (a[0] != 0) {
            return 2;
        }
        a[0] = 1;
        return 3;
    }

    public float floatArray(float[] a) {
        if (a.length == 0) {
            return 1;
        }
        if (a[0] == a[0]) {
            return 2;
        }
        a[0] = 1.0f;
        return 3;
    }

    public double doubleArray(double[] a) {
        if (a.length == 0) {
            return 1;
        }
        if (a[0] == a[0]) {
            return 2;
        }
        a[0] = 1.0d;
        return 3;
    }

    public int booleanArray(boolean[] a) {
        if (a.length == 0) {
            return 1;
        }
        if (a[0]) {
            return 2;
        }
        a[0] = true;
        return 3;
    }

    public int objectArray(ObjectWithPrimitivesClass[] a) {
        if (a.length == 0) {
            return 1;
        }
        if (a[0] == null) {
            return 2;
        }
        a[0] = null;
        return 3;
    }
}
