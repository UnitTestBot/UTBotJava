package org.utbot.examples.synthesis;

public class ComplexCounter {
    private int a;
    private int b;

    public ComplexCounter() {
        this.a = 0;
        this.b = 0;
    }

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }

    public void incrementA(int value) {
        if (value < 0) return;
        for (int i = 0; i < value; ++i) {
            this.a++;
        }
    }

    public void incrementB(int value) {
        if (value > 0) {
            this.b += value;
        }
    }

    @Override
    public String toString() {
        return "C";
    }
}
