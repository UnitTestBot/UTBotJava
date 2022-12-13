package org.utbot.fuzzing.samples;

@SuppressWarnings("unused")
public class Enums {

    public int test(int x) {
        switch(x) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
        }
        return 0;
    }

    public int test3(int x) {
        switch(x) {
            case 11:
                return 1;
            case 25:
                return 2;
            case 32:
                return 3;
        }
        return 0;
    }

    public int test4(S x) {
        switch(x) {
            case A:
                return 1;
            case B:
                return 2;
            case C:
                return 3;
        }
        return 0;
    }

    public enum S {
        A, B, C
    }

    public int test2(int x) {
        int a = 0;
        switch(x) {
            case 1:
                a = 1;
                break;
            case 2:
                a = 2;
                break;
            case 3:
                a = 3;
        }
        return a;
    }

}
