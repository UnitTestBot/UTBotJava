package org.utbot.fuzzing.samples;

import java.util.List;

@SuppressWarnings("unused")
public class Stubs {

    public static void name(String value) {}

    public static <T extends Iterable<T>> int resolve(T recursive) {
        int result = 0;
        for (T t : recursive) {
            result++;
        }
        return result;
    }

    public static <T extends List<Number>> int types(T[] t1, T[] t2, T[] t3) {
        return t1.length + t2.length + t3.length;
    }

    public static <T extends Number> int arrayLength(java.util.List<T>[][] test) {
        int length = 0;
        for (int i = 0; i < test.length; i++) {
            for (int j = 0; j < test[i].length; j++) {
                length += test[i][j].size();
            }
        }
        return length;
    }

    public static <A extends Iterable<B>, B extends List<A>, C extends List<? extends Iterable<A>>> A example(A c1, B c2, C c) {
        return c2.iterator().next();
    }
}
