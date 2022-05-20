package org.utbot.examples.wrappers;

public class CharacterWrapper {
    public Character primitiveToWrapper(char i) {
        Character a = i;
        if (a >= 100) {
            return a;
        } else {
            return (char) (a + 100);
        }
    }

    public char wrapperToPrimitive(Character i) {
        char a = i;
        if (a >= 100) {
            return a;
        } else {
            return (char) (a + 100);
        }
    }

    int equality(char a, char b) {
        Character aWrapper = a;
        Character bWrapper = b;
        if (aWrapper.equals(bWrapper)) {
            if (aWrapper == bWrapper) {
                // if a == b and aWrapper == bWrapper than a,b should be less than 127
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
