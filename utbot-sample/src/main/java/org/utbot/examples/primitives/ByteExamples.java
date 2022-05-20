package org.utbot.examples.primitives;

public class ByteExamples {
    public int negByte(byte b) {
        int a = -b;
        if (a < b) {
            return 0;
        } else {
            return 1;
        }
    }

    public int negConstByte(byte b) {
        byte a = 10;
        int c = -a;
        if (a > b && c < b) {
            return 0;
        } else {
            return 1;
        }
    }

    public int sumTwoBytes(byte a, byte b) {
        int s = a + b;
        if (s > Byte.MAX_VALUE) {
            return 1;
        }
        if (s < Byte.MIN_VALUE) {
            return 2;
        }
        return 3;
    }
}
