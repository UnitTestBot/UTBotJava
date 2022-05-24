package org.utbot.examples.lambda;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public class SimpleLambdaExamples {
    public int biFunctionLambdaExample(int a, int b) {
        BiFunction<Integer, Integer, Integer> division = (numerator, divisor) -> numerator / divisor;

        return division.apply(a, b);
    }

    @SuppressWarnings("Convert2MethodRef")
    public Predicate<Object> choosePredicate(boolean isNotNullPredicate) {
        if (isNotNullPredicate) {
            return (o -> o != null);
        } else {
            return (o -> o == null);
        }
    }
}
