package org.utbot.examples.types;

public class CastExamples {
    public byte longToByte(long a, long b) {
        byte castA = (byte) a;
        byte castB = (byte) b;
        byte c = (byte) (castA + castB);
        if (c > 10) {
            return c;
        }
        return (byte) (castA > castB ? -1 : 0);
    }

    public long shortToLong(short a, short b) {
        long c = (long) a + (long) b;
        if (c > 10) {
            return c;
        }
        return (long) a > (long) b ? -1 : 0;
    }

    public double floatToDouble(float a, float b) {
        double c = (double) a + (double) b;
        if (c > Float.MAX_VALUE) {
            return 2;
        }
        if (c > 10) {
            return 1;
        }
        return (double) a > (double) b ? -1 : 0;
    }

    public float doubleToFloatArray(double x) {
        float[] a = new float[1];
        a[0] = (float) x + 5;
        if (a[0] > 20) {
            return 1;
        }
        return 0;
    }

    public int floatToInt(float x) {
        if (x < 0) {
            if ((int) x < 0) {
                return 1;
            }
            return 2; // smth small to int zero
        }
        return 3;
    }

    public char shortToChar(short a, short b) {
        char c = (char) ((char) a + (char) b);
        if (c > 10) {
            return c;
        }
        return (char) ((char) a > (char) b ? -1 : 0);
    }
}