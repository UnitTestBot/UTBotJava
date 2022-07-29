package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;
import org.utbot.engine.overrides.strings.UtNativeString;
import org.utbot.engine.overrides.strings.UtString;
import org.utbot.engine.overrides.strings.UtStringBuilder;

import static org.utbot.api.mock.UtMock.assumeOrExecuteConcretely;
import static org.utbot.engine.overrides.UtLogicMock.ite;
import static org.utbot.engine.overrides.UtLogicMock.less;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;

@UtClassMock(target = java.lang.Long.class, internalUsage = true)
public class Long {
    @SuppressWarnings({"UnnecessaryBoxing", "unused", "deprecation"})
    public static java.lang.Long valueOf(long x) {
        return new java.lang.Long(x);
    }

    @SuppressWarnings({"unused", "DuplicatedCode", "IfStatementWithIdenticalBranches"})
    public static long parseLong(String s, int radix) {
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
            return java.lang.Long.parseLong(s, radix);
        } else {
            executeConcretely();
            return java.lang.Long.parseLong(s, radix);
        }
    }

    @SuppressWarnings({"unused", "DuplicatedCode"})
    public static long parseUnsignedLong(String s, int radix) {
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
            return java.lang.Long.parseUnsignedLong(s, radix);
        }
    }

    public static String toString(long l) {
        if (l == 0x8000000000000000L) { // java.lang.Long.MIN_VALUE
            return "-9223372036854775808";
        }
        // assumes are placed here to limit search space of solver
        // and reduce time of solving queries with bv2int expressions
        assumeOrExecuteConcretely(l <= 10000);
        assumeOrExecuteConcretely(l >= -10000);
        // condition = l < 0
        boolean condition = less(l, 0);
        // prefix = condition ? "-" : ""
        String prefix = ite(condition, "-", "");
        UtStringBuilder sb = new UtStringBuilder(prefix);
        // value = condition ? -l : l
        long value = ite(condition, -l, l);
        return sb.append(new UtString(new UtNativeString(value)).toStringImpl()).toString();
    }
}
