package org.utbot.examples.lambda;

import java.util.function.*;

public class PredicateNotExample {
    public boolean predicateNotExample(int a) {
        if (Predicate.not(i -> i.equals(5)).test(a)) {
            return true;
        } else {
            return false;
        }
    }
}
