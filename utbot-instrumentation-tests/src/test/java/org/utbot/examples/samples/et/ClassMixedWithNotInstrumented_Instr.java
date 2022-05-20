package org.utbot.examples.samples.et;

public class ClassMixedWithNotInstrumented_Instr {
    static public void a(int x) {
        ClassMixedWithNotInstrumented_Not_Instr.b(x);
    }

    public static void a_throws(int x) {
        if (x == 0) {
            throw new IllegalArgumentException();
        }
        ClassMixedWithNotInstrumented_Not_Instr.b_throws(x);
    }
}

// #1. a(2) B is not instrumented
// #2. a_throws(2) B is not instrumented
