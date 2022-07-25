package org.utbot.examples.enums;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ComplexEnumExamples {

    public enum Color {
        RED,
        GREEN,
        BLUE
    }

    private static final Color[] COLOR_VALUES = Color.values();

    public int countEqualColors(@NotNull Color a, @NotNull Color b, @NotNull Color c) {
        int equalToA = 1;
        if (b == a) equalToA++;
        if (c == a) equalToA++;

        int equalToB = 1;
        if (a == b) equalToB++;
        if (c == b) equalToB++;

        if (equalToA > equalToB)
            return equalToA;
        else
            return equalToB;
    }

    public List<Color> returnColors(@NotNull List<Color> source) {
        if (source.isEmpty())
            return new LinkedList<>();
        for (Color color : source) {
            assert color != null;
        }
       return source;
    }

    public List<Color> copyColors(@NotNull List<Color> source) {
        LinkedList<Color> result = new LinkedList<>();
        for (Color color : source) {
            assert color != null;
            result.add(color);
        }
        return result;
    }

    public int enumToEnumMapCountValues(@NotNull Map<Color, Color> map) {
        int count = 0;
        for (Color color: map.values()) {
            if (color == Color.RED) {
                count++;
            }
        }
        return count;
    }

    public int enumToEnumMapCountKeys(@NotNull Map<Color, Color> map) {
        int count = 0;
        for (Color key: map.keySet()) {
            if (key == Color.GREEN || Color.BLUE.equals(key)) {
                count++;
            } else {
                // Do nothing
            }
        }
        return count;
    }

    public int enumToEnumMapCountMatches(@NotNull Map<Color, Color> map) {
        int count = 0;
        for (Map.Entry<Color, Color> entry: map.entrySet()) {
            if (entry.getKey() == entry.getValue() && entry.getKey() != null) {
                count++;
            }
        }
        return count;
    }

    public State findState(int code) {
        return State.findStateByCode(code);
    }

    public Map<Color, Integer> countValuesInArray(@NotNull Color[] colors) {
        HashMap<Color, Integer> counters = new HashMap<>();
        for (Color c : colors) {
            if (c != null) {
                Integer value = counters.getOrDefault(c, 0);
                counters.put(c, value + 1);
            }
        }
        return counters;
    }

    public int countRedInArray(@NotNull Color[] colors) {
        int count = 0;
        for (Color c : colors) {
            if (c == Color.RED) {
                count++;
            }
        }
        return count;
    }
}
