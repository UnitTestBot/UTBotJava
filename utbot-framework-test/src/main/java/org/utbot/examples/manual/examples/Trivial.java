package org.utbot.examples.manual.examples;

public class Trivial {
    public int aMethod(int a) {
        String s = "a";
        if (a > 1) {
            return s.length();
        }
        return s.length() + 1;
    }
}
