package org.utbot.engine.overrides.strings;

/**
 * An auxiliary class without implementation,
 * that specifies interface of interaction with
 * smt string objects.
 * @see org.utbot.engine.UtNativeStringWrapper
 */
@SuppressWarnings("unused")
public class UtNativeString {
    /**
     * Constructor, that creates a new symbolic string.
     * Length and content can be arbitrary and is set
     * via path constraints.
     */
    public UtNativeString() {}

    /**
     * Constructor, that creates a new string
     * that is equal smt expression: <code>int2string(i)</code>>
     *
     * if i is greater or equal to 0, than
     * constructed string will be equal to
     * <code>i.toString()</code>>, otherwise, it is undefined
     * @param i number, that needs to be converted to string
     */
    public UtNativeString(int i) { }

    /**
     * Constructor, that creates a new string
     * that is equal smt expression: <code>int2string(l)</code>
     * <p>
     * if i is greater or equal to 0, than
     * constructed string will be equal to
     * <code>l.toString()</code>, otherwise, it is undefined
     * @param l number, that needs to be converted to string
     */
    public UtNativeString(long l) {}

    /**
     * Returned variable's expression is equal to
     * <code>mkInt2BV(str.length(this), Long.SIZE_BITS)</code>
     * @return the length of this string
     */
    public int length() {
        return 0;
    }

    /**
     * If string represent a decimal positive number,
     * then returned value is equal to <code>Integer.valueOf(this)</code>>
     * Otherwise, the result is equal to -1
     * <p>
     * Returned variable's expression is equal to
     * <code>mkInt2BV(string2int(this), Long.SIZE_BITS)</code>
     */
    public int toInteger() { return 0; }

    /**
     * If string represent a decimal positive number,
     * then returned value is equal to <code>Long.valueOf(this)</code>>
     * Otherwise, the result is equal to -1L
     * <p>
     * Returned variable's expression is equal to
     * <code>mkInt2BV(string2int(this), Long.SIZE_BITS)</code>>
     */
    public long toLong() { return 0; }

    /**
     * If i in valid index range of string, then
     * a returned value's expression is equal to
     * <code>mkSeqNth(this, i).cast(char)</code>
     * @param i the index of char value to be returned
     * @return the specified char value
     */
    public char charAt(int i) {
        return '\0';
    }

    /**
     * Returns a char array with the same content as this string,
     * shifted by offset indexes to the left.
     * <p>
     * The returned value's UtExpression is equal to
     * UtStringToArray(this, offset).
     * @param offset - the number of indexes to be shifted to the left
     * @return array of the string chars with shifted indexes by specified offset.
     * @see org.utbot.engine.pc.UtArrayToString
     */
    public char[] toCharArray(int offset) { return null; }

    /**
     * If i in valid index range of string, then
     * a returned value's expression is equal to
     * <code>mkSeqNth(this, i).cast(int)</code>
     * @param i the index of codePoint value to be returned
     * @return the specified codePoint value
     */
    public int codePointAt(int i) {
        return 0;
    }
}
