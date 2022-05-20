package org.utbot.examples.primitives;

import java.util.ArrayList;
import java.util.List;

public class IntExamples {
    public static boolean isInteger(String value) {
        try {
            return (Integer.valueOf(value) != null);
        } catch (NumberFormatException e) {
            return Boolean.FALSE;
        }
    }

    public int max(int x, int y) {
        if (x > y) {
            return x;
        } else {
            return y;
        }
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    public int preferableLt(int x) {
        if (x < 42) {
            return x;
        }
        return x;
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    public int preferableLe(int x) {
        if (x <= 42) {
            return x;
        }
        return x;
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    public int preferableGe(int x) {
        if (x >= 42) {
            return x;
        }
        return x;
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    public int preferableGt(int x) {
        if (x > 42) {
            return x;
        }
        return x;
    }

    public int complexCompare(int a, int b) {
        int c = 11;
        if ((a < b) && (b < c)) {
            return 0;
        }
        if ((a < b) && (b > c)) {
            return 1;
        }
        if ((a == b) && (b == c)) {
            return 3;
        }
        return 6;
    }

    public int contains(int a, int b){
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        if (list.contains(a)) {
            return a;
        }else return b;
    }

    public int containsCompare(int a, int b){
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        if (list.contains(a) && b > a) {
            return a;
        }else return b;
    }

    public int booleanIfGreater(int a, int b){
        boolean res = a > b;
        if (res) {
            return a;
        }else return b;
    }

    public int booleanIf(int a, int b){
        if (true) {
            return a;
        }else return b;
    }

    public int newComplexCompare(int a, int b) {
        int c = 11;
        int d = 20;
        if ((a < b) && (a < c && b < d)) {
            return 0;
        }
        if ((a < b) && (a > c && b > d)) {
            return 1;
        }
        if ((a == b) && (b == c)) {
            return 3;
        }
        return 6;
    }
    public int newComplexCompareOr(int a, int b) {
        int c = 11;
        int d = 20;
        if (((a < b)) || (a < c && b < d)) {
            return 0;
        }
        if ((a < b)  || b > d) {
            return 1;
        }
        if ((a == b) && (b == c)) {
            return 3;
        }
        return 6;
    }

    public int complexCondition(int a, int b) {
        int c = 10;
        int d = a + b + c;
        int f = b + c;
        int k = f + 12;

        if ((f < k) && (k < d)) {
            return 1;
        } else {
            return 0;
        }
    }

    public boolean orderCheck(int first, int second, int third) {
        return first < second && second < third;
    }

    public boolean orderCheckWithMethods(int first, int second, int third) {
        return orderCheckInt(first, second) && orderCheckInt(second, third);
    }

    private boolean orderCheckInt(int first, int second) {
        return first < second;
    }
}
