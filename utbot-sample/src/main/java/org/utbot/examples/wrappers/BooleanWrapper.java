package org.utbot.examples.wrappers;

@SuppressWarnings("ConstantConditions")
public class BooleanWrapper {
    public Boolean primitiveToWrapper(boolean x) {
        Boolean a = x;
        if (a) {
            return a;
        } else {
            return !a;
        }
    }

    public boolean wrapperToPrimitive(Boolean x) {
        boolean a = x;
        if (a) {
            return a;
        } else {
            return !a;
        }
    }

    int equality(boolean a, boolean b) {
        Boolean aWrapper = a;
        Boolean bWrapper = b;
        if (aWrapper.equals(bWrapper)) {
            if (aWrapper == bWrapper) {
                // Boolean instances got from boxing are the same
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
