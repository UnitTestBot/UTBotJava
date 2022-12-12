package org.utbot.fuzzing.samples;

import java.util.regex.Pattern;

@SuppressWarnings({"unused", "SimplifiableConditionalExpression"})
public class Strings {

    // should find a string that starts with "bad!" prefix
    public static void test(String s) {
        if (s.charAt(0) == "b".charAt(0)) {
            if (s.charAt(1) == "a".charAt(0)) {
                if (s.charAt(2) == "d".charAt(0)) {
                    if (s.charAt(3) == "!".charAt(0)) {
                        throw new IllegalArgumentException();
                    }
                }
            }
        }
    }

    // should try to find the string with size 6 and with "!" in the end
    public static void testStrRem(String str) {
        if (!"world???".equals(str) && str.charAt(5) == '!' && str.length() == 6) {
            throw new RuntimeException();
        }
    }

    public boolean isValidUuid(String uuid) {
        return isNotBlank(uuid) && uuid
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    public boolean isValidUuidShortVersion(String uuid) {
        return uuid != null && uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    public boolean isValidDouble(String value) {
        return value.matches("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?([fFdD]?)") && value.contains("E") && value.contains("-");
    }

    static final String pattern = "\\d+";

    public boolean isNumber(String s) {
        return Pattern.matches(pattern, s) ? true : false;
    }

    private static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    private static boolean isBlank(CharSequence cs) {
        int strLen = length(cs);
        if (strLen != 0) {
            for (int i  = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }

        }
        return true;
    }

    private static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
}
