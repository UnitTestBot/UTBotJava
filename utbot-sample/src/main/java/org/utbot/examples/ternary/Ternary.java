package org.utbot.examples.ternary;

public class Ternary {
    public int max(int val1, int val2) {
        return val1 >= val2 ? val1 : val2;
    }

    public int simpleOperation(int exp1, int exp2) {
        int result = 12 > 10 ? ++exp1 : ++exp2;
        return result;
    }


    public int parse(String input) {
        int value = input == null || input.equals("") ? 0 : Integer.parseInt(input);
        return value;
    }

    public String stringExpr(int num) {
        return num > 10 ? "Number is greater than 10" :
                num > 5 ? "Number is greater than 5" : "Number is less than equal to 5";
    }

    public int minValue(int a, int b) {
        return (a < b) ? a : b;
    }

    public int subDelay(boolean flag) {
        return flag ? 100 : 0;
    }

    public int plusOrMinus(int num1, int num2) {
        return (num1 > num2) ? (num1 + num2) : (num1 - num2);
    }

    public int longTernary(int num1, int num2) {
        return num1 > num2 ? 1 : num1 == num2 ? 2 : 3;
    }

    public int veryLongTernary(int num1, int num2, int num3) {
        return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5;
    }

    public int intFunc(int num1, int num2) {
        return num1 > num2 ? intFunc1() : intFunc2();
    }

    public int minMax(int num1, int num2) {
        int a = num1 > num2 ?
                max(num1, num2) * 3
                : minValue(num1, num2) * 5;
        a += 80;
        a *= 5;
        return a;
    }

    public int ternaryInTheMiddle(int num1, int num2, int num3) {
        return max(num1 + 228, num2 > num3 ? num2 + 1 : num3 + 2) + 4;
    }

    public int twoIfsOneLine(int num1, int num2) {
        int a = 0;
        if (num1 > num2){a = (num1 - 10) > 0 ? 2 : 3;}
        else {a = num2 - 200;}
        return a;
    }

    private int intFunc1() {
        return 10;
    }

    private int intFunc2() {
        return 20;
    }
}
