package org.utbot.examples.natives;

/**
 * A class with test cases containing calls to java.lang code
 * which uses static fields like System.out
 */
public class NativeExamples {

    public int findAndPrintSum(int a, int b) {
        int sum = a + b;
        System.out.println("Sum: " + sum);
        System.err.print(sum);
        return sum;
    }

    public double findSumWithMathRandom(double a) {
        double b = Math.random();
        return a + b;
    }
}
