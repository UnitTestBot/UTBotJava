package org.utbot.examples.casts;

import java.util.Objects;

public class ColoredPoint extends Point implements Colorable {
    public int color;

    public ColoredPoint(int x, int y, int color) {
        super(x, y);
        setColor(color);
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String toString() {
        return super.toString() + "@" + color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColoredPoint that = (ColoredPoint) o;
        return color == that.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color);
    }
}