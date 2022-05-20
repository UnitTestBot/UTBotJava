package org.utbot.examples.arrays;

@SuppressWarnings("DuplicatedCode")
public class PrimitiveArrays {
    public int[] defaultIntValues() {
        int[] array = new int[3];
        //noinspection ConstantConditions,IfStatementWithIdenticalBranches
        if (array[1] != 0) {
            return array;
        }
        return array;
    }

    public double[] defaultDoubleValues() {
        double[] array = new double[3];
        //noinspection ConstantConditions,IfStatementWithIdenticalBranches
        if (array[1] != 0.0) {
            return array;
        }
        return array;
    }

    public boolean[] defaultBooleanValues() {
        boolean[] array = new boolean[3];
        //noinspection ConstantConditions,IfStatementWithIdenticalBranches
        if (!array[1]) {
            return array;
        }
        return array;
    }

    public byte byteArray(byte[] a, byte x) {
        if (a.length != 2) {
            return -1;
        }
        a[0] = 5;
        a[1] = x;
        if (a[0] + a[1] > 20) {
            return 1;
        }
        return 0;
    }

    public byte shortArray(short[] a, short x) {
        if (a.length != 2) {
            return -1;
        }
        a[0] = 5;
        a[1] = x;
        if (a[0] + a[1] > 20) {
            return 1;
        }
        return 0;
    }

    public byte charArray(char[] a, char x) {
        if (a.length != 2) {
            return -1;
        }
        a[0] = 5;
        a[1] = x;
        if (a[0] + a[1] > 20) {
            return 1;
        }
        return 0;
    }

    public byte intArray(int[] a, int x) {
        if (a.length != 2) {
            return -1;
        }
        a[0] = 5;
        a[1] = x;
        if (a[0] + a[1] > 20) {
            return 1;
        }
        return 0;
    }

    public long longArray(long[] a, long x) {
        if (a.length != 2) {
            return -1;
        }
        a[0] = 5;
        a[1] = x;
        if (a[0] + a[1] > 20) {
            return 1;
        }
        return 0;
    }

    public float floatArray(float[] a, float x) {
        if (a.length != 2) {
            return -1;
        }
        a[0] = 5;
        a[1] = x;
        if (a[0] + a[1] > 20) {
            return 1;
        }
        return 0;
    }

    public double doubleArray(double[] a, double x) {
        if (a.length != 2) {
            return -1;
        }
        a[0] = 5;
        a[1] = x;
        if (a[0] + a[1] > 20) {
            return 1;
        }
        return 0;
    }

    public int booleanArray(boolean[] a, boolean x, boolean y) {
        if (a.length != 2) {
            return -1;
        }
        a[0] = x;
        a[1] = y;
        if (a[0] ^ a[1]) {
            return 1;
        }
        return 0;
    }

    public byte byteSizeAndIndex(byte[] a, byte x) {
        if (a == null || a.length <= x || x < 1) {
            return -1;
        }
        byte[] b = new byte[x];
        b[0] = 5;
        a[x] = x;
        if (b[0] + a[x] > 7) {
            return 1;
        }
        return 0;
    }

    public byte shortSizeAndIndex(short[] a, short x) {
        if (a == null || a.length <= x || x < 1) {
            return -1;
        }
        short[] b = new short[x];
        b[0] = 5;
        a[x] = x;
        if (b[0] + a[x] > 7) {
            return 1;
        }
        return 0;
    }

    public byte charSizeAndIndex(char[] a, char x) {
        if (a == null || a.length <= x || x < 1) {
            return -1;
        }
        char[] b = new char[x];
        b[0] = 5;
        a[x] = x;
        if (b[0] + a[x] > 7) {
            return 1;
        }
        return 0;
    }
}
