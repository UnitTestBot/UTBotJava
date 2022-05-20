package org.utbot.examples.statics.substitution;

public class StaticSubstitutionExamples {

    public int lessThanZero() {
        int value = StaticSubstitution.mutableValue;
        return value > 0 ? value : 0;
    }
}
