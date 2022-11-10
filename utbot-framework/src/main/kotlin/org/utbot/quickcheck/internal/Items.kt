

package org.utbot.quickcheck.internal;

import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

public final class Items {
    private Items() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public static <T> T choose(Collection<T> items, SourceOfRandomness random) {
        int size = items.size();
        if (size == 0) {
            throw new IllegalArgumentException(
                "Collection is empty, can't pick an element from it");
        }

        if (items instanceof RandomAccess && items instanceof List<?>) {
            List<T> list = (List<T>) items;
            return size == 1
                ? list.get(0)
                : list.get(random.nextInt(size));
        }

        if (size == 1) {
            return items.iterator().next();
        }

        Object[] array = items.toArray(new Object[0]);
        return (T) array[random.nextInt(array.length)];
    }

    public static <T> T chooseWeighted(
        Collection<org.utbot.quickcheck.internal.Weighted<T>> items,
        SourceOfRandomness random) {

        if (items.size() == 1)
            return items.iterator().next().item;

        int range = items.stream().mapToInt(i -> i.weight).sum();
        int sample = random.nextInt(range);

        int threshold = 0;
        for (Weighted<T> each : items) {
            threshold += each.weight;
            if (sample < threshold)
                return each.item;
        }

        throw new AssertionError(
            String.format("sample = %d, range = %d", sample, range));
    }
}
