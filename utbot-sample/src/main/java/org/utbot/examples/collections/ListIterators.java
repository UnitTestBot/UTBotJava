package org.utbot.examples.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ListIterators {

    List<Integer> iterate(List<Integer> list) {
        Iterator<Integer> iterator = list.iterator();
        List<Integer> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    List<Integer> iterateReversed(List<Integer> list) {
        ListIterator<Integer> iterator = list.listIterator(list.size());
        List<Integer> result = new ArrayList<>();
        while (iterator.hasPrevious()) {
            result.add(iterator.previous());
        }
        return result;
    }

    int iterateForEach(List<Integer> list) {
        int sum = 0;
        for (int i : list) {
            sum += i;
        }
        return sum;
    }

    List<Integer> addElements(List<Integer> list, int[] array) {
        ListIterator<Integer> iterator = list.listIterator();
        int index = 0;
        while (iterator.hasNext()) {
            iterator.add(array[index++]);
            iterator.next();
        }
        return list;
    }

    List<Integer> setElements(List<Integer> list, int[] arr) {
        ListIterator<Integer> iterator = list.listIterator();
        int index = 0;
        while (iterator.hasNext()) {
            iterator.next();
            iterator.set(arr[index]);
        }
        return list;
    }

    List<Integer> removeElements(List<Integer> list, int i) {
        ListIterator<Integer> iterator = list.listIterator();
        int a = 0;
        for (int k = 0; k < i; k++) {
            a = iterator.next();
        }
        iterator.remove();
        return list;
    }
}
