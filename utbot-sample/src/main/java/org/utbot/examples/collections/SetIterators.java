package org.utbot.examples.collections;

import org.utbot.api.mock.UtMock;

import java.util.Iterator;
import java.util.Set;

public class SetIterators {
    @SuppressWarnings({"IfStatementWithIdenticalBranches", "RedundantOperationOnEmptyContainer"})
    Iterator<Integer> returnIterator(Set<Integer> set) {
        UtMock.assume(set != null);

        if (set.isEmpty()) {
            return set.iterator();
        } else {
            return set.iterator();
        }
    }

    int iteratorHasNext(Set<Integer> s) {
        Iterator<Integer> iterator = s.iterator();
        if (!iterator.hasNext()) {
            return s.size();
        } else {
            return s.size();
        }
    }

    int iteratorNext(Set<Integer> s) {
        Iterator<Integer> iterator = s.iterator();
        return iterator.next();
    }

    Set<Integer> iteratorRemove(Set<Integer> s) {
        Iterator<Integer> iterator = s.iterator();
        iterator.next();
        iterator.remove();
        return s;
    }

    Set<Integer> iteratorRemoveOnIndex(Set<Integer> s, int i) {
        if (i == 0) {
            return null;
        }
        Iterator<Integer> iterator = s.iterator();
        for (int j = 0; j < i; j++){
            iterator.next();
        }
        iterator.remove();
        return s;
    }

    int iterateForEach(Set<Integer> s) {
        int sum = 0;
        for (int i: s) { // possible NPE
            sum += i;
        }
        return sum;
    }

    int iterateWithIterator(Set<Integer> s) {
        int sum = 0;
        Iterator<Integer> iterator = s.iterator();
        while (iterator.hasNext()){
            sum += iterator.next(); // possible NPE
        }
        return sum;
    }
}
