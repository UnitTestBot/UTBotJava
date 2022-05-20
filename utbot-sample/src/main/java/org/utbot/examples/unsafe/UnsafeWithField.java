package org.utbot.examples.unsafe;

import java.text.NumberFormat.Field;

public class UnsafeWithField {
    Field field;

    Field setField(Field f) {
        field = f;
        return Field.INTEGER;
    }
}