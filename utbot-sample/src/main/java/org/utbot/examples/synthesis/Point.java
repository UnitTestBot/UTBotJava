package org.utbot.examples.synthesis;

import java.util.Objects;

public class Point implements SynthesisInterface {

    private int x;
    private int y;

    public Point(int area) {
        this.x = area / 10;
        this.y = area - 10;
    }

    @Override
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point)) return false;
        Point point = (Point) o;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}