package org.utbot.examples.statics.substitution;

/**
 * A class with static fields.
 * If a field is not final, we have two options:
 * - to use value from static initializer only
 * - to replace this value with symbolic variable
 * This setting is configured in UtSettings.
 */
public class StaticSubstitution {
    public static int mutableValue;
    public static final int finalValue = 5;

    static {
        mutableValue = finalValue;
    }
}
