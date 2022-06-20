package org.utbot.examples.postcondition;

public class PrimitivesContainer {
    private int i = 1;
    boolean bool = true;
//    String s = "1";
    char c = 1;
    float f = 1.0f;
    private double d = 1.0;
    byte b = 1;
    long l = 1;

    public int getInt() {
        return i;
    }

    public boolean getBool() {
        return bool;
    }

/*
    public String getString() {
        return s;
    }
*/

    public char getChar() {
        return c;
    }

    public float getFloat() {
        return f;
    }

    public double getDouble() {
        return d;
    }

    public byte getByte() {
        return b;
    }

    public long getLong() {
        return l;
    }

    public int getFixedInt() {
        return 1;
    }

    public boolean getFixedBool() {
        bool = true;
        return bool;
    }

/*
    public String getFixedString() {
        return "1";
    }
*/

    public char getFixedChar() {
        return '1';
    }

    public float getFixedFloat() {
        return 1.0f;
    }

    public double getFixedDouble() {
        return 1.0;
    }

    public byte getFixedByte() {
        return '1';
    }

    public long getFixedLong() {
        return 1;
    }
}
