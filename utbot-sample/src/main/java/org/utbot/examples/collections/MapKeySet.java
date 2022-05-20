package org.utbot.examples.collections;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MapKeySet {
    int addToKeySet(Map<Integer, Integer> m) {
        Set<Integer> set = m.keySet();
        set.add(0);
        // must throw exception
        return -1;
    }

    Map<Integer, Integer> removeFromKeySet(Map<Integer, Integer> m, int i) {
        Set<Integer> set = m.keySet();
        set.remove(i);
        return m;
    }

    int getFromKeySet(Map<Integer, Integer> m, int i) {
        Set<Integer> set = m.keySet();
        boolean a = set.contains(i);
        boolean b = m.containsKey(i);
        if (a == b) {
            return 1;
        } else {
            // unreachable branch
            return -1;
        }
    }

    int iteratorHasNext(Map<Integer, Integer> m) {
        Iterator<Integer> iterator = m.keySet().iterator();
        if (!iterator.hasNext()) {
            return m.size();
        } else {
            return m.size();
        }
    }

    int iteratorNext(Map<Integer, Integer> m) {
        Iterator<Integer> iterator = m.keySet().iterator();
        return iterator.next();
    }

    Map<Integer, Integer> iteratorRemove(Map<Integer, Integer> m) {
        Iterator<Integer> iterator = m.keySet().iterator();
        iterator.next();
        iterator.remove();
        return m;
    }

    Map<Integer, Integer> iteratorRemoveOnIndex(Map<Integer, Integer> m, int i) {
        if (i == 0) {
            return null;
        }
        Iterator<Integer> iterator = m.keySet().iterator();
        for (int j = 0; j < i; j++){
            iterator.next();
        }
        iterator.remove();
        return m;
    }

    int iterateForEach(Map<Integer, Integer> m) {
        int sum = 0;
        for (int i: m.keySet()) {
            sum += i;
        }
        return sum;
    }

    int iterateWithIterator(Map<Integer, Integer> m) {
        int sum = 0;
        Iterator<Integer> iterator = m.keySet().iterator();
        while (iterator.hasNext()){
            sum += iterator.next();
        }
        return sum;
    }

    Integer nullKey(Map<Integer, Integer> m) {
        if (m.containsKey(null)) {
            return m.get(null);
        } else {
            return 0;
        }
    }
}
