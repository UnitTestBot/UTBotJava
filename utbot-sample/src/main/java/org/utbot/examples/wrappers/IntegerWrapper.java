package org.utbot.examples.wrappers;

public class IntegerWrapper {

    public Integer primitiveToWrapper(int i) {
        Integer a = i;
        if (a >= 0) {
            return -a;
        } else {
            return a;
        }
    }

    public int wrapperToPrimitive(Integer i) {
        int a = i;
        if (a >= 0) {
            return -a;
        } else {
            return a;
        }
    }

    public int bitCount(Integer i) {
        if (Integer.bitCount(i) == 5) {
            return 1;
        } else {
            return 0;
        }
    }

    public int numberOfZeros(Integer i) {
        if (Integer.numberOfLeadingZeros(i) < 5 && Integer.numberOfTrailingZeros(i) < 5) {
            return 1;
        } else {
            return 0;
        }
    }

    public int equality(int a, int b) {
        Integer aWrapper = a;
        Integer bWrapper = b;
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
