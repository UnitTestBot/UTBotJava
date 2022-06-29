package org.utbot.examples.enums;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static org.utbot.examples.enums.ClassWithEnum.ManyConstantsEnum.A;

public class EnumCollections {

    public enum Color {
        RED,
        GREEN,
        BLUE
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
}
