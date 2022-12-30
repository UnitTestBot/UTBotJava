package org.utbot.fuzzing.samples;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@SuppressWarnings({"unused", "RedundantIfStatement"})
public class Collections {
    /**
     * Should create unsorted list that will be sorted as a result.
     */
    public static <T extends Number> Collection<T> sorted(Collection<T> source) {
        if (source.size() < 2) throw new IllegalArgumentException();
        return source.stream().sorted().collect(Collectors.toList());
    }

    /**
     * Should create at least both answers: one that finds the key, and another returns null.
     */
    public static String getKeyForValue(Map<String, Number> map, Number value) {
        for (Map.Entry<String, Number> entry : map.entrySet()) {
            if (java.util.Objects.equals(entry.getValue(), value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Should find a branch that returns true and size of array is greater than 1 (non-trivial).
     */
    public static boolean isDiagonal(Collection<Collection<Double>> matrix) {
        int cols = matrix.size();
        if (cols <= 1) {
            return false;
        }
        int i = 0;
        for (Collection<Double> col : matrix) {
            if (col.size() != cols) {
                return false;
            }
            int j = 0;
            for (Double value : col) {
                if (i == j && value == 0.0) return false;
                if (i != j && value != 0.0) return false;
                j++;
            }
            i++;
        }
        return true;
    }

    /**
     * Checks that different collections can be created. Part 1
     */
    public boolean allCollectionAreSameSize1(
            Collection<Integer> c,
            List<Integer> l,
            Set<Integer> s,
            SortedSet<Integer> ss,
            Deque<Integer> d,
            Iterable<Integer> i
    ) {
        if (c.size() != l.size()) {
            return false;
        }
        if (l.size() != s.size()) {
            return false;
        }
        if (s.size() != ss.size()) {
            return false;
        }
        if (ss.size() != d.size()) {
            return false;
        }
        if (d.size() != StreamSupport.stream(i.spliterator(), false).count()) {
            return false;
        }
        return true;
    }

    /**
     * Checks that different collections can be created. Part 2
     */
    public boolean allCollectionAreSameSize2(
            Iterable<Integer> i,
            Stack<Integer> st,
            NavigableSet<Integer> ns,
            Map<Integer, Integer> m,
            SortedMap<Integer, Integer> sm,
            NavigableMap<Integer, Integer> nm
    ) {
        if (StreamSupport.stream(i.spliterator(), false).count() != st.size()) {
            return false;
        }
        if (st.size() != ns.size()) {
            return false;
        }
        if (ns.size() != m.size()) {
            return false;
        }
        if (m.size() != sm.size()) {
            return false;
        }
        if (sm.size() != nm.size()) {
            return false;
        }
        return true;
    }

    /**
     * Should create TreeSet without any modifications as T extends Number is not Comparable
     */
    public <T extends Number> boolean testTreeSetWithoutComparable(NavigableSet<T> set) {
        if (set.size() > 3) {
            return true;
        }
        return false;
    }

    /**
     * Should create TreeSet with modifications as Integer is Comparable
     */
    public boolean testTreeSetWithComparable(NavigableSet<Integer> set) {
        if (set.size() > 3) {
            return true;
        }
        return false;
    }

    public static class ConcreteList<T extends Number> extends LinkedList<T> {
        public boolean equals(Collection<T> collection) {
            if (collection.size() != size()) {
                return false;
            }
            int i = 0;
            for (T t : collection) {
                if (!java.util.Objects.equals(get(i), t)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Should create concrete class
     */
    public boolean testConcreteCollectionIsCreated(ConcreteList<?> list) {
        if (list.size() > 3) {
            return true;
        }
        return false;
    }

    public static class ConcreteMap<K, V> extends HashMap<K, V> { }

    /**
     * Should create concrete class
     */
    public boolean testConcreteMapIsCreated(ConcreteMap<?, ?> map) {
        if (map.size() > 3) {
            return true;
        }
        return false;
    }

    /**
     * Should create test with no error
     */
    public boolean testNoErrorWithHashMap(HashMap<?, ?> map) {
        if (map.size() > 5) {
            return true;
        }
        return false;
    }

    /**
     * Should generate iterators with recursions
     */
    public static  <T extends Iterator<T>> int size(Iterator<T> some) {
        int r = 0;
        while (some.hasNext()) {
            some.next();
            r++;
        }
        return r;
    }
}
