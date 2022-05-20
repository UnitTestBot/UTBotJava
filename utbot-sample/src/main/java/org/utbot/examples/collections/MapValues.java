package org.utbot.examples.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapValues {
    int addToValues(Map<Integer, Integer> m) {
        Collection<Integer> values = m.values();
        values.add(0);
        // must throw exception
        return -1;
    }

    Map<Integer, Integer> removeFromValues(Map<Integer, Integer> m, int i) {
        Collection<Integer> values = m.values();
        // must throw exception
        values.remove(i);
        return m;
    }

    int getFromValues(Map<Integer, Integer> m, int i) {
        Collection<Integer> values = m.values();
        boolean a = values.contains(i);
        boolean b = m.containsValue(i);
        if (a == b) {
            return 1;
        } else {
            // unreachable branch
            return -1;
        }
    }

    int iteratorHasNext(Map<Integer, Integer> m) {
        Iterator<Integer> iterator = m.values().iterator();
        if (!iterator.hasNext()) {
            return m.size();
        } else {
            return m.size();
        }
    }

    int iteratorNext(Map<Integer, Integer> m) {
        Iterator<Integer> iterator = m.values().iterator();
        return iterator.next();
    }

    Map<Integer, Integer> iteratorRemove(Map<Integer, Integer> m) {
        Iterator<Integer> iterator = m.values().iterator();
        iterator.next();
        iterator.remove();
        return m;
    }

    Map<Integer, Integer> iteratorRemoveOnIndex(Map<Integer, Integer> m, int i) {
        if (i == 0) {
            return null;
        }
        Iterator<Integer> iterator = m.values().iterator();
        for (int j = 0; j < i; j++){
            iterator.next();
        }
        iterator.remove();
        return m;
    }

    int iterateForEach(Map<Integer, Integer> m) {
        int sum = 0;
        for (int i: m.values()) {
            sum += i;
        }
        return sum;
    }

    int iterateWithIterator(Map<Integer, Integer> m) {
        int sum = 0;
        Iterator<Integer> iterator = m.values().iterator();
        while (iterator.hasNext()){
            sum += iterator.next();
        }
        return sum;
    }
}
