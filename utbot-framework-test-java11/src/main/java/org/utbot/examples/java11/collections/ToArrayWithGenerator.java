package org.utbot.examples.java11.collections;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("Since15")
public class ToArrayWithGenerator<T> {
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    public boolean checkSetSize(int size) {
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < size; i++) {
            set.add(i);
        }

        Integer[] array = set.toArray(Integer[]::new);
        return array.length == size;
    }

    @SuppressWarnings({"SuspiciousToArrayCall", "MismatchedReadAndWriteOfArray"})
    public boolean checkSetSizeArrayStoreException(int size) {
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < size; i++) {
            set.add(i);
        }

        String[] array = set.toArray(String[]::new);
        return array.length == size;
    }

    public boolean checkListSize(int size) {
        List<Integer> integers = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            integers.add(i);
        }

        return integers.toArray(Integer[]::new).length == size;
    }

    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    public boolean checkMapKeysSize(int size) {
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(i, "hello");
        }

        Integer[] keyArray = map.keySet().toArray(Integer[]::new);
        return keyArray.length == size;
    }

    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    public boolean checkMapValuesSize(int size) {
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(i, "hello");
        }

        String[] valuesArray = map.values().toArray(String[]::new);
        return valuesArray.length == size;
    }

    @SuppressWarnings({"SuspiciousToArrayCall", "UnnecessaryLocalVariable"})
    public String[] getMapEntrySetArrayStoreException() {
        Map<String, String> namesMap = new HashMap<>();
        namesMap.put("Joe", "Jane");
        namesMap.put("Bill", "Anna");
        String[] names = namesMap.entrySet().toArray(String[]::new);
        return names;
    }

    public int getMapEntrySetSize() {
        Map<String, String> namesMap = new HashMap<>();
        namesMap.put("Joe", "Jane");
        namesMap.put("Bill", "Anna");
        Set<Map.Entry<String, String>> entries = namesMap.entrySet();
        return entries.toArray(Map.Entry[]::new).length;
    }

    public int getCollectionArgumentSize(@NotNull Collection<Integer> arg) {
        return arg.toArray(Integer[]::new).length;
    }

    public int getSetArgumentSize(@NotNull Set<Integer> arg) {
        return arg.toArray(Integer[]::new).length;
    }

    public int getListArgumentSize(@NotNull List<Integer> arg) {
        return arg.toArray(Integer[]::new).length;
    }

    public int getAbstractCollectionArgumentSize(@NotNull AbstractCollection<Integer> arg) {
        return arg.toArray(Integer[]::new).length;
    }

    public int getGenericCollectionArgumentSize(@NotNull Collection<T> arg) {
        return arg.toArray(Object[]::new).length;
    }

    public int countMatchingElements(@NotNull ArrayList<Integer> arrayList) {
        int size = arrayList.size();
        Integer[] array = arrayList.toArray(Integer[]::new);
        if (array.length != size) {
            return -1;
        }

        int count = 0;
        for (int i = 0; i < size; i++) {
            Integer arrayElement = array[i];
            Integer listElement = arrayList.get(i);
            if (arrayElement == null) {
                if (listElement == null) {
                    count++;
                } else {
                    return -2;
                }
            } else {
                if (arrayElement.equals(listElement)) {
                    count++;
                }
            }
        }

        return count;
    }

}
