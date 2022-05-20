package org.utbot.examples.recursion;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Recursion {
    public int factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException();
        }
        if (n == 0) {
            return 1;
        }
        return n * factorial(n - 1);
    }

    public int fib(int n) {
        if (n < 0) throw new IllegalArgumentException();
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib(n - 1) + fib(n - 2);
    }

    public int sum(int fst, int snd) {
        if (snd == 0) {
            return fst;
        }
        return sum(fst + (int) Math.signum(snd), snd - (int) Math.signum(snd));
    }

    public int pow(int a, int n) {
        if (n < 0) {
            throw new IllegalArgumentException();
        }
        if (n == 0) {
            return 1;
        }
        if (n % 2 == 1) {
            return pow(a, n - 1) * a;
        } else {
            int b = pow(a, n / 2);
            return b * b;
        }
    }

    @SuppressWarnings("InfiniteRecursion")
    public void infiniteRecursion(int i) {
        if (i > 10000) {
            throw new StackOverflowError();
        }
        infiniteRecursion(i + 1);
    }


    private int dfs(int v, List<List<Integer>> graph, List<Integer> colors, List<Integer> weight) {
        if (colors.get(v) == 1) {
            return 0;
        }
        colors.set(v, 1);

        int acc = weight.get(v);
        for (int i = 0; i < graph.get(v).size(); i++) {
            acc += dfs(graph.get(v).get(i), graph, colors, weight);
        }

        return acc;
    }

    public int vertexSum(int n) {
        if (n <= 10) {
            throw new IllegalArgumentException();
        }
        List<List<Integer>> to = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<Integer> weight = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            colors.add(0);
            weight.add(new Random().nextInt(n));
        }
        for (int i = 0; i < 5; i++) {
            to.add(new ArrayList<>());
        }
        to.get(0).add(1);
        to.get(0).add(2);
        to.get(1).add(3);
        to.get(2).add(4);
        to.get(4).add(0);

        return dfs(0, to, colors, weight);
    }

    public void recursionWithException(int n) {
        if (n < 42) {
            recursionWithException(n + 1);
        }
        if (n > 42) {
            recursionWithException(n - 1);
        }
        throw new IllegalArgumentException();
    }

    private void secondMethod(int n) {
        firstMethod(n);
    }

    public void firstMethod(int n) {
        if (n < 4) {
            return;
        }
        secondMethod(n - 1);
    }
}
