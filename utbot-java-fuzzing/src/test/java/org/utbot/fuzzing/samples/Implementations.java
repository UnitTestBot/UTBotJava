package org.utbot.fuzzing.samples;

public final class Implementations {
    public interface A<T> {
        T getValue();
    }

    public static class AString implements A<String> {

        private final String value;

        public AString(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    public static class AInteger implements A<Integer> {

        private final Integer value;

        public AInteger(Integer value) {
            this.value = value;
        }

        @Override
        public Integer getValue() {
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static int test(A<Integer> value) {
        if (value.getValue() < 0) {
            return 0;
        }
        return value.getValue();
    }
}
