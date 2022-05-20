package org.utbot.instrumentation.samples.mock;

@SuppressWarnings("unused")
public class ClassForMockInstanceMethods {
    int x = 239;

    public ClassForMockInstanceMethods() {
    }

    int testI() {
        return x;
    }

    long testL() {
        return 1L;
    }

    SomeClass testObject() {
        return new SomeClass(1337);
    }

    char testChar() {
        return 'z';
    }

    float testFloat() {
        return 1.0f;
    }

    byte testByte() {
        return (byte) 1;
    }

    double testDouble() {
        return 1.0;
    }

    int[] testArray() {
        return new int[]{-1, -1};
    }

    short testShort() {
        return (short) 1;
    }

    boolean testBoolean() {
        return false;
    }

    void testVoid() {
        x += 10;
    }

    public static class SomeClass {
        int x;

        public SomeClass(int x) {
            this.x = x;
        }
    }
}
