package org.utbot.fuzzing.samples;

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
}
