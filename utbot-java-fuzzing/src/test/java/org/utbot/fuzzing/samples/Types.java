package org.utbot.fuzzing.samples;

@SuppressWarnings("ALL")
public class Types {

    public static class MyBean<T extends MyBean<T>> {
        public void foo(int value) {

        }
    }

    public static <T extends MyBean<T>> void bar(T bean) {

    }

}
