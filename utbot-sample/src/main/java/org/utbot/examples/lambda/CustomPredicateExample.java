package org.utbot.examples.lambda;

public class CustomPredicateExample {
    static int someStaticField = 5;
    int someNonStaticField = 10;

    public boolean noCapturedValuesPredicateCheck(PredicateNoCapturedValues<Integer> predicate, int x) {
        //noinspection RedundantIfStatement
        if (predicate.test(x)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean capturedLocalVariablePredicateCheck(PredicateCapturedLocalVariable<Integer> predicate, int x) {
        //noinspection RedundantIfStatement
        if (predicate.test(x)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean capturedParameterPredicateCheck(PredicateCapturedParameter<Integer> predicate, int x) {
        //noinspection RedundantIfStatement
        if (predicate.test(x)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean capturedStaticFieldPredicateCheck(PredicateCapturedStaticField<Integer> predicate, int x) {
        //noinspection RedundantIfStatement
        if (predicate.test(x)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean capturedNonStaticFieldPredicateCheck(PredicateCapturedNonStaticField<Integer> predicate, int x) {
        //noinspection RedundantIfStatement
        if (predicate.test(x)) {
            return true;
        } else {
            return false;
        }
    }

    // this method contains implementation of functional interface 'CustomPredicate'
    void someLambdas(int someParameter) {
        PredicateNoCapturedValues<Integer> predicate1 = (x) -> x == 5;

        int localVariable = 10;
        PredicateCapturedLocalVariable<Integer> predicate2 = (x) -> x + localVariable == 5;

        PredicateCapturedParameter<Integer> predicate3 = (x) -> x + someParameter == 5;

        PredicateCapturedStaticField<Integer> predicate4 = (x) -> x + someStaticField == 5;

        PredicateCapturedNonStaticField<Integer> predicate5 = (x) -> x + someNonStaticField == 5;
    }
}
