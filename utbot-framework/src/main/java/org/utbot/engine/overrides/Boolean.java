package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;

@UtClassMock(target = java.lang.Boolean.class, internalUsage = true)
public class Boolean {
    @SuppressWarnings({"UnnecessaryBoxing", "unused", "deprecation"})
    public static java.lang.Boolean valueOf(boolean x) {
        return new java.lang.Boolean(x);
    }

    public static java.lang.Boolean valueOf(String s) {
        return java.lang.Boolean.parseBoolean(s) ? java.lang.Boolean.valueOf(true) : java.lang.Boolean.valueOf(false);
    }

    @SuppressWarnings("unused")
    public static boolean parseBoolean(String s) {
        return s != null && s.equalsIgnoreCase("true");
    }
}
