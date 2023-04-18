package org.utbot.examples.math;

public class OverflowExamples {
    public byte byteAddOverflow(byte x, byte y) {
        return (byte) (x + y);
    }
    public byte byteWithIntOverflow(byte x, int y) {
        return (byte) (x + y);
    }
    public byte byteMulOverflow(byte x, byte y) {
        return (byte) (x * y);
    }
    public byte byteSubOverflow(byte x, byte y) {
        return (byte) (x - y);
    }

    public short shortAddOverflow(short x, short y) {
        return (short) (x + y);
    }
    public short shortMulOverflow(short x, short y) {
        return (short) (x * y);
    }
    public short shortSubOverflow(short x, short y) {
        return (short) (x - y);
    }

    public int intAddOverflow(int x, int y) {
        return x + y;
    }
    public int intSubOverflow(int x, int y) {
        return x - y;
    }
    public int intMulOverflow(int x, int y) {
        return x * y;
    }

    public int intCubeOverflow(int x) {
        return x * x * x;
    }

    public long longAddOverflow(long x, long y) {
        return x + y;
    }
    public long longSubOverflow(long x, long y) {
        return x - y;
    }
    public long longMulOverflow(long x, long y) {
        return x * y;
    }

    public int incOverflow(int x) {
        return x + 1;
    }

    public int intOverflow(int x, int y) {
        if (x * x * x > 0) { // possible overflow
            if (x > 0 && y == 10) {
                return 1;
            }
        } else {
            if (x > 0 && y == 20) {
                return 2;
            }
        }
        return 0;
    }
}