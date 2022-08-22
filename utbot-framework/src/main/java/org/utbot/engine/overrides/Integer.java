package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;
import org.utbot.engine.overrides.strings.UtNativeString;
import org.utbot.engine.overrides.strings.UtString;
import org.utbot.engine.overrides.strings.UtStringBuilder;

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

    public static String toString(int i) {
        if (i == 0x80000000) { // java.lang.MIN_VALUE
            return "-2147483648";
        }
        // assumes are placed here to limit search space of solver
        // and reduce time of solving queries with bv2int expressions
        assumeOrExecuteConcretely(i <= 0x8000);
        assumeOrExecuteConcretely(i >= -0x8000);
        // condition = i < 0
        boolean condition = less(i, 0);
        // prefix = condition ? "-" : ""
        String prefix = ite(condition, "-", "");
        UtStringBuilder sb = new UtStringBuilder(prefix);
        // value = condition ? -i : i
        int value = ite(condition, -i, i);
        return sb.append(new UtString(new UtNativeString(value)).toStringImpl()).toString();
    }
}
