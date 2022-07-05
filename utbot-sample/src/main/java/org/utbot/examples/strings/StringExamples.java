package org.utbot.examples.strings;

import java.util.Arrays;

import static java.lang.Boolean.valueOf;

class IntPair {
    int fst;
    int snd;

    @Override
    public String toString() {
        return fst + ", " + snd;
    }
}

public class StringExamples {
    public String concat(String fst, String snd) {
        return fst + snd;
    }

    public String concatWithObject(IntPair pair) {
        if (pair == null) {
            return "fst.toString() = " + pair;
        } else {
            return "fst.toString() = " + pair;
        }
    }

    public String stringConstants(String s) {
        return "String('" + s + "')";
    }

    @SuppressWarnings("ConstantConditions")
    public boolean containsOnLiterals() {
        return "abcdef".contains("ab");
    }

    public String intToString(int a, int b) {
        if (a > b) {
            return Integer.toString(a);
        } else {
            return Integer.toString(b);
        }
    }

    public String longToString(long a, long b) {
        if (a > b) {
            return Long.toString(a);
        } else {
            return Long.toString(b);
        }
    }

    public String startsWithLiteral(String str) {
        if (str.startsWith("1234567890")) {
            str = str.replace("3", "A");
        } else {
            str = str.trim();
        }

        if (str.charAt(0) == 'x') {
            return str;
        } else {
            return str.toLowerCase();
        }
    }

    public String byteToString(byte a, byte b) {
        if (a > b) {
            return Byte.toString(a);
        } else {
            return Byte.toString(b);
        }
    }

    public String replace(String a, String b) {
        return a.replace("abc", b);
    }

    public String charToString(char a, char b) {
        if (a > b) {
            return Character.toString(a);
        } else {
            return Character.toString(b);
        }
    }

    public String shortToString(short a, short b) {
        if (a > b) {
            return Short.toString(a);
        } else {
            return Short.toString(b);
        }
    }

    public String booleanToString(boolean a, boolean b) {
        if (a ^ b) {
            return Boolean.toString(a ^ b);
        } else {
            return Boolean.toString(a ^ b);
        }
    }

    public int stringToInt(String s) {
        int i = Integer.valueOf(s);
        if (i < 0) {
            return i;
        } else {
            return i;
        }
    }

    public long stringToLong(String s) {
        long l = Long.valueOf(s);
        if (l < 0) {
            return l;
        } else {
            return l;
        }
    }

    public byte stringToByte(String s) {
        byte b = Byte.valueOf(s);
        if (b < 0) {
            return b;
        } else {
            return b;
        }
    }

    public short stringToShort(String s) {
        short si = Short.valueOf(s);
        if (si < 0) {
            return si;
        } else {
            return si;
        }
    }

    public boolean stringToBoolean(String s) {
        boolean b = valueOf(s);
        if (b) {
            return b;
        } else {
            return b;
        }
    }

    public String concatWithInts(int a, int b) {
        if (a == b) {
            throw new IllegalArgumentException("Interval is wrong, a equals to b: (" + a + ", " + b + ")");
        }
        if (a < b) {
            return "a < b, a:" + a + ", b:" + b;
        }
        return "a > b, a:" + a + ", b:" + b;
    }

    @SuppressWarnings({"StringBufferMayBeStringBuilder", "StringBufferReplaceableByString"})
    public String useStringBuffer(String fst, String snd) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(fst);
        buffer.append(", ");
        buffer.append(snd);
        return buffer.toString();
    }

    public String nullableStringBuffer(StringBuffer buffer, int i) {
        if (i >= 0) {
            buffer.append("Positive");
        } else {
            buffer.append("Negative");
        }
        return buffer.toString();
    }

    public boolean isValidUuid(String uuid) {
        return isNotBlank(uuid) && uuid
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    public boolean isValidUuidShortVersion(String uuid) {
        return uuid != null && uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    public boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    // Apache StringUtils
    public boolean isBlank(CharSequence cs) {
        int strLen = length(cs);
        if (strLen != 0) {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }

        }
        return true;
    }

    // Apache StringUtils
    public int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    public boolean longer(CharSequence cs, int i) {
        if (i <= 0) {
            throw new IllegalArgumentException();
        }
        return length(cs) > i;
    }

    public boolean equalChar(CharSequence cs) {
        return (cs.charAt(0) == "abc".charAt(0));
    }

    public String substring(String s, int o) {
        String subs = s.substring(o);
        if (subs.equals("password") && o != 0) {
            return subs;
        } else if (subs.equals("password")) {
            return subs;
        } else {
            return subs;
        }
    }

    public String substringWithEndIndex(String s, int b, int e) {
        String subs = s.substring(b, e);
        if (subs.equals("password") && b != 0 && e != s.length()) {
            return subs;
        } else {
            return subs;
        }
    }

    public String substringWithEndIndexNotEqual(String s, int e) {
        return s.substring(1, e);
    }

    @SuppressWarnings("StringOperationCanBeSimplified")
    public boolean fullSubstringEquality(String s) {
//        // TODO: otherwise string length is negative somewhere in substring
//        if (s.length() < 3) {
//            return false;
//        }
        return s.substring(0).equals(s);
    }

    @SuppressWarnings("StringEquality")
    public int useIntern(String s) {
        String abc = "abc";
        if (!s.equals(abc)) {
            return 1;
        }
        String s1 = s.substring(1);
        String s2 = abc.substring(1);
        if (s1 == s2) {
            return 2;
        }
        String s3 = s1.intern();
        String s4 = s2.intern();
        return s3 == s4 ? 3 : 4;
    }

    public int prefixAndSuffix(String s) {
        if (s.length() != 5) {
            return 0;
        }
        if (!s.startsWith("ab")) {
            return 1;
        }
        if (!s.endsWith("de")) {
            return 2;
        }
        return s.contains("+") ? 3 : 4;
    }

    public int prefixWithTwoArgs(String s) {
        return s.startsWith("abc", 1) ? 1 : 2;
    }

    public int prefixWithOffset(int o) {
        return "babc".startsWith("abc", o) ? 1 : 2;
    }

    public boolean startsWith(String s, String prefix) {
        if (prefix.length() < 2) {
            throw new IllegalArgumentException();
        }
        if (s.startsWith(prefix)) {
            return true;
        } else {
            return false;
        }
    }

    public int startsWithOffset(String s, String prefix, int index) {
        if (prefix.length() < 2) {
            throw new IllegalArgumentException();
        }
        boolean starts = s.startsWith(prefix, index);
        if (starts && index > 0) {
            return 0;
        } else if (starts) {
            return 1;
        } else {
            return 2;
        }
    }

    public boolean endsWith(String s, String suffix) {
        if (suffix.length() < 2) {
            throw new IllegalArgumentException();
        }
        boolean ends = s.endsWith(suffix);
        if (ends) {
            return true;
        } else {
            return false;
        }
    }

    public String replaceAll(String s, String regex, String replacement) {
        return s.replaceAll(regex, replacement);
    }

    public int indexOf(String s, String find) {
        int i = s.indexOf(find);
        if (i > 0) {
            return i;
        } else if (i == 0) {
            return i;
        } else {
            return i;
        }
    }

    public int indexOfWithOffset(String s, String find, int offset) {
        int i = s.indexOf(find, offset);
        if (i > offset && offset > 0) {
            return i;
        } else if (i == offset) {
            return i;
        } else {
            return i;
        }
    }

    public int lastIndexOf(String s, String find) {
        int i = s.lastIndexOf(find);
        if (i < s.length() - find.length()) {
            return i;
        } else if (i >= 0) {
            return i;
        } else {
            return i;
        }
    }

    public int lastIndexOfWithOffset(String s, String find, int offset) {
        int i = s.lastIndexOf(find, offset);
        if (i >= 0 && i < offset - find.length() && offset < s.length()) {
            return i;
        } else if (i >= 0) {
            return i;
        } else {
            return i;
        }
    }

    public int compareCodePoints(String s, String t, int i) {
        boolean less = s.codePointAt(i) < t.codePointAt(i);
        if (i == 0 && less) {
            return 0;
        } else if (i != 0 && less) {
            return 1;
        } else if (i == 0){
            return 2;
        } else {
            return 3;
        }
    }

    public char[] toCharArray(String s) {
        return s.toCharArray();
    }

    // Quark related, SAT-1056
    public Object getObj(String str) {
        return str;
    }

    // More complex case
    public Object getObjWithCondition(String str) {
        if (str == null) {
            return "null";
        }
        if (str.equals("BEDA")) {
            return "48858";
        }
        return str;
    }

    public String equalsIgnoreCase(String s) {
        if ("SUCCESS".equalsIgnoreCase(s)) {
            return "success";
        } else {
            return "failure";
        }
    }

    public String listToString() {
        return Arrays.asList("a", "b", "c").toString();
    }
}
