package org.utbot.engine.overrides;

/**
 * Auxiliary class with static methods without implementation.
 * These static methods are just markers for {@link org.utbot.engine.Traverser},
 * to do some corresponding behavior, that can be represented with smt expressions.
 * <p>
 * <code>UtLogicMock</code> is used to store bool smt bool expressions in
 * boolean variables instead of forking on conditions.
 */
@SuppressWarnings("unused")
public class UtLogicMock {
    public static boolean less(int a, int b) {
        return a < b;
    }

    public static boolean less(long a, long b) {
        return a < b;
    }

    public static <T> T ite(boolean condition, T thenValue, T elseValue) {
        return condition ? thenValue : elseValue;
    }

    public static int ite(boolean condition, int thenValue, int elseValue) {
        return condition ? thenValue : elseValue;
    }

    public static long ite(boolean condition, long thenValue, long elseValue) {
        return condition ? thenValue : elseValue;
    }
}
