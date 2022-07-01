package org.utbot.examples.enums;

import org.jetbrains.annotations.NotNull;
import org.utbot.api.mock.UtMock;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EnumCollections {

    public enum Color {
        RED,
        GREEN,
        BLUE
    }

    public List<Color> copyColors(@NotNull List<Color> source) {
        if (source.isEmpty())
            return new LinkedList<>();
        for (Color color : source) {
            UtMock.assume(color != null);
        }
       return source;
    }

    public List<Boolean> copyBooleans(@NotNull List<Boolean> source) {
        if (source.isEmpty())
            return new LinkedList<>();
        for (Boolean value : source) {
            UtMock.assume(value != null);
        }
        return source;
    }

    public List<Boolean> copyBooleansExplicitLoop(@NotNull List<Boolean> source) {
        if (source.isEmpty())
            return new LinkedList<>();
        for (int i = 0; i < source.size(); i++) {
            UtMock.assume(source.get(i) != null);
        }
        return source;
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
}
