package org.utbot.examples.algorithms;

public class ReturnExample {

    public int compare(int a, int b) {
        int c = a + b;
        if (a < 0 || b < 0)
            return a;
        if (b == 10)
            return c;
        if (a > b)
            return b;
        if (a < b)
            return a;
        return c;
    }

    public char compareChars(char a, char b, int n) {
        if (n < 1)
            return ' ';
        for (int i = 0; i < n; i++) {
            if (Character.toChars(i)[0] == a) {
                return b;
            }
            if (Character.toChars(i)[0] == b) {
                return a;
            }
        }
        return a;
    }


    public void innerVoidCompareChars(char a, char b, int n) {
        compareChars(a, b, n);
    }

    public char innerReturnCompareChars(char a, char b, int n) {
        return compareChars(a, b, n);
    }

    public void innerVoidCallCompare(int a, int b) {
        compare(a, b);
    }

    public int innerReturnCallCompare(int a, int b) {
        return compare(a, b);
    }
}
