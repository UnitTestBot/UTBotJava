package org.utbot.engine.overrides.strings;

import org.utbot.engine.overrides.UtArrayMock;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Locale;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;
import static java.lang.Math.min;

@SuppressWarnings({"ConstantConditions", "unused"})
public class UtString implements java.io.Serializable, Comparable<String>, CharSequence {
    char[] value;
    int length;

    private static final long serialVersionUID = -6849794470754667710L;

    public UtString(UtNativeString str) {
        visit(this);
        length = str.length();
        value = str.toCharArray(0);
    }

    public static UtNativeString toUtNativeString(String s, int offset) {
        char[] value = s.toCharArray();
        int length = value.length;
        UtNativeString nativeString = new UtNativeString();
        assume(nativeString.length() == length - offset);
        assume(nativeString.length() <= 2);
        for (int i = offset; i < length; i++) {
            assume(nativeString.charAt(i - offset) == value[i]);
        }
        return nativeString;
    }

    public UtString() {
        visit(this);
        value = new char[0];
        length = 0;
    }

    public UtString(String original) {
        visit(this);
        this.length = original.length();
        this.value = original.toCharArray();
    }

    public UtString(char[] value) {
        visit(this);
        this.length = value.length;
        this.value = value;
    }

    public UtString(char[] value, boolean share) {
        visit(this);
        this.length = value.length;
        this.value = value;
    }

    public UtString(char[] value, int offset, int count) {
        visit(this);
        if (offset < 0) {
            throw new StringIndexOutOfBoundsException(offset);
        }
        if (count <= 0) {
            if (count < 0) {
                throw new StringIndexOutOfBoundsException(count);
            }
            if (offset <= value.length) {
                this.length = 0;
                this.value = new char[0];
                return;
            }
        }
        // Note: offset or count might be near -1>>>1.
        if (offset > value.length - count) {
            throw new StringIndexOutOfBoundsException(offset + count);
        }
        this.value = new char[count];
        this.length = count;
        UtArrayMock.arraycopy(value, offset, this.value, 0, count);
    }

    @SuppressWarnings("UnnecessaryContinue")
    public UtString(int[] codePoints, int offset, int count) {
        visit(this);
        if (offset < 0) {
            throw new StringIndexOutOfBoundsException(offset);
        }
        if (count <= 0) {
            if (count < 0) {
                throw new StringIndexOutOfBoundsException(count);
            }
            if (offset <= codePoints.length) {
                this.value = new char[0];
                this.length = 0;
                return;
            }
        }
        // Note: offset or count might be near -1>>>1.
        if (offset > codePoints.length - count) {
            throw new StringIndexOutOfBoundsException(offset + count);
        }

        final int end = offset + count;

        // Pass 1: Compute precise size of char[]
        int n = count;
        for (int i = offset; i < end; i++) {
            int c = codePoints[i];
            if (java.lang.Character.isBmpCodePoint(c)) {
                continue;
            } else if (java.lang.Character.isValidCodePoint(c)) {
                n++;
            } else {
                throw new IllegalArgumentException(java.lang.Integer.toString(c));
            }
        }

        // Pass 2: Allocate and fill in char[]
        final char[] v = new char[n];

        for (int i = offset, j = 0; i < end; i++, j++) {
            int c = codePoints[i];
            if (java.lang.Character.isBmpCodePoint(c)) {
                v[j] = (char) c;
            } else {
                v[j++] = Character.highSurrogate(c);
                v[j] = Character.lowSurrogate(c);
                return;
            }
        }
        new String(codePoints, offset, count);

        this.value = v;
        this.length = n;
    }

    @Deprecated
    public UtString(byte[] ascii, int hibyte, int offset, int count) {
        checkBounds(ascii, offset, count);
        visit(this);
        char[] value = new char[count];

        if (hibyte == 0) {
            for (int i = count; i-- > 0; ) {
                value[i] = (char) (ascii[i + offset] & 0xff);
            }
        } else {
            hibyte <<= 8;
            for (int i = count; i-- > 0; ) {
                value[i] = (char) (hibyte | (ascii[i + offset] & 0xff));
            }
        }
        this.value = value;
        this.length = count;
    }

    @Deprecated
    public UtString(byte[] ascii, int hibyte) {
        this(ascii, hibyte, 0, ascii.length);
    }

    private static void checkBounds(byte[] bytes, int offset, int length) {
        if (length < 0) {
            throw new StringIndexOutOfBoundsException(length);
        }
        if (offset < 0) {
            throw new StringIndexOutOfBoundsException(offset);
        }
        if (offset > bytes.length - length) {
            throw new StringIndexOutOfBoundsException(offset + length);
        }
    }

// TODO: add symbolic implementations for the following constructors:

//    @SuppressWarnings("RedundantThrows")
//    public UtString(byte[] bytes, int offset, int length, String charsetName)
//            throws UnsupportedEncodingException {
//        assume(false);
//    }
//
//
//    public UtString(byte[] bytes, int offset, int length, Charset charset) {
//        assume(false);
//    }
//
//    @SuppressWarnings("RedundantThrows")
//    public UtString(byte[] bytes, String charsetName)
//            throws UnsupportedEncodingException {
//        assume(false);
//    }
//
//    public UtString(byte[] bytes, Charset charset) {
//        assume(false);
//    }
//
//    public UtString(byte[] bytes, int offset, int length) {
//        assume(false);
//    }
//
//    public UtString(byte[] bytes) {
//        assume(false);
//    }

    public UtString(StringBuffer buffer) {
        visit(this);
        length = buffer.length();
        value = new char[length];
        buffer.getChars(0, length, value, 0);
    }

    public UtString(StringBuilder builder) {
        visit(this);
        length = builder.length();
        value = new char[length];
        builder.getChars(0, length, value, 0);
    }

    public void preconditionCheck() {
        if (alreadyVisited(this)) {
            return;
        }
        assume(value != null);
        assume(length == value.length);
        parameter(value);
        
        visit(this);
    }

    public int length() {
        preconditionCheck();
        return length;
    }

    public boolean isEmpty() {
        preconditionCheck();
        return length == 0;
    }

    /**
     * @param index the index of the char value
     * @return the char value at the specified index of this string. The first char value is at index 0.
     */
    public char charAtImpl(int index) {
        return value[index];
    }

    public char charAt(int index) {
        preconditionCheck();
        return charAtImpl(index);
    }

    public int codePointAt(int index) {
        preconditionCheck();
        if ((index < 0) || (index >= value.length)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return Character.codePointAt(value, index);
    }

    public int codePointBefore(int index) {
        preconditionCheck();
        int i = index - 1;
        if ((i < 0) || (i >= value.length)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return Character.codePointBefore(value, index);
    }


    public int codePointCount(int beginIndex, int endIndex) {
        preconditionCheck();
        return Character.codePointCount(value, beginIndex, endIndex - beginIndex);
    }


    public int offsetByCodePoints(int index, int codePointOffset) {
        preconditionCheck();
        return Character.offsetByCodePoints(value, 0, value.length, index, codePointOffset);
    }

    @SuppressWarnings("DuplicatedCode")
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        preconditionCheck();
        if (srcBegin < 0) {
            throw new StringIndexOutOfBoundsException(srcBegin);
        }
        if (srcEnd > length) {
            throw new StringIndexOutOfBoundsException(srcEnd);
        }
        if (srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
        }
        UtArrayMock.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    @Deprecated
    @SuppressWarnings("DuplicatedCode")
    public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
        preconditionCheck();
        if (srcBegin < 0) {
            throw new StringIndexOutOfBoundsException(srcBegin);
        }
        if (srcEnd > length) {
            throw new StringIndexOutOfBoundsException(srcEnd);
        }
        if (srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
        }
        for (int i = 0; i < srcEnd - srcBegin; i++) {
            dst[i + dstBegin] = (byte) value[i];
        }
    }

    @SuppressWarnings("RedundantThrows")
    public byte[] getBytes(String charsetName)
            throws UnsupportedEncodingException {
        if (charsetName == null) {
            throw new NullPointerException();
        }
        assume(charsetName.length() <= 10);
        executeConcretely();
        return toString().getBytes(charsetName);
    }

    public byte[] getBytes(Charset charset) {
        if (charset == null) {
            throw new NullPointerException();
        }
        executeConcretely();
        return toString().getBytes(charset);
    }

    public byte[] getBytes() {
        executeConcretely();
        return toString().getBytes();
    }

    private boolean contentEquals(char[] otherValue, int toffset, int ooffset, int n) {
        //TODO: remove assume
        assume(n <= 25);
        for (int j = 0; j < n; j++) {
            int i1 = toffset + j;
            int i2 = ooffset + j;
            if (value[i1] != otherValue[i2]) {
                return false;
            }
        }
        return true;
    }

    // for simpler analysis instead of contentEquals(otherValue, 0, 0, int n)
    private boolean contentEqualsZeroOffset(char[] otherValue, int n) {
        //TODO: remove assume
        assume(n <= 25);
        for (int i = 0; i < n; i++) {
            if (value[i] != otherValue[i]) {
                return false;
            }
        }

        return true;
    }

    public boolean equals(Object anObject) {
        preconditionCheck();
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof String) {
            String anotherString = (String) anObject;
            int n = length;
            if (n == anotherString.length()) {
                char[] v2 = anotherString.toCharArray();
                return contentEqualsZeroOffset(v2, n);
            }
        }
        return false;
    }

    public boolean contentEquals(StringBuffer sb) {
        return contentEquals((CharSequence) sb);
    }

    @SuppressWarnings("DuplicatedCode")
    private boolean nonSyncContentEquals(StringBuffer sb) {
        char[] v1 = value;
        int n = length;
        if (n != sb.length()) {
            return false;
        }
        char[] v2 = new char[n];
        sb.getChars(0, n, v2, 0);
        return contentEqualsZeroOffset(v2, n);
    }

    @SuppressWarnings("DuplicatedCode")
    private boolean nonSyncContentEquals(StringBuilder sb) {
        char[] v1 = value;
        int n = length;
        if (n != sb.length()) {
            return false;
        }
        char[] v2 = new char[n];
        sb.getChars(0, n, v2, 0);
        return contentEqualsZeroOffset(v2, n);
    }

    public boolean contentEquals(CharSequence cs) {
        preconditionCheck();
        // Argument is a StringBuffer, StringBuilder
        if (cs instanceof StringBuffer) {
            return contentEquals((StringBuffer) cs);
        } else if (cs instanceof StringBuilder) {
            return nonSyncContentEquals((StringBuilder) cs);
        }

        // Argument is a String
        if (cs instanceof String) {
            return equals(cs);
        }

        // Argument is a generic CharSequence
        char[] v1 = value;
        int n = length;
        if (n != cs.length()) {
            return false;
        }
        // TODO: remove assume
        assume(n <= 25);
        for (int i = 0; i < n; i++) {
            if (v1[i] != cs.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean equalsIgnoreCase(String anotherString) {
        preconditionCheck();

        if (anotherString == null) {
            return false;
        }

        int n = anotherString.length();
        if (length != n) {
            return false;
        }
        char[] otherValue = anotherString.toCharArray();
        // TODO remove assume
        assume(n <= 25);
        for (int j = 0; j < n; j++) {
            if (java.lang.Character.toLowerCase(value[j]) != java.lang.Character.toLowerCase(otherValue[j])) {
                return false;
            }
        }
        return true;
    }


    public int compareTo(String anotherString) {
        preconditionCheck();
        int len1 = length;
        int len2 = anotherString.length();
        int lim = min(len1, len2);
        char[] v1 = value;
        char[] v2 = anotherString.toCharArray();
        for (int k = 0; k < lim; k++) {
            char c1 = v1[k];
            char c2 = v2[k];
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }

    private static class CaseInsensitiveComparator
            implements Comparator<CharSequence>, java.io.Serializable {
        // use serialVersionUID from JDK 1.2.2 for interoperability
        private static final long serialVersionUID = 8575799808933029326L;

        public int compare(CharSequence s1, CharSequence s2) {
            int n1 = s1.length();
            int n2 = s2.length();
            int min = min(n1, n2);
            for (int i = 0; i < min; i++) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(i);
                if (c1 != c2) {
                    c1 = java.lang.Character.toUpperCase(c1);
                    c2 = java.lang.Character.toUpperCase(c2);
                    if (c1 != c2) {
                        c1 = java.lang.Character.toLowerCase(c1);
                        c2 = java.lang.Character.toLowerCase(c2);
                        if (c1 != c2) {
                            // No overflow because of numeric promotion
                            return c1 - c2;
                        }
                    }
                }
            }
            return n1 - n2;
        }

        /**
         * Replaces the de-serialized object.
         */
        private Object readResolve() {
            return CASE_INSENSITIVE_ORDER;
        }
    }

    public static final Comparator<CharSequence> CASE_INSENSITIVE_ORDER
            = new UtString.CaseInsensitiveComparator();

    public int compareToIgnoreCase(String str) {
        preconditionCheck();
        return CASE_INSENSITIVE_ORDER.compare(this, str);
    }


    public boolean regionMatches(int toffset, String other, int ooffset,
                                 int len) {
        preconditionCheck();
        // Note: toffset, ooffset, or len might be near -1>>>1.
        if ((ooffset < 0) || (toffset < 0)
                || (toffset > (long) length - len)
                || (ooffset > (long) other.length() - len)) {
            return false;
        }
        char[] pa = other.toCharArray();
        return contentEquals(pa, toffset, ooffset, len);
    }

    public boolean regionMatches(boolean ignoreCase, int toffset,
                                 String other, int ooffset, int len) {
        preconditionCheck();
        // Note: toffset, ooffset, or len might be near -1>>>1.
        if ((ooffset < 0) || (toffset < 0)
                || (toffset > (long) length - len)
                || (ooffset > (long) other.length() - len)) {
            return false;
        }
        char[] pa = other.toCharArray();

        if (ignoreCase) {
            // TODO remove assume
            assume(len <= 25);
            for (int j = 0; j < len; j++) {
                if (java.lang.Character.toLowerCase(value[j + toffset]) != java.lang.Character.toLowerCase(pa[j + ooffset])) {
                    return false;
                }
            }
        }
        return contentEquals(pa, toffset, ooffset, len);
    }

    public boolean startsWith(String prefix, int toffset) {
        preconditionCheck();
        if (toffset < 0 || toffset > length - prefix.length()) {
            return false;
        }
        char[] pa = prefix.toCharArray();
        return contentEquals(pa, toffset, 0, prefix.length());
    }

    public boolean startsWith(String prefix) {
        preconditionCheck();
        if (prefix.length() > length) {
            return false;
        }
        char[] pa = prefix.toCharArray();
        return contentEqualsZeroOffset(pa, prefix.length());
    }

    public boolean endsWith(String suffix) {
        preconditionCheck();
        if (suffix.length() > length) {
            return false;
        }
        char[] pa = suffix.toCharArray();
        return contentEquals(pa, length - suffix.length(), 0, suffix.length());
    }

    public int hashCode() {
        preconditionCheck();
        return 0xFF;
    }

    public int indexOf(int ch) {
        return indexOf(ch, 0);
    }

    public int indexOf(int ch, int fromIndex) {
        preconditionCheck();
        final int max = length;
        if (fromIndex < 0) {
            fromIndex = 0;
        } else if (fromIndex >= max) {
            // Note: fromIndex might be near -1>>>1.
            return -1;
        }

        if (ch < java.lang.Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            // handle most cases here (ch is a BMP code point or a
            // negative value (invalid code point))
            // TODO remove assume
            assume(max - fromIndex <= 10);
            for (int i = fromIndex; i < max; i++) {
                if (value[i] == ch) {
                    return i;
                }
            }
            return -1;
        } else {
            return indexOfSupplementary(ch, fromIndex);
        }
    }

    private int indexOfSupplementary(int ch, int fromIndex) {
        if (java.lang.Character.isValidCodePoint(ch)) {
            final char hi = java.lang.Character.highSurrogate(ch);
            final char lo = java.lang.Character.lowSurrogate(ch);
            final int max = length - 1;
            // TODO remove assume
            assume(max - fromIndex <= 10);
            for (int i = fromIndex; i < max; i++) {
                if (value[i] == hi && value[i + 1] == lo) {
                    return i;
                }
            }
        }
        return -1;
    }


    public int lastIndexOf(int ch) {
        return lastIndexOf(ch, length - 1);
    }

    public int lastIndexOf(int ch, int fromIndex) {
        preconditionCheck();
        if (ch < java.lang.Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            // handle most cases here (ch is a BMP code point or a
            // negative value (invalid code point))
            for (int i = min(fromIndex, length); i >= 0; i--) {
                if (value[i] == ch) {
                    return i;
                }
            }
            return -1;
        } else {
            return lastIndexOfSupplementary(ch, fromIndex);
        }
    }

    private int lastIndexOfSupplementary(int ch, int fromIndex) {
        if (java.lang.Character.isValidCodePoint(ch)) {
            char hi = java.lang.Character.highSurrogate(ch);
            char lo = java.lang.Character.lowSurrogate(ch);
            for (int i = min(fromIndex, length - 2); i >= 0; i--) {
                if (value[i] == hi && value[i + 1] == lo) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int indexOf(String str) {
        return indexOf(str, 0);
    }

    public int indexOf(String str, int fromIndex) {
        preconditionCheck();
        int strLength = str.length();
        final int max = length - strLength + 1;
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (strLength == 0) {
            return fromIndex;
        }
        char[] v2 = str.toCharArray();
        char ch = v2[0];
        // TODO remove assume
        assume(max - fromIndex <= 10);
        for (int i = fromIndex; i < max; i++) {
            if (value[i] == ch && contentEquals(v2, i + 1, 1, strLength - 1)) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(String str) {
        return lastIndexOf(str, length);
    }

    public int lastIndexOf(String str, int fromIndex) {
        preconditionCheck();
        int strLength = str.length();
        if (strLength == 0) {
            if (fromIndex > length)
                return length;
            if (fromIndex < 0)
                return -1;
            return fromIndex;
        }
        if (fromIndex + strLength > length) {
            fromIndex = length - strLength;
        }
        char[] v2 = str.toCharArray();
        char ch = v2[0];
        // TODO remove assume
        assume(fromIndex <= 10);
        for (int i = fromIndex; i >= 0; i--) {
            if (value[i] == ch && contentEquals(v2, i + 1, 1, strLength - 1)) {
                return i;
            }
        }
        return -1;
    }

    public String substring(int beginIndex) {
        preconditionCheck();
        if (beginIndex < 0) {
            throw new StringIndexOutOfBoundsException(beginIndex);
        }
        int subLen = value.length - beginIndex;
        if (subLen < 0) {
            throw new StringIndexOutOfBoundsException(subLen);
        }
        return (beginIndex == 0) ? this.toStringImpl() : new String(value, beginIndex, subLen);
    }

    public String substring(int beginIndex, int endIndex) {
        preconditionCheck();
        if (beginIndex < 0) {
            throw new StringIndexOutOfBoundsException(beginIndex);
        }
        if (endIndex > value.length) {
            throw new StringIndexOutOfBoundsException(endIndex);
        }
        int subLen = endIndex - beginIndex;
        if (subLen < 0) {
            throw new StringIndexOutOfBoundsException(subLen);
        }
        return ((beginIndex == 0) && (endIndex == value.length))
                ? this.toStringImpl()
                : new String(value, beginIndex, subLen);
    }

    public CharSequence subSequence(int beginIndex, int endIndex) {
        preconditionCheck();
        return this.substring(beginIndex, endIndex);
    }

    public String concat(String str) {
        preconditionCheck();
        char[] val = value;
        char[] otherVal = str.toCharArray();
        if (str.length() == 0) {
            return this.toStringImpl();
        }
        char[] newValue = new char[length + str.length()];
        UtArrayMock.arraycopy(value, 0, newValue, 0, length);
        UtArrayMock.arraycopy(otherVal, 0, newValue, length + 1, str.length());
        return new String(newValue);
    }

    public String replace(char oldChar, char newChar) {
        preconditionCheck();
        if (oldChar != newChar) {
            int len = value.length;
            int i = -1;
            char[] val = value; /* avoid getfield opcode */

            while (++i < len) {
                if (val[i] == oldChar) {
                    break;
                }
            }
            if (i < len) {
                char[] newValue = new char[len];
                UtArrayMock.arraycopy(value, 0, newValue, 0, len);
                while (i < len) {
                    char c = val[i];
                    newValue[i] = (c == oldChar) ? newChar : c;
                    i++;
                }
                return new String(newValue);
            }
        }
        return this.toStringImpl();
    }

    /**
     * @param regex the regular expression to which this string is to be matched
     * @return {@code true} if, and only if, this string matches the given regular expression
     */
    public boolean matchesImpl(String regex) {
        return toString().matches(regex);
    }

    public boolean matches(String regex) {
        preconditionCheck();
        return matchesImpl(regex);
    }

    public boolean contains(CharSequence s) {
        return indexOf(s.toString()) > -1;
    }

    @SuppressWarnings("DuplicatedCode")
    public String replaceFirst(String regex, String replacement) {
        preconditionCheck();
        if (regex == null) {
            throw new NullPointerException();
        }
        if (replacement == null) {
            throw new NullPointerException();
        }
        assume(regex.length() < 10);
        assume(replacement.length() < 10);
        executeConcretely();
        return toString().replaceFirst(regex, replacement);
    }

    @SuppressWarnings("DuplicatedCode")
    public String replaceAll(String regex, String replacement) {
        preconditionCheck();
        if (regex == null) {
            throw new NullPointerException();
        }
        if (replacement == null) {
            throw new NullPointerException();
        }
        assume(regex.length() < 10);
        assume(replacement.length() < 10);
        executeConcretely();
        return toString().replaceAll(regex, replacement);
    }

    @SuppressWarnings("DuplicatedCode")
    public String replace(CharSequence target, CharSequence replacement) {
        preconditionCheck();
        if (target == null) {
            throw new NullPointerException();
        }
        if (replacement == null) {
            throw new NullPointerException();
        }
        assume(target.length() < 10);
        assume(replacement.length() < 10);
        executeConcretely();
        return toString().replace(target, replacement);
    }

    public String[] split(String regex, int limit) {
        preconditionCheck();
        if (regex == null) {
            throw new NullPointerException();
        }
        if (limit < 0) {
            throw new IllegalArgumentException();
        }
        if (regex.length() == 0) {
            int size = limit == 0 ? length + 1 : min(limit, length + 1);
            String[] strings = new String[size];
            strings[size] = substring(size - 1);
            // TODO remove assume
            assume(size < 10);
            for (int i = 0; i < size - 1; i++) {
                strings[i] = Character.toString(value[i]);
            }
            return strings;
        }
        assume(regex.length() < 10);
        executeConcretely();
        return toStringImpl().split(regex, limit);
    }

    public String[] split(String regex) {
        preconditionCheck();
        if (regex == null) {
            throw new NullPointerException();
        }
        if (regex.length() == 0) {
            String[] strings = new String[length + 1];
            strings[length] = "";
            // TODO remove assume
            assume(length <= 25);
            for (int i = 0; i < length; i++) {
                strings[i] = Character.toString(value[i]);
            }
            return strings;
        }
        executeConcretely();
        return toStringImpl().split(regex);
    }

    public String toLowerCase(Locale locale) {
        preconditionCheck();
        if (locale == null) {
            throw new NullPointerException();
        }
        executeConcretely();
        return toStringImpl().toLowerCase(locale);
    }

    public String toLowerCase() {
        preconditionCheck();
        char[] newValue = UtArrayMock.copyOf(value, length);
        for (int i = 0; i < length; i++) {
            newValue[i] = Character.toLowerCase(value[i]);
        }
        return new String(value);
    }

    public String toUpperCase(Locale locale) {
        preconditionCheck();
        if (locale == null) {
            throw new NullPointerException();
        }
        executeConcretely();
        return toStringImpl().toUpperCase(locale);
    }

    public String toUpperCase() {
        preconditionCheck();
        char[] newValue = UtArrayMock.copyOf(value, length);
        for (int i = 0; i < length; i++) {
            newValue[i] = Character.toUpperCase(value[i]);
        }
        return new String(value);
    }

    public String trim() {
        preconditionCheck();
        int len = length;
        int st = 0;
        char[] val = value;

        while ((st < len) && (val[st] <= ' ')) {
            st++;
        }
        while ((st < len) && (val[len - 1] <= ' ')) {
            len--;
        }
        return ((st > 0) || (len < length)) ? substring(st, len) : this.toStringImpl();
    }

    /**
     * @return this object (which is already a string!) is itself returned.
     */
    public String toStringImpl() {
        return new String(value);
    }

    public String toString() {
        preconditionCheck();
        return toStringImpl();
    }

    public char[] toCharArray() {
        preconditionCheck();
        return UtArrayMock.copyOf(value, length);
    }

    // TODO perhaps we should implement it with cache JIRA:1512
    public String intern() {
        executeConcretely();
        return toStringImpl();
    }
}
