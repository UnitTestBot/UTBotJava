package org.utbot.examples.wrappers;

public class ByteWrapper {
    public Byte primitiveToWrapper(byte i) {
        Byte a = i;
        if (a >= 0) {
            return (byte) -a;
        } else {
            return a;
        }
    }

    public byte wrapperToPrimitive(Byte i) {
        byte a = i;
        if (a >= 0) {
            return (byte) -a;
        } else {
            return a;
        }
    }

    int equality(byte a, byte b) {
        Byte aWrapper = a;
        Byte bWrapper = b;
        if (aWrapper.equals(bWrapper)) {
            if (aWrapper == bWrapper) {
                // all the byte instances are absolutely equal
                return 1;
            } else {
                // unreachable branch
                return 2;
            }
        } else {
            if (aWrapper == bWrapper) {
                // unreachable branch
                return 3;
            } else {
                return 4;
            }
        }
    }
}
