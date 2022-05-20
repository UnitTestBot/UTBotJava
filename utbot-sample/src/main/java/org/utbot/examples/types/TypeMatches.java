package org.utbot.examples.types;

public class TypeMatches {
    public double compareDoubleByte(double a, byte b) {
        if (a < b) {
            return 0;
        } else {
            return 1;
        }
    }

    public short compareShortLong(short a, long b) {
        if (a < b) {
            return 0;
        } else {
            return 1;
        }
    }

    public float compareFloatDouble(float a, double b) {
        if (a < b) {
            return 0;
        } else {
            return 1;
        }
    }

    public int sumByteAndShort(byte a, short b) {
        int s = a + b;
        if (s > Short.MAX_VALUE) {
            return 1;
        }
        if (s < Short.MIN_VALUE) {
            return 2;
        }
        return 3;
    }

    public int sumShortAndChar(short a, char b) {
        int s = a + b;
        if (s > Character.MAX_VALUE) {
            return 1;
        }
        if (s < Character.MIN_VALUE) {
            return 2;
        }
        return 3;
    }
}