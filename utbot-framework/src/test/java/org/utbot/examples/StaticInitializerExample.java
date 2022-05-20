package org.utbot.examples;

public class StaticInitializerExample {
    public Boolean positive(int i) {
        if (i > 0) {
            return true;
        }
        return false;
    }

    public Boolean negative(int i) {
        if (i < 0) {
            return true;
        }
        return false;
    }
}