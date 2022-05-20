package org.utbot.instrumentation.samples.mock;


@SuppressWarnings("unused")
public class ClassForMock {
    int x = 10;
    String s = "123";

    public ClassForMock() {
        x = 15;
    }

    public ClassForMock(String s) {
        this.s = s;
    }

    public int getX() {
        return x;
    }

    public static String getString() {
        return "string";
    }

    public String complicatedMethod(int x, int y, int z, ClassForMock classForMock) {
        if (classForMock == this) {
            return "equals";
        }
        if (x + y == z) {
            return "x + y == z";
        }
        return "none";
    }

    public int provideInt() {
        return x;
    }

    public boolean check(ClassForMock classForMock) {
        return provideInt() == classForMock.provideInt();
    }
}
