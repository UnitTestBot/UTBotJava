package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;

@UtClassMock(target = java.lang.Character.class, internalUsage = true)
public class Character {
    @SuppressWarnings({"UnnecessaryBoxing", "unused", "deprecation"})
    public static java.lang.Character valueOf(char x) {
        return new java.lang.Character(x);
    }

    @SuppressWarnings("unused")
    public static boolean isWhitespace(char ch) {
        switch (ch) {
            case ' ':
            case '\n':
            case '\t':
            case '\f':
            case '\r':
                return true;
            default:
                return false;
        }
    }

    @SuppressWarnings("unused")
    public static char toLowerCase(char ch) {
        if (ch >= 'A' && ch <= 'Z') {
            return (char) (ch - 'A' + 'a');
        } else {
            return ch;
        }
    }

    @SuppressWarnings("unused")
    public static char toUpperCase(char ch) {
        if (ch >= 'a' && ch <= 'z') {
            return (char) (ch - 'a' + 'A');
        } else {
            return ch;
        }
    }
}
