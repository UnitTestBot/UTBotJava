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

    public int countEqualColors(@NotNull Color a, @NotNull Color b, @NotNull Color c) {
        int equalToA = 1;
        if (b == a) {
            equalToA++;
        }

        if (c == a) {
            equalToA++;
        }

        int equalToB = 1;
        if (a == b) {
            equalToB++;
        }

        if (c == b) {
            equalToB++;
        }

        if (equalToA > equalToB) {
            return equalToA;
        } else {
            return equalToB;
        }
    }

    public int countNullColors(Color a, Color b) {
        int nullCount = 0;
        if (a == null) {
            nullCount++;
        }

        if (b == null) {
            nullCount++;
        }

        return nullCount;
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

    public Map<Color, Integer> countValuesInArray(Color @NotNull [] colors) {
        HashMap<Color, Integer> counters = new HashMap<>();
        for (Color c : colors) {
            if (c != null) {
                Integer value = counters.getOrDefault(c, 0);
                counters.put(c, value + 1);
            }
        }
        return counters;
    }

    public int countRedInArray(@NotNull Color @NotNull [] colors) {
        int count = 0;
        for (Color c : colors) {
            if (c == Color.RED) {
                count++;
            }
        }
        return count;
    }
}
