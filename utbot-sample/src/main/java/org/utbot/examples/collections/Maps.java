package org.utbot.examples.collections;

import org.utbot.api.mock.UtMock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Maps {
    public String mapAsStaticField() {
        CollectionAsField.staticMap.put("key1", "value");
        return CollectionAsField.staticMap.get("key1");
    }

    public String mapAsParameter(Map<String, String> map) {
        map.put("key1", "value");
        return map.get("key1");
    }


    public String mapAsNonStaticField(CollectionAsField object) {
        object.nonStaticMap.put("key1", "value");
        return object.nonStaticMap.get("key1");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public String methodWithRawType(Map map) {
        nestedMethodWithGenericInfo(map);
        map.put("key1", "value");

        return (String) map.get("key1");
    }

    @SuppressWarnings("UnusedReturnValue")
    private Map<String, String> nestedMethodWithGenericInfo(Map<String, String> map) {
        return map;
    }

    Map<Integer, Integer> create(int[] keys, int[] values) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            result.put(keys[i], values[i]);
        }
        return result;
    }

    String mapToString(long startTime, int pageSize, int pageNum) {
        Map<String, Object> params = new HashMap<>();

        params.put("startTime", startTime);
        params.put("pageSize", pageSize);
        params.put("pageNum", pageNum);

        return params.toString();
    }

    @SuppressWarnings("OverwrittenKey")
    Integer mapPutAndGet() {
        Map<Long, Integer> values = new HashMap<>();

        values.put(1L, 2);
        values.put(1L, 3);

        return values.get(1L);
    }

    @SuppressWarnings("OverwrittenKey")
    Integer putInMapFromParameters(Map<Long, Integer> values) {
        values.put(1L, 2);
        values.put(1L, 3);

        return values.get(1L);
    }

    @SuppressWarnings("OverwrittenKey")
    Integer containsKeyAndPuts(Map<Long, Integer> values) {
        UtMock.assume(!values.containsKey(1L));

        values.put(1L, 2);
        values.put(1L, 3);

        UtMock.assume(values.get(1L).equals(3));

        return values.get(1L);
    }

    Map<Character, Integer> countChars(String s) {
        Map<Character, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < s.length(); i++) {
            map.compute(s.charAt(i), (character, value) -> value == null ? 1 : value + 1);
        }
        return map;
    }

    Map<Integer, Integer> createWithDifferentType(int seed) {
        if (seed % 2 == 0) {
            return new HashMap<>();
        } else {
            return new LinkedHashMap<>();
        }
    }

    Map<Integer, Integer> putElements(Map<Integer, Integer> map, int[] array) {
        if (map.size() > 0 && array.length > 0) {
            for (int i = 0; i < array.length; i++) {
                map.put(array[i], array[i]);
            }
        }

        return map;
    }

    Map<Integer, Integer> computeValue(Map<Integer, Integer> map, int key) {
        map.compute(key, (oldKey, value) -> {
            if (value == null) {
                return key + 1;
            } else {
                return value + 1;
            }
        });
        return map;
    }

    Map<Integer, Integer> computeValueIfAbsent(Map<Integer, Integer> map, int key) {
        map.computeIfAbsent(key, (oldKey) -> key + 1);
        return map;
    }

    Map<Integer, Integer> computeValueIfPresent(Map<Integer, Integer> map, int key) {
        if (map.computeIfPresent(key, (oldKey, value) -> value + 1) == null) {
            return map;
        } else {
            return map;
        }
    }

    int clearEntries(Map<Integer, Integer> map) {
        if (map.isEmpty()) {
            return 0;
        }
        map.clear();
        if (map.isEmpty()) {
            return 1;
        }
        return 2;
    }

    int containsKey(Map<Integer, Integer> map, int key) {
        if (map.containsKey(key)) {
            return 1;
        } else {
            return 0;
        }
    }

    int containsValue(Map<Integer, Integer> map, int value) {
        if (map.containsValue(value)) {
            return 1;
        } else {
            return 0;
        }
    }

    int getOrDefaultElement(Map<Integer, Integer> map, int key) {
        if (map.getOrDefault(key, null) == null) {
            if (map.containsKey(key)) {
                return 0;
            } else {
                return 1;
            }
        } else {
            return map.get(key);
        }
    }

    int removeKeyWithValue(Map<Integer, Integer> map, int key, int value) {
        boolean containsKey = map.containsKey(key);
        boolean containsValue = map.containsValue(value);
        if (!containsKey && !containsValue) {
            return 0;
        } else if (containsKey && !containsValue) {
            if (map.remove(key, value)) {
                return 1;
            } else {
                return -1;
            }
        } else if (!containsKey) {
            if (map.remove(key, value)) {
                return 2;
            } else {
                return -2;
            }
        } else {
            if (map.remove(key, value)) {
                return 3;
            } else {
                return -3;
            }
        }
    }

    Map<Integer, Integer> putElementIfAbsent(Map<Integer, Integer> map, int key, int value) {
        Integer result = map.get(key);
        Integer newResult = map.putIfAbsent(key, value);
        if (result == newResult) {
            return map;
        } else {
            // unreachable branch
            return map;
        }

    }

    Map<Integer, Integer> replaceEntry(Map<Integer, Integer> map, int key, int value) {
        boolean containsKey = map.containsKey(key);
        Integer prev = map.replace(key, value);
        if (containsKey && prev == null) {
            return map;
        } else if (!containsKey && prev == null) {
            return map;
        } else {
            return map;
        }
    }

    int replaceEntryWithValue(Map<Integer, Integer> map, int key, int oldValue) {
        boolean containsKey = map.containsKey(key);
        boolean containsValue = map.containsValue(oldValue);
        if (!containsKey && !containsValue) {
            return 0;
        } else if (containsKey && !containsValue) {
            if (map.replace(key, oldValue, -1)) {
                return 1;
            } else {
                return -1;
            }
        } else if (!containsKey) {
            if (map.replace(key, oldValue, -1)) {
                return 2;
            } else {
                return -2;
            }
        } else {
            if (map.replace(key, oldValue, -1)) {
                return 3;
            } else {
                return -3;
            }
        }
    }

    Map<Integer, Integer> merge(Map<Integer, Integer> map, int key, Integer value) {
        if (map.merge(key, value, Integer::sum) == null) {
            return map;
        } else {
            return map;
        }
    }

    int putAllEntries(Map<Integer, Integer> map, Map<Integer, Integer> other) {
        int current = map.size();
        map.putAll(other);
        if (map.size() == current) {
            return 0;
        } else if (map.size() == current + other.size()) {
            return 1;
        } else {
            return 2;
        }
    }

    Map<Integer, Integer> replaceAllEntries(Map<Integer, Integer> map) {
        if (map.isEmpty()) {
            return null;
        }
        map.replaceAll((key, value) -> key > value ? value + 1 : value - 1);
        return map;
    }

    int removeElements(Map<Integer, Integer> map, int i, int j) {
        Integer a = map.remove(i);
        Integer b = map.remove(j);
        if (a != null && b != null) {
            if (i == j) {
                // impossible branch
                return 0;
            } else if (i < j) {
                return 1;
            } else {
                return 2;
            }
        } else if (a != null) {
            return 3;
        } else if (b != null) {
            return 4;
        } else {
            return -1;
        }
    }

    CustomClass removeCustomObject(Map<CustomClass, CustomClass> map, int i) {
        CustomClass removed = map.remove(new CustomClass(i));
        if (removed == null) {
            return null;
        } else {
            return removed;
        }
    }

    public List<String> mapOperator(Map<String, String> map) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue().equals("key")) {
                result.add(entry.getKey());
            }
        }
        if (result.size() > 1) {
            return result;
        } else {
            return new ArrayList<>(map.values());
        }
    }

    public Map<String, Integer> createMapWithString() {
        Map<String, Integer> map = new HashMap<>();
        map.put("tuesday", 354);
        map.remove("tuesday");

        return map;
    }

    public Map<WorkDays, Integer> createMapWithEnum() {
        Map<WorkDays, Integer> map = new HashMap<>();
        map.put(WorkDays.Monday, 112);
        map.put(WorkDays.Tuesday, 354);
        map.put(WorkDays.Friday, 567);
        map.remove(WorkDays.Tuesday);

        return map;
    }

    public enum WorkDays {
        Monday,
        Tuesday,
        Wednesday,
        Thursday,
        Friday
    }
}
