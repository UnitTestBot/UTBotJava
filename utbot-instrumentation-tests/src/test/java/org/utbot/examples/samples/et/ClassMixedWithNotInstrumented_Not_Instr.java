package org.utbot.examples.samples.et;

public class ClassMixedWithNotInstrumented_Not_Instr {
    static public void b(int x) {
        if (x > 0) {
            ClassMixedWithNotInstrumented_Instr.a(x - 1);
            ClassMixedWithNotInstrumented_Instr.a(x - 1);
        }
    }

    public static void b_throws(int x) {
        if (x == 2) {
            try {
                ClassMixedWithNotInstrumented_Instr.a_throws(x - 1);
                ClassMixedWithNotInstrumented_Instr.a_throws(x - 1);
            } catch (Exception ignored) {

            }
        } else {
            ClassMixedWithNotInstrumented_Instr.a_throws(x - 1);
            ClassMixedWithNotInstrumented_Instr.a_throws(x - 1);
        }
    }
}

// #1. a(2) B is not instrumented
// #2. a_throws(2) B is not instrumented
