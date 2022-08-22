package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;
import org.utbot.engine.overrides.strings.UtNativeString;
import org.utbot.engine.overrides.strings.UtString;
import org.utbot.engine.overrides.strings.UtStringBuilder;

import static org.utbot.api.mock.UtMock.assumeOrExecuteConcretely;
import static org.utbot.engine.overrides.UtLogicMock.ite;
import static org.utbot.engine.overrides.UtLogicMock.less;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;

@UtClassMock(target = java.lang.Short.class, internalUsage = true)
public class Short {
    @SuppressWarnings({"UnnecessaryBoxing", "unused", "deprecation"})
    public static java.lang.Short valueOf(short x) {
        return new java.lang.Short(x);
    }

    @SuppressWarnings({"unused", "DuplicatedCode", "IfStatementWithIdenticalBranches"})
    public static java.lang.Short parseShort(String s, int radix) {
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
            return java.lang.Short.parseShort(s);
        } else {
            executeConcretely();
            return java.lang.Short.parseShort(s);
        }
    }

    public static String toString(short s) {
        // condition = s < 0
        boolean condition = less(s, (short) 0);
        // assumes are placed here to limit search space of solver
        // and reduce time of solving queries with bv2int expressions
        assumeOrExecuteConcretely(s <= 10000);
        assumeOrExecuteConcretely(s >= -10000);
        // prefix = condition ? "-" : ""
        String prefix = ite(condition, "-", "");
        UtStringBuilder sb = new UtStringBuilder(prefix);
        // value = condition ? -i : i
        int value = ite(condition, (short)-s, s);
        return sb.append(new UtString(new UtNativeString(value)).toStringImpl()).toString();
    }
}
