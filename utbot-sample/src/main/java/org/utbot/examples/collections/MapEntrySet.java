package org.utbot.examples.collections;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MapEntrySet {
    int addToEntrySet(Map<Integer, Integer> m) {
        Set<Map.Entry<Integer, Integer>> set = m.entrySet();
        set.add(new Map.Entry<Integer, Integer>() {
            public Integer getKey() { return 0; }
            public Integer getValue() { return 1; }
            public Integer setValue(Integer value) { return null; }
        });
        // must throw exception
        return -1;
    }

    Map<Integer, Integer> removeFromEntrySet(Map<Integer, Integer> m, int i, int j) {
        Set<Map.Entry<Integer, Integer>> set = m.entrySet();
        if (set.remove(new Map.Entry<Integer, Integer>() {
            public Integer getKey() {
                return i;
            }
            public Integer getValue() {
                return j;
            }
            public Integer setValue(Integer value) {
                return null;
            }
        })) {
            return m;
        } else {
            return m;
        }
    }

    int getFromEntrySet(Map<Integer, Integer> m, int i, int j) {
        Set<Map.Entry<Integer, Integer>> set = m.entrySet();
        boolean a = set.contains(new Map.Entry<Integer, Integer>() {
            public Integer getKey() { return i; }
            public Integer getValue() { return j; }
            public Integer setValue(Integer value) { return null; }
        });
        if (a == (m.containsKey(i) && Objects.equals(m.get(i), j))) {
            return 1;
        } else {
            // unreachable branch
            return -1;
        }
    }

    int iteratorHasNext(Map<Integer, Integer> m) {
        Iterator<Map.Entry<Integer, Integer>> iterator = m.entrySet().iterator();
        if (!iterator.hasNext()) {
            return m.size();
        } else {
            return m.size();
        }
    }

    int[] iteratorNext(Map<Integer, Integer> m) {
        Iterator<Map.Entry<Integer, Integer>> iterator = m.entrySet().iterator();
        Map.Entry<Integer, Integer> entry = iterator.next();
        return new int[]{entry.getKey(), entry.getValue()};
    }

    Map<Integer, Integer> iteratorRemove(Map<Integer, Integer> m) {
        Iterator<Map.Entry<Integer, Integer>> iterator = m.entrySet().iterator();
        iterator.next();
        iterator.remove();
        return m;
    }

    Map<Integer, Integer> iteratorRemoveOnIndex(Map<Integer, Integer> m, int i) {
        if (i == 0) {
            return null;
        }
        Iterator<Map.Entry<Integer, Integer>> iterator = m.entrySet().iterator();
        for (int j = 0; j < i; j++) {
            iterator.next();
        }
        iterator.remove();
        return m;
    }

    int[] iterateForEach(Map<Integer, Integer> m) {
        int keySum = 0;
        int valueSum = 0;
        for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
            keySum += entry.getKey();
            valueSum += entry.getValue();
        }
        return new int[]{keySum, valueSum};
    }

    int[] iterateWithIterator(Map<Integer, Integer> m) {
        int valueSum = 0;
        int keySum = 0;
        Iterator<Map.Entry<Integer, Integer>> iterator = m.entrySet().iterator();
        while (iterator.hasNext()) {
            keySum += iterator.next().getKey();
            valueSum += iterator.next().getValue();
        }
        return new int[]{keySum, valueSum};
    }
}
