package org.utbot.examples.collections;

import org.utbot.api.mock.UtMock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Lists {
    int bigListFromParameters(List<Integer> list) {
        UtMock.assume(list != null && list.size() == 11);

        return list.size();
    }

    Collection<Integer> getNonEmptyCollection(Collection<Integer> collection) {
        if (collection.size() == 0) {
            return null;
        }

        return collection;
    }

    List<Integer> create(int[] array) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            result.add(array[i]);
        }
        return result;
    }

    @SuppressWarnings({"WhileLoopReplaceableByForEach", "DuplicatedCode"})
    boolean iterableContains(Iterable<?> iterable) {
        Objects.requireNonNull(iterable);

        Iterator<?> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            Object value = iterator.next();
            if (value.equals(1)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings({"WhileLoopReplaceableByForEach", "DuplicatedCode"})
    boolean collectionContains(Collection<?> collection) {
        Objects.requireNonNull(collection);

        Iterator<?> iterator = collection.iterator();
        while (iterator.hasNext()) {
            Object value = iterator.next();
            if (value.equals(1)) {
                return true;
            }
        }

        return false;
    }

    Integer[] getFromAnotherListToArray(List<Integer> other) {
        if (other.get(0) == null) {
            return null;
        }
        Integer[] result = new Integer[1];
        result[0] = other.get(0);
        return result;
    }

    List<Integer> createWithDifferentType(int seed) {
        List<Integer> result;
        if (seed % 2 == 0) {
            result = new ArrayList<>();
        } else {
            result = new LinkedList<>();
        }
        for (int i = 0; i < 4; i++) {
            result.add(i);
        }
        return result;
    }

    List<Integer> addElements(List<Integer> list, int[] array) {
        if (list.size() >= 2 && array.length >= 2) {
            for (int i = 0; i < array.length; i++) {
                list.add(i, array[i]);
            }
        }
        return list;
    }

    int[] getElements(List<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            a[i] = list.get(i);
        }
        return a;
    }

    List<Integer> setElements(int[] arr) {
        List<Integer> list = new ArrayList<>(arr.length);
        for (int i = 0; i < arr.length; i++) {
            list.add(null);
            list.set(i, arr[i]);
        }
        return list;
    }

    int removeElements(List<Integer> list, int i, int j) {
        int a = list.remove(i);
        int b = list.remove(j);
        if (a < b) {
            return a;
        } else {
            return b;
        }
    }

    List<Integer> clear(List<Integer> list) {
        if (list.size() < 2) {
            list.add(1);
            list.add(2);
        }
        list.clear();
        return list;
    }

    List<Integer> removeFromList(List<Integer> list, int i) {
        if (list instanceof ArrayList) {
            list.set(i, list.get(list.size() - 1));
            list.remove(list.size() - 1);
            return list;
        } else if (list instanceof LinkedList) {
            list.remove(i);
            return list;
        }
        return list;
    }

    List<Integer> addAll(List<Integer> list, int i) {
       List<Integer> newList = new ArrayList<>();
       newList.add(i);
       if (list.size() > 0) {
           newList.addAll(list);
       }
       return newList;
    }

    List<Integer> addAllByIndex(List<Integer> list, int i) {
        List<Integer> newList = new ArrayList<>();
        newList.add(0);
        newList.add(1);
        if (i >= 0 && i < list.size()) {
            list.addAll(i, newList);
        }
        return list;
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    List<String> asListExample(String[] values) {
        UtMock.assume(values != null);

        if (values.length == 0) {
            return Arrays.asList(values);
        } else {
            return Arrays.asList(values);
        }
    }
}
