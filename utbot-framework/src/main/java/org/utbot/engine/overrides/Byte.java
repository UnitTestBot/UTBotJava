package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;
import org.utbot.engine.overrides.strings.UtStringBuilder;

import java.util.Arrays;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.engine.overrides.UtLogicMock.ite;
import static org.utbot.api.mock.UtMock.assumeOrExecuteConcretely;
import static org.utbot.engine.overrides.UtLogicMock.less;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;

@UtClassMock(target = java.lang.Byte.class, internalUsage = true)
public class Byte {
    @SuppressWarnings({"UnnecessaryBoxing", "unused", "deprecation"})
    public static java.lang.Byte valueOf(byte x) {
        return new java.lang.Byte(x);
    }

    @SuppressWarnings({"unused", "DuplicatedCode", "IfStatementWithIdenticalBranches"})
    public static byte parseByte(String s, int radix) {
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
            return java.lang.Byte.parseByte(s);
        } else {
            executeConcretely();
            return java.lang.Byte.parseByte(s);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static String toString(byte b) {
        // condition = b < 0
        boolean condition = less(b, (byte) 0);
        // assumes are placed here to limit search space of solver
        // and reduce time of solving queries with bv2int expressions
        assume(b < 128);
        assume(b >= -128);

        if (b == -128) {
            return "-128";
        } else {
            String prefix = ite(condition, "-", "");
            int value = ite(condition, (byte) -b, b);
            char[] reversed = new char[3];
            int offset = 0;
            while (value > 0) {
                reversed[offset] = (char) ('0' + value % 10);
                value = value / 10;
                offset++;
            }

            if (offset > 0) {
                char[] buffer = new char[offset];
                int counter = 0;
                while (offset > 0) {
                    offset--;
                    buffer[counter++] = reversed[offset];
                }
                return prefix + new String(buffer);
            } else {
                return "0";
            }
        }
    }
}
