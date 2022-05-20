package org.utbot.examples.wrappers;

public class ShortWrapper {
    Short primitiveToWrapper(short i) {
        Short a = i;
        if (a >= 0) {
            return (short) -a;
        } else {
            return a;
        }
    }

    short wrapperToPrimitive(Short i) {
        short a = i;
        if (a >= 0) {
            return (short) -a;
        } else {
            return a;
        }
    }

    int equality(short a, short b) {
        Short aWrapper = a;
        Short bWrapper = b;
        if (aWrapper.equals(bWrapper)) {
            if (aWrapper == bWrapper) {
                // if a == b and aWrapper == bWrapper than a,b should be in -128..127
                return 1;
            } else {
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
