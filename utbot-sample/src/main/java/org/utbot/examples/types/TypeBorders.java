package org.utbot.examples.types;

@SuppressWarnings("All")
public class TypeBorders {

    public int byteBorder(byte x) {
        if (x > Byte.MAX_VALUE) {
            return 0;
        } else if (x < Byte.MIN_VALUE) {
            return 1;
        } else if (x == Byte.MAX_VALUE) {
            return 2;
        } else if (x == Byte.MIN_VALUE) {
            return 3;
        } else {
            return 4;
        }
    }

    public int shortBorder(short x) {
        if (x > Short.MAX_VALUE) {
            return 0;
        } else if (x < Short.MIN_VALUE) {
            return 1;
        } else if (x == Short.MAX_VALUE) {
            return 2;
        } else if (x == Short.MIN_VALUE) {
            return 3;
        } else {
            return 4;
        }
    }

    public int charBorder(char x) {
        if (x > Character.MAX_VALUE) {
            return 0;
        } else if (x < Character.MIN_VALUE) {
            return 1;
        } else if (x == Character.MAX_VALUE) {
            return 2;
        } else if (x == Character.MIN_VALUE) {
            return 3;
        } else {
            return 4;
        }
    }

    public int intBorder(int x) {
        if (x > Integer.MAX_VALUE) {
            return 0;
        } else if (x < Integer.MIN_VALUE) {
            return 1;
        } else if (x == Integer.MAX_VALUE) {
            return 2;
        } else if (x == Integer.MIN_VALUE) {
            return 3;
        } else {
            return 4;
        }
    }

    public int longBorder(long x) {
        if (x > Long.MAX_VALUE) {
            return 0;
        }
        if (x < Long.MIN_VALUE) {
            return 1;
        } else if (x == Long.MAX_VALUE) {
            return 2;
        } else if (x == Long.MIN_VALUE) {
            return 3;
        } else {
            return 4;
        }
    }

    public int unreachableByteValue(byte x) {
        int y = x;
        if (y > 200) {
            return 1;
        }
        return 0;
    }
}