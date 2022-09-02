package org.utbot.examples.lambda;

/**
 * This functional interface is implemented via one lambda in {@link CustomPredicateExample#someLambdas}.
 *
 * DO NOT implement it anymore, because test on {@link CustomPredicateExample#capturedLocalVariablePredicateCheck}
 * relies on the fact that there is only one implementation and that implementation is lambda.
 * In addition, in this case we want the implementing lambda to capture some local variable.
 *
 * It is important because we want to test how we generate tests when the only available implementation is lambda,
 * and we want to check different cases: with or without captured values. Note that lambdas may capture
 * local variables, method parameters, static and non-static fields. That is why we have multiple functional interfaces
 * in this package: one for each case.
 */
@FunctionalInterface
public interface PredicateCapturedLocalVariable<T> {
    boolean test(T value);
}
