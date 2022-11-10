

package org.utbot.quickcheck.internal;

public final class Weighted<T> {
    public final T item;
    public final int weight;

    public Weighted(T item, int weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException(
                "non-positive weight: " + weight);
        }

        this.item = item;
        this.weight = weight;
    }
}
