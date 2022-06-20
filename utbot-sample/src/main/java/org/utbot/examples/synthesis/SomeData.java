package org.utbot.examples.synthesis;

public class SomeData {
    private int data1 = 0;
    private int data2 = -1;

    void adjustData1(boolean isPositive, int delta) {
        if (isPositive) {
            data1 += delta;
        } else {
            data1 -= delta;
        }
    }

    void adjustData2(boolean isPositive) {
        if (isPositive) {
            data2++;
            data1--;
        } else {
            data2--;
            data1++;
        }
    }

    boolean bothNumbersArePositive() {
        if (data1 > 0 && data2 > 0) {
            return true;
        } else {
            return false;
        }
    }
}