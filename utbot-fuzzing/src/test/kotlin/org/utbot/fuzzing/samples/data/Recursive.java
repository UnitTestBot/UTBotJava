package org.utbot.fuzzing.samples.data;

@SuppressWarnings("unused")
public class Recursive {

    private final int a;

    public Recursive(int a) {
        this.a = a;
    }

    public Recursive(Recursive r) {
        this.a = r.a;
    }

    public int val() {
        return a;
    }
}
