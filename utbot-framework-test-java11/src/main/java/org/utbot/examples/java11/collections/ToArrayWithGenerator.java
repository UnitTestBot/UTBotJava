package org.utbot.examples.java11.collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ToArrayWithGenerator {
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


}

