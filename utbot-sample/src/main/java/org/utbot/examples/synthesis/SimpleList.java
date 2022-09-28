package org.utbot.examples.synthesis;

public class SimpleList {
    private Point[] points = new Point[10];
    private int size = 0;

    public Point get(int index) {
        if (index >= size) throw new IndexOutOfBoundsException();
        return points[index];
    }

    public void add(Point p) {
        if (points.length <= size) {
            Point[] newArr = new Point[points.length * 2];
            System.arraycopy(newArr, 0, points, 0, points.length);
            points = newArr;
        }
        points[size++] = p;
    }

    public int size() {
        return size;
    }
}
