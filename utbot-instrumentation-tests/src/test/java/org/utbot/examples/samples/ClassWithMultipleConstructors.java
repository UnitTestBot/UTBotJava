package org.utbot.examples.samples;

public class ClassWithMultipleConstructors {
    int sum;

    public ClassWithMultipleConstructors() {
        sum = 0;
    }

    public ClassWithMultipleConstructors(int x) {
        this();
        sum = x;
    }

    public ClassWithMultipleConstructors(int x, int y) {
        this(x);
        sum += y;
    }

    public ClassWithMultipleConstructors(int x, int y, int z) {
        this(x, y);
        sum += z;
        if (x * x < 0) {
            throw new IllegalStateException("The square of a real number cannot be less than zero!");
        }
    }

    public ClassWithMultipleConstructors(String x, String y) {
        this(Integer.parseInt(x), Integer.parseInt(y));
    }

    public ClassWithMultipleConstructors(String x) {
        switch (x) {
            case "one":
                sum = 1;
                break;
            case "two":
                sum = 2;
                break;
            default:
                sum = -1;
                break;
        }
    }

    @Override
    public String toString() {
        return "sum=" + sum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassWithMultipleConstructors that = (ClassWithMultipleConstructors) o;
        return sum == that.sum;
    }
}
