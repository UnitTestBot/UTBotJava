package org.utbot.examples.samples.et;

public class ClassSimpleCatch {
    public static void A() {
        int x = B();
    }

    public static int B() {
        return 1;
    }

    public static void A_catches() {
        try {
            int x = B_throws();
        } catch (IndexOutOfBoundsException ignored) {
            String s = "catched!";
        }
    }

    public static void A_doesNotCatch() {
        int x = B_throws();
    }

    public static void A_catchesWrongException() {
        try {
            int x = B_throws();
        } catch (IllegalArgumentException ignored) {
            String s = "catched!";
        }
    }

    public static int B_throws() {
        int[] a = new int[15];
        a[20] = 1;
        return a[20];
    }


}

// #1. A
// #2. A_catches
// #3. A_doesNotCatch
// #4. A_catchesWrongException
