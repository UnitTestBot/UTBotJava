package org.utbot.examples.objects;

import java.util.Objects;

public class WrappedInt {
    private int value;

    public WrappedInt(int value) {
        this.value = value;
    }

    public WrappedInt() {
        this(0);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedInt that = (WrappedInt) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
