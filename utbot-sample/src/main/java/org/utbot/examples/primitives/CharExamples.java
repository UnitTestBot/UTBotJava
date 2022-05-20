package org.utbot.examples.primitives;

public class CharExamples {
    public int charDiv(char a, char b) {
        return a / b;
    }

    public int charNeg(char c) {
        int i = -c;
        if (i < -50000) {
            return 1;
        }
        return 2;
    }

    public int byteToChar(byte b) {
        char c = (char) b;
        switch (c) {
            case '\uFFFF':
                return -1;
            case '\uFF80':
                return -128;
            case '\u0000':
                return 0;
            case '\u007F':
                return 127;
            default:
                return 200;
        }
    }

    public CharAsFieldObject updateObject(CharAsFieldObject obj, int i) {
        obj.c = (char) i;
        if (obj.c > 50000) {
            return obj;
        }
        CharAsFieldObject newOne = new CharAsFieldObject();
        newOne.c = '\u0444';
        return newOne;
    }
}