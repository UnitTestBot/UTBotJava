package org.utbot.examples.wrappers;

public class LongWrapper {
    Long primitiveToWrapper(long i) {
        Long a = i;
        if (a >= 0) {
            return -a;
        } else {
            return a;
        }
    }

    long wrapperToPrimitive(Long i) {
        long a = i;
        if (a >= 0) {
            return -a;
        } else {
            return a;
        }
    }

    int equality(long a, long b) {
        Long aWrapper = a;
        Long bWrapper = b;
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

    Long parseLong(String line) {
        return Long.parseLong(line, 16);
    }
}
