package org.utbot.examples.collections;

import org.utbot.api.mock.UtMock;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Sets {
    Set<Integer> create(int[] array) {
        Set<Integer> result = new HashSet<>();
        for (int i = 0; i < array.length; i++) {
            result.add(array[i]);
        }
        return result;
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    public Set<Integer> setContainsInteger(Set<Integer> set, Integer a, Integer b) {
        if (set.contains(1 + a)) {
            set.remove(1 + a);
            return set;
        } else {
            set.add(4 + a + b);
            set.remove(4 + a + b);
            if (set.isEmpty()) {
                return null;
            }
            return set;
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean setContains(Set<String> set, String str, String str2) {
        if (set.contains("aaa" + str)) {
            set.remove("aaa" + str);
            return true;
        } else {
            set.add(str + "aaa" + "bbb");
            set.remove(str2);
            if (set.isEmpty()) {
                return true;
            }
            return false;
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean simpleContains(Set<String> set) {
        if (set.contains("aaa")) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean moreComplicatedContains(Set<String> set, String str) {
        if (set.contains("aaa" + str)) {
            return true;
        }
        return false;
    }

    Set<Character> findAllChars(String s) {
        Set<Character> set = new HashSet<>();
        for (int i = 0; i < s.length(); i++) {
            set.add(s.charAt(i));
        }
        return set;
    }

    int removeSpace(Set<Character> s) {
        int counter = 0;
        if (s.remove(' ')) {
            counter++;
        }
        if (s.remove('\t')) {
            counter++;
        }
        if (s.remove('\r')) {
            counter++;
        }
        if (s.remove('\n')) {
            counter++;
        }
        return counter;
    }

    Set<Integer> createWithDifferentType(int seed) {
        if (seed % 2 == 0) {
            return new HashSet<>();
        } else {
            return new LinkedHashSet<>();
        }
    }

    Set<Integer> addElements(Set<Integer> set, int[] array) {
        if (set.size() > 0 && array.length > 0) {
            for (int i = 0; i < array.length; i++) {
                set.add(array[i]);
            }
        }
        return set;
    }

    int removeElements(Set<Integer> set, int i, int j) {
        boolean a = set.remove(i);
        boolean b = set.remove(j);
        if (a && b) {
            if (i == j) {
                // impossible branch
                return 0;
            } else if (i < j) {
                return 1;
            } else {
                return 2;
            }
        } else if (a) {
            return 3;
        } else if (b) {
            return 4;
        } else {
            return -1;
        }
    }

    int containsElement(Set<Integer> set, int i) {
        if (set.contains(i)) {
            return 1;
        } else {
            return 0;
        }
    }

    int addAllElements(Set<Integer> set, Set<Integer> other) {
        int current = set.size();
        set.addAll(other);
        if (set.size() == current) {
            return 0;
        }
        if (set.size() == current + other.size()) {
            return 1;
        }
        return 2;
    }

    int removeAllElements(Set<Integer> set, Set<Integer> other) {
        int current = set.size();
        final int otherSize = other.size(); // to consider case set == other
        set.removeAll(other);
        if (set.size() == current) {
            return 0;
        }
        if (set.size() == current - otherSize) {
            return 1;
        }
        return 2;
    }

    int retainAllElements(Set<Integer> set, Set<Integer> other) {
        int current = set.size();
        set.retainAll(other);
        if (set.size() == current) {
            return 1;
        } else {
            return 0;
        }
    }

    int containsAllElements(Set<Integer> set, Set<Integer> other) {
        if (set.isEmpty() || other.isEmpty()) {
            return -1;
        }
        if (set.containsAll(other)) {
            return 1;
        }
        return 0;
    }

    int clearElements(Set<Integer> set) {
        if (set.isEmpty()) {
            return 0;
        }
        set.clear();
        if (set.isEmpty()) {
            return 1;
        }
        return -1;
    }

    int removeCustomObject(Set<CustomClass> set, int i) {
        if (set.remove(new CustomClass(i))) {
            return 1;
        } else {
            return 0;
        }
    }
}

class CustomClass {
    int value;

    CustomClass(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CustomClass) {
            CustomClass that = (CustomClass) o;
            return value == that.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
