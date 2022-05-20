package org.utbot.examples.objects;

import java.util.Objects;

public class ObjectWithPrimitivesClass {
    public int valueByDefault = 5;

    public int x, y;
    public short shortValue;
    double weight;

    public ObjectWithPrimitivesClass() {
    }

    public ObjectWithPrimitivesClass(int x, int y, double weight) {
        this.x = x;
        this.y = y;
        this.weight = weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectWithPrimitivesClass that = (ObjectWithPrimitivesClass) o;
        return valueByDefault == that.valueByDefault && x == that.x && y == that.y && Double.compare(that.weight, weight) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueByDefault, x, y, weight);
    }

    @Override
    public String toString() {
        return String.format(
                "ObjectWithPrimitivesClass(valueByDefault = %d, x = %d, y = %d, shortValue = %d, weight = %f)",
                valueByDefault,
                x,
                y,
                shortValue,
                weight
        );
    }
}
