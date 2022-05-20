package org.utbot.instrumentation.samples.mock;

@SuppressWarnings("unused")
public class ClassForMockStaticMethods {
    static int x = 239;

    static int testI() {
        return x;
    }

    static long testL() {
        return 1L;
    }

    static SomeClass2 testObject() {
        return new SomeClass2(1337);
    }

    static char testChar() {
        return 'z';
    }

    static float testFloat() {
        return 1.0f;
    }

    static byte testByte() {
        return (byte) 1;
    }

    static double testDouble() {
        return 1.0;
    }

    static int[] testArray() {
        return new int[]{-1, -1};
    }

    static short testShort() {
        return (short) 1;
    }

    static boolean testBoolean() {
        return false;
    }

    static void testVoid() {
        x += 10;
    }

    public static class SomeClass2 {
        int x;

        public SomeClass2(int x) {
            this.x = x;
        }
    }
}