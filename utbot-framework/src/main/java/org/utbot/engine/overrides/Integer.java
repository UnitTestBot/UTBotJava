package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;
import org.utbot.engine.overrides.strings.UtStringBuilder;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.api.mock.UtMock.assumeOrExecuteConcretely;
import static org.utbot.engine.overrides.UtLogicMock.ite;
import static org.utbot.engine.overrides.UtLogicMock.less;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;

@UtClassMock(target = java.lang.Integer.class, internalUsage = true)
public class Integer {
    @SuppressWarnings({"UnnecessaryBoxing", "unused", "deprecation"})
    public static java.lang.Integer valueOf(int x) {
        return new java.lang.Integer(x);
    }

    @SuppressWarnings({"unused", "DuplicatedCode", "IfStatementWithIdenticalBranches"})
    public static int parseInt(String s, int radix) {
        if (s == null) {
            throw new NumberFormatException();
        }
        if (radix < 2) { // MIN_RADIX
            throw new NumberFormatException();
        }
        if (radix > 36) { // MAX_RADIX
            throw new NumberFormatException();
        }
        if (s.length() == 0) {
            throw new NumberFormatException();
        }
        if ((s.charAt(0) == '-' || s.charAt(0) == '+') && s.length() == 1) {
            throw new NumberFormatException();
        }
        assumeOrExecuteConcretely(s.length() <= 10);
        // we need two branches to add more options for concrete executor to find both branches
        if (s.charAt(0) == '-') {
            executeConcretely();
            return java.lang.Integer.parseInt(s);
        } else {
            executeConcretely();
            return java.lang.Integer.parseInt(s);
        }
    }

    @SuppressWarnings({"unused", "DuplicatedCode"})
    public static int parseUnsignedInt(String s, int radix) {
        if (s == null) {
            throw new NumberFormatException();
        }
        if (radix < 2) { // MIN_RADIX
            throw new NumberFormatException();
        }
        if (radix > 36) { // MAX_RADIX
            throw new NumberFormatException();
        }
        if (s.length() == 0) {
            throw new NumberFormatException();
        }
        if ((s.charAt(0) == '-' || s.charAt(0) == '+') && s.length() == 1) {
            throw new NumberFormatException();
        }
        assumeOrExecuteConcretely(s.length() <= 10);
        if (s.charAt(0) == '-') {
            throw new NumberFormatException();
        } else {
            executeConcretely();
            return java.lang.Integer.parseUnsignedInt(s);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static String toString(int i) {
        if (i == 0x80000000) { // java.lang.Integer.MIN_VALUE
            return "-2147483648";
        }

        if (i == 0) {
            return "0";
        }

        // isNegative = i < 0
        boolean isNegative = less(i, 0);
        String prefix = ite(isNegative, "-", "");

        int value = ite(isNegative, -i, i);
        char[] reversed = new char[10];
        int offset = 0;
        while (value > 0) {
            reversed[offset] = (char) ('0' + (value % 10));
            value /= 10;
            offset++;
        }

        char[] buffer = new char[offset];
        int counter = 0;
        while (offset > 0) {
            offset--;
            buffer[counter++] = reversed[offset];
        }
        return prefix + new String(buffer);
    }
}
