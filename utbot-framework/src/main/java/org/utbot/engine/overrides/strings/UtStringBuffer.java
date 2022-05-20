package org.utbot.engine.overrides.strings;

import org.utbot.engine.overrides.UtArrayMock;
import java.io.Serializable;
import java.util.Arrays;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.engine.ResolverKt.MAX_STRING_SIZE;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

@SuppressWarnings("unused")
public class UtStringBuffer implements Appendable, Serializable, CharSequence {

    char[] value;
    int count;

    public UtStringBuffer(int capacity) {
        visit(this);
        value = new char[MAX_STRING_SIZE];
        count = 0;
    }

    public UtStringBuffer() {
        visit(this);
        value = new char[MAX_STRING_SIZE];
        count = 0;
    }

    void preconditionCheck() {
        if (alreadyVisited(this)) {
            return;
        }
        assume(value != null);
        parameter(value);
        assume(value.length <= MAX_STRING_SIZE);
        assume(count <= value.length && count >= 0);

        visit(this);
    }

    @Override
    public int length() {
        preconditionCheck();
        return count;
    }

    public int capacity() {
        preconditionCheck();
        return value.length;
    }


    public void ensureCapacity(int minimumCapacity) {
    }

    public void trimToSize() {
        preconditionCheck();
        if (count < value.length) {
            char[] tmp = new char[count];
            UtArrayMock.arraycopy(tmp, 0, value, 0, count);
            value = tmp;
        }
    }

    public void setLength(int newLength) {
        preconditionCheck();
        if (newLength < 0)
            throw new StringIndexOutOfBoundsException(newLength);
        ensureCapacity(newLength);

        if (count < newLength) {
            Arrays.fill(value, count, newLength, '\0');
        }

        count = newLength;
    }

    @Override
    public char charAt(int index) {
        preconditionCheck();
        if ((index < 0) || (index >= count))
            throw new StringIndexOutOfBoundsException(index);
        return value[index];
    }

    public int codePointAt(int index) {
        preconditionCheck();
        if ((index < 0) || (index >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return Character.codePointAt(value, index, count);
    }

    public int codePointBefore(int index) {
        preconditionCheck();
        int i = index - 1;
        if ((i < 0) || (i >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return Character.codePointBefore(value, index, 0);
    }

    public int codePointCount(int beginIndex, int endIndex) {
        preconditionCheck();
        if (beginIndex < 0 || endIndex > count || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException();
        }
        return Character.codePointCount(value, beginIndex, endIndex - beginIndex);
    }

    public int offsetByCodePoints(int index, int codePointOffset) {
        preconditionCheck();
        if (index < 0 || index > count) {
            throw new IndexOutOfBoundsException();
        }
        return Character.offsetByCodePoints(value, 0, count, index, codePointOffset);
    }

    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        preconditionCheck();
        if (srcBegin < 0)
            throw new StringIndexOutOfBoundsException(srcBegin);
        if ((srcEnd < 0) || (srcEnd > count))
            throw new StringIndexOutOfBoundsException(srcEnd);
        if (srcBegin > srcEnd)
            throw new StringIndexOutOfBoundsException("srcBegin > srcEnd");
        UtArrayMock.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    public void setCharAt(int index, char ch) {
        preconditionCheck();
        if ((index < 0) || (index >= count))
            throw new StringIndexOutOfBoundsException(index);
        value[index] = ch;
    }

    public StringBuffer asStringBuilder() {
        return null;
    }

    public StringBuffer append(Object obj) {
        return append(String.valueOf(obj));
    }

    public StringBuffer append(String str) {
        preconditionCheck();
        if (str == null) {
            return appendNull();
        }
        int len = str.length();
        assume(count + len <= value.length);
        str.getChars(0, len, value, count);
        count += len;
        return this.asStringBuilder();
    }

    // Documentation in subclasses because of synchro difference
    public StringBuffer append(StringBuffer sb) {
        if (sb == null)
            return appendNull();
        preconditionCheck();
        int len = sb.length();
        assume(count + len <= value.length);
        sb.getChars(0, len, value, count);
        count += len;
        return this.asStringBuilder();
    }


    StringBuffer append(StringBuilder sb) {
        if (sb == null)
            return appendNull();
        preconditionCheck();
        int len = sb.length();
        assume(count + len <= value.length);
        sb.getChars(0, len, value, count);
        count += len;
        return this.asStringBuilder();
    }

    // Documentation in subclasses because of synchro difference
    @Override
    public StringBuffer append(CharSequence s) {
        if (s == null)
            return appendNull();
        if (s instanceof String)
            return this.append((String) s);
        if (s instanceof StringBuilder)
            return this.append((StringBuilder) s);
        if (s instanceof StringBuffer)
            return this.append((StringBuffer) s);

        return this.append(s, 0, s.length());
    }

    private StringBuffer appendNull() {
        preconditionCheck();
        int c = count;
        final char[] value = this.value;
        assume(count + 4 <= value.length);
        value[c++] = 'n';
        value[c++] = 'u';
        value[c++] = 'l';
        value[c++] = 'l';
        count = c;
        return this.asStringBuilder();
    }

    @Override
    public StringBuffer append(CharSequence s, int start, int end) {
        preconditionCheck();
        if (s == null) {
            s = "null";
        }
        if ((start < 0) || (start > end) || (end > s.length())) {
            throw new IndexOutOfBoundsException();
        }
        int len = end - start;
        char[] otherValue = s.toString().toCharArray();
        assume(count + len <= value.length);
        UtArrayMock.arraycopy(otherValue, start, value, count, len);
        count += len;
        return this.asStringBuilder();
    }

    public StringBuffer append(char[] str) {
        preconditionCheck();
        int len = str.length;
        assume(count + len <= value.length);
        UtArrayMock.arraycopy(str, 0, value, count, len);
        count += len;
        return this.asStringBuilder();
    }

    public StringBuffer append(char[] str, int offset, int len) {
        preconditionCheck();
        if (len > 0) {
            assume(count + len <= value.length);
            UtArrayMock.arraycopy(str, offset, value, count, len);
            count += len;
        }
        return this.asStringBuilder();
    }

    public StringBuffer append(boolean b) {
        preconditionCheck();
        if (b) {
            assume(count + 4 <= value.length);
            value[count++] = 't';
            value[count++] = 'r';
            value[count++] = 'u';
            value[count++] = 'e';
        } else {
            assume(count + 5 <= value.length);
            value[count++] = 'f';
            value[count++] = 'a';
            value[count++] = 'l';
            value[count++] = 's';
            value[count++] = 'e';
        }
        return this.asStringBuilder();
    }

    @Override
    public StringBuffer append(char c) {
        preconditionCheck();
        assume(count + 1 <= value.length);
        value[count++] = c;
        return this.asStringBuilder();
    }


    public StringBuffer append(int i) {
        return append(Integer.toString(i));
    }

    public StringBuffer append(long l) {
        return append(Long.toString(l));
    }

    public StringBuffer append(float f) {
        return append(Float.toString(f));
    }

    public StringBuffer append(double d) {
        return append(Double.toString(d));
    }

    public StringBuffer delete(int start, int end) {
        preconditionCheck();
        if (start < 0)
            throw new StringIndexOutOfBoundsException(start);
        if (end > count)
            end = count;
        if (start > end)
            throw new StringIndexOutOfBoundsException();
        int len = end - start;
        if (len > 0) {
            UtArrayMock.arraycopy(value, start + len, value, start, count - end);
            count -= len;
        }
        return this.asStringBuilder();
    }

    public StringBuffer appendCodePoint(int codePoint) {
        preconditionCheck();
        final int count = this.count;

        if (Character.isBmpCodePoint(codePoint)) {
            assume(count + 1 <= value.length);
            value[count] = (char) codePoint;
            this.count = count + 1;
        } else if (Character.isValidCodePoint(codePoint)) {
            assume(count + 2 <= value.length);
            value[count + 1] = Character.lowSurrogate(codePoint);
            value[count] = Character.highSurrogate(codePoint);
            this.count = count + 2;
        } else {
            throw new IllegalArgumentException();
        }
        return this.asStringBuilder();
    }

    public StringBuffer deleteCharAt(int index) {
        preconditionCheck();
        if ((index < 0) || (index >= count))
            throw new StringIndexOutOfBoundsException(index);
        UtArrayMock.arraycopy(value, index + 1, value, index, count - index - 1);
        count--;
        return this.asStringBuilder();
    }

    public StringBuffer replace(int start, int end, String str) {
        preconditionCheck();
        if (start < 0)
            throw new StringIndexOutOfBoundsException(start);
        if (start > count)
            throw new StringIndexOutOfBoundsException("start > length()");
        if (start > end)
            throw new StringIndexOutOfBoundsException("start > end");

        if (end > count)
            end = count;
        int len = str.length();
        int newCount = count + len - (end - start);
        assume(newCount <= value.length);

        UtArrayMock.arraycopy(value, end, value, start + len, count - end);
        char[] strValue = str.toCharArray();
        UtArrayMock.arraycopy(strValue, 0, value, start, strValue.length);
        count = newCount;
        return this.asStringBuilder();
    }

    public String substring(int start) {
        return substring(start, count);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }

    public String substring(int start, int end) {
        preconditionCheck();
        if (start < 0)
            throw new StringIndexOutOfBoundsException(start);
        if (end > count)
            throw new StringIndexOutOfBoundsException(end);
        if (start > end)
            throw new StringIndexOutOfBoundsException(end - start);
        return new String(value, start, end - start);
    }

    public StringBuffer insert(int index, char[] str, int offset, int len) {
        preconditionCheck();
        if ((index < 0) || (index > length()))
            throw new StringIndexOutOfBoundsException(index);
        if ((offset < 0) || (len < 0) || (offset > str.length - len))
            throw new StringIndexOutOfBoundsException(
                    "offset " + offset + ", len " + len + ", str.length "
                            + str.length);
        assume(count + len <= value.length);
        UtArrayMock.arraycopy(value, index, value, index + len, count - index);
        UtArrayMock.arraycopy(str, offset, value, index, len);
        count += len;
        return this.asStringBuilder();
    }

    public StringBuffer insert(int offset, Object obj) {
        return insert(offset, String.valueOf(obj));
    }

    public StringBuffer insert(int offset, String str) {
        preconditionCheck();
        if ((offset < 0) || (offset > length()))
            throw new StringIndexOutOfBoundsException(offset);
        if (str == null)
            str = "null";
        int len = str.length();
        assume(count + len <= value.length);
        UtArrayMock.arraycopy(value, offset, value, offset + len, count - offset);
        char[] strValue = str.toCharArray();
        UtArrayMock.arraycopy(strValue, 0, value, offset, len);
        count += len;
        return this.asStringBuilder();
    }

    public StringBuffer insert(int offset, char[] str) {
        preconditionCheck();
        if ((offset < 0) || (offset > length()))
            throw new StringIndexOutOfBoundsException(offset);
        int len = str.length;
        assume(count + len <= value.length);
        UtArrayMock.arraycopy(value, offset, value, offset + len, count - offset);
        UtArrayMock.arraycopy(str, 0, value, offset, len);
        count += len;
        return this.asStringBuilder();
    }

    public StringBuffer insert(int dstOffset, CharSequence s) {
        if (s == null)
            s = "null";
        if (s instanceof String)
            return this.insert(dstOffset, (String) s);
        return this.insert(dstOffset, s, 0, s.length());
    }

    public StringBuffer insert(int dstOffset, CharSequence s, int start, int end) {
        preconditionCheck();
        if (s == null) {
            s = "null";
        }
        if ((dstOffset < 0) || (dstOffset > this.length())) {
            throw new IndexOutOfBoundsException();
        }
        if ((start < 0) || (end < 0) || (start > end) || (end > s.length())) {
            throw new IndexOutOfBoundsException();
        }
        int len = end - start;
        assume(count + len <= value.length);

        UtArrayMock.arraycopy(value, dstOffset, value, dstOffset + len, count - dstOffset);
        char[] otherValue = s.toString().toCharArray();
        UtArrayMock.arraycopy(otherValue, start, value, dstOffset, len);
        count += len;
        return this.asStringBuilder();
    }

    public StringBuffer insert(int offset, boolean b) {
        return insert(offset, String.valueOf(b));
    }

    public StringBuffer insert(int offset, char c) {
        preconditionCheck();
        assume(count + 1 <= value.length);
        UtArrayMock.arraycopy(value, offset, value, offset + 1, count - offset);
        value[offset] = c;
        count += 1;
        return this.asStringBuilder();
    }

    public StringBuffer insert(int offset, int i) {
        return insert(offset, String.valueOf(i));
    }

    public StringBuffer insert(int offset, long l) {
        return insert(offset, String.valueOf(l));
    }

    public StringBuffer insert(int offset, float f) {
        return insert(offset, String.valueOf(f));
    }

    public StringBuffer insert(int offset, double d) {
        return insert(offset, String.valueOf(d));
    }

    public int indexOf(String str) {
        return indexOf(str, 0);
    }

    public int indexOf(String str, int fromIndex) {
        preconditionCheck();
        return new String(value).indexOf(str, fromIndex);
    }

    public int lastIndexOf(String str) {
        return lastIndexOf(str, count);
    }

    public int lastIndexOf(String str, int fromIndex) {
        preconditionCheck();
        return new String(value).lastIndexOf(str, fromIndex);
    }

    public StringBuffer reverse() {
        preconditionCheck();
        boolean hasSurrogates = false;
        int n = count - 1;
        for (int j = (n - 1) >> 1; j >= 0; j--) {
            int k = n - j;
            char cj = value[j];
            char ck = value[k];
            value[j] = ck;
            value[k] = cj;
            if (Character.isSurrogate(cj) ||
                    Character.isSurrogate(ck)) {
                hasSurrogates = true;
            }
        }
        if (hasSurrogates) {
            reverseAllValidSurrogatePairs();
        }
        return this.asStringBuilder();
    }

    /**
     * Outlined helper method for reverse()
     */
    private void reverseAllValidSurrogatePairs() {
        preconditionCheck();
        for (int i = 0; i < count - 1; i++) {
            char c2 = value[i];
            if (Character.isLowSurrogate(c2)) {
                char c1 = value[i + 1];
                if (Character.isHighSurrogate(c1)) {
                    value[i++] = c1;
                    value[i] = c2;
                }
            }
        }
    }

    @Override
    public String toString() {
        preconditionCheck();
        return new String(value, 0, count);
    }

    /**
     * Needed by {@code String} for the contentEquals method.
     */
    final char[] getValue() {
        preconditionCheck();
        return value;
    }


    /**
     * use serialVersionUID for interoperability
     */
    static final long serialVersionUID = 4383685877147921099L;

    public UtStringBuffer(String str) {
        visit(this);
        value = new char[MAX_STRING_SIZE];
        count = 0;
        append(str);
    }

    public UtStringBuffer(CharSequence seq) {
        visit(this);
        value = new char[MAX_STRING_SIZE];
        count = 0;
        append(seq);
    }

    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        preconditionCheck();
        s.defaultWriteObject();
        s.writeInt(count);
        s.writeObject(value);
    }

    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        preconditionCheck();
        s.defaultReadObject();
        count = s.readInt();
        value = (char[]) s.readObject();
    }
}
