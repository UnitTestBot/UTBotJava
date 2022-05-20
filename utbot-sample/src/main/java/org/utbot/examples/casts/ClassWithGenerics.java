package org.utbot.examples.casts;

public class ClassWithGenerics<T, R extends Comparable<R>> {
    private final T genericField;
    private final R comparableGenericField;

    public ClassWithGenerics(T genericField, R comparableGenericField) {
        this.genericField = genericField;
        this.comparableGenericField = comparableGenericField;
    }

    public T getGenericField() {
        return genericField;
    }

    public R getComparableGenericField() {
        return comparableGenericField;
    }

    class InnerClassWithGeneric {
        private final T[] genericArray;

        public InnerClassWithGeneric(T[] genericArray) {
            this.genericArray = genericArray;
        }

        public T[] getGenericArray() {
            return genericArray;
        }
    }
}
