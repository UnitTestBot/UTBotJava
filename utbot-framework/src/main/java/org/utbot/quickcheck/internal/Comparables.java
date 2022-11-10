

package org.utbot.quickcheck.internal;

import java.util.function.Predicate;

public final class Comparables {
    private Comparables() {
        throw new UnsupportedOperationException();
    }

    public static <T extends Comparable<? super T>>
    Predicate<T> inRange(T min, T max) {
        return c -> {
            if (min == null && max == null)
                return true;
            if (min == null)
                return c.compareTo(max) <= 0;
            if (max == null)
                return c.compareTo(min) >= 0;
            return c.compareTo(min) >= 0 && c.compareTo(max) <= 0;
        };
    }

}
