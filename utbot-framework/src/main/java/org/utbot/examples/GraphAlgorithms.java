package org.utbot.examples;

import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

public class GraphAlgorithms<T> {
//    public GraphAlgorithms(T lol) {
//        this.lol = lol;
//    }

    private class GraphAlgorithms1<R> {
        T a;
        R b;
        int c;

        GraphAlgorithms1(T a, R b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }
    public static GraphAlgorithms<Integer> GRAPH = new GraphAlgorithms<Integer>(1, null);
    public GraphAlgorithms(int a, T lol) {
        this.a = a;
        this.lol = lol;
    }
    int a;
    public T lol;

    int[] array = {1, 2, 3};
//    ArrayList<? extends Double> arr2;

//    public boolean bfs(Graph graph, int startNodeNumber, int goalNodeNumber) {
//        Deque<Integer> queue = new LinkedList<>();//(graph.getSize());
//        boolean[] visited = new boolean[graph.getSize()];
//        queue.push(startNodeNumber);
//        visited[startNodeNumber] = true;
//        while (!queue.isEmpty()) {
//            int curNodeNumber = queue.pop();
//            if (curNodeNumber == goalNodeNumber) return true;
//            for (int child : graph.getChildrenOf(curNodeNumber)) {
//                if (!visited[child]) {
//                    queue.push(child);
//                    visited[child] = true;
//                }
//            }
//        }
//        return true;
//    }
//
//    public boolean testFunc(ArrayList<ArrayList<Long>> array) {
//        for (int i = 0; i < array.size() - 1; i++) {
//            if (array.get(i).get(i) > array.get(i + 1).get(i + 1)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public boolean testFunc1(ArrayList<Long> array) {
//        for (int i = 0; i < array.size() - 1; i++) {
//            if (array.get(i) > array.get(i + 1)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public boolean testFunc2(long[] array) {
//        for (int i = 0; i < array.length - 1; i++) {
//            if (array[i] > array[i + 1]) {
//                return true;
//            }
//        }
//        return false;
//    }


//    public Object testFunc3(ArrayList<A<T>> a) {
//        if (a.get(0) == null) {
//            return null;
//        }
//        return a.get(a.size() - 1);
//    }

//    public static <T extends Number> Number testFunc3(Map<ArrayList<A<? extends Number>>, T[]> arr) {
//        return arr.entrySet().iterator().next().getKey().get(0).arr[0];
//    }

    public static int testFunc3(int[] matrix, String s) {
        if (matrix[0] == 0) {
            if (matrix[1] == 1) {
                if (matrix[2] == 2 && s.equals("lol")) {
                    return 123;
                }
            }
        }
        return matrix[0];
    }


    public static void test(String s) {
        if (s.charAt(0) == "b".charAt(0)) {
            if (s.charAt(1) == "a".charAt(0)) {
                if (s.charAt(2) == "d".charAt(0)) {
                    if (s.charAt(3) == "!".charAt(0)) {
                        throw new IllegalArgumentException();
                    }
                }
            }
        }
    }

    // should try to find the string with size 6 and with "!" in the end
    public static void testStrRem(String str) {
        if (!"world???".equals(str) && str.charAt(5) == '!' && str.length() == 6) {
            throw new RuntimeException();
        }
    }


    public static int floatToInt(float x) {
        if (x < 0) {
            if ((int) x < 0) {
                return 1;
            }
            return 2; // smth small to int zero
        }
        return 3;
    }

    // should find all branches that return -2, -1, 0, 1, 2.
    public static int numberOfRootsInSquareFunction(double a, double b, double c) {
        if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(c)) return -1;
        if (a == 0.0 || b == 0.0 || c == 0.0) return -2;
        double result = b * b - 4 * a * c;
        if (result > 0) {
            return 2;
        } else if (result == 0) {
            return 1;
        }
        return 0;
    }

    // will never be succeeded to find the value because of floating precision
    public static void floatEq(float v) {
        if (v == 28.7) {
            throw new IllegalArgumentException();
        }
    }

    // should generate double for constant float that equals to 28.700000762939453
    public static void floatEq(double v) {
        if (v == 28.7f) {
            throw new IllegalArgumentException();
        }
    }

    public static void diff(int a) {
        a = Math.abs(a);
        while (a > 0) {
            a = a % 2;
        }
        if (a < 0) {
            throw new IllegalArgumentException();
        }
        throw new RuntimeException();
    }

    // should cover 100% and better when values are close to constants,
    // also should generate "this" empty object
    public static String extent(int a) {
        if (a < -2.0) {
            return "-1";
        }
        if (a > 5) {
            return "-2";
        }
        if (a == 3) {
            return "-3";
        }
        if (4L < a) {
            return "-4";
        }
        return "0";
    }

    // should cover 100% with 3 tests
    public static boolean isGreater(long a, short b, int c) {
        if (b > a && a < c) {
            return true;
        }
        return false;
    }

    // should find a bad value with integer overflow
    public static boolean unreachable(int x) {
        int y = x * x - 2 * x + 1;
        if (y < 0) throw new IllegalArgumentException();
        return true;
    }

    public static boolean chars(char a) {
        if (a >= 'a' && a <= 'z') {
            return true;
        }
        return false;
    }



//    fun propagateHandshakes(friends: Map<String, Set<String>>): Map<String, Set<String>> {
//        val b = mutableMapOf<String, MutableSet<String>>()
//        val c = mutableSetOf<String>()
//        for (j in friends.values) c += j
//        for ((i, j) in friends)
//        for (k in j) {
//            b.getOrPut(i, ::mutableSetOf).add(k)
//            friends[k]?.let { b[i]?.plusAssign(it) }
//        }
//        for (i in c)
//            if (!b.keys.contains(i)) b.getOrPut(i) { mutableSetOf() }
//        for ((i, j) in b) {
//            val k = j.toMutableSet()
//            if (k.contains(i)) k.remove(i)
//            b[i] = k
//        }
//        return b
//    }

//    public static <T extends Number> int testFunc3(ArrayList<A<T>> arr) {
//        if (arr.get(0).a == 0) {
//            if (arr.get(1).a == 1) {
//                return 777;
//            }
//        }
//        return 0;
//    }

//    public boolean testFunc3(java.util.concurrent.BlockingQueue<java.lang.Runnable> a) {
//        for (int i = 0; i < array.size() - 1; i++) {
//            if (array.get(i).a > array.get(i + 1).a) {
//                return true;
//            }
//        }
//        return false;
//    }

//    public boolean testFunc3(A<Integer> a) {
////        for (int i = 0; i < array.size() - 1; i++) {
////            if (array.get(i).a > array.get(i + 1).a) {
////                return true;
////            }
////        }
//        return false;
//    }

}
