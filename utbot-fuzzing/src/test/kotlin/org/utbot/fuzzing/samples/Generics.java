package org.utbot.fuzzing.samples;

@SuppressWarnings("unused")
public class Generics<T extends java.util.List<Number>> {

    private final T[] value;

    public Generics(T[] value) {
        this.value = value;
    }

    // should generate data with numbers to sum it
    public double getSum() {
        double sum = 0;
        for (T numbers : value) {
            for (Number number : numbers) {
                if (number.doubleValue() > 0) {
                    sum += number.doubleValue();
                }
            }
        }
        if (sum == 0.0) {
            throw new IllegalStateException();
        }
        return sum;
    }
}
