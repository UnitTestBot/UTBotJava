package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;

@UtClassMock(target = java.lang.System.class, internalUsage = true)
public class System {
    /**
     * Copies an array from the specified source array, beginning at the
     * specified position, to the specified position of the destination array.
     * A subsequence of array components are copied from the source
     * array referenced by <code>src</code> to the destination array
     * referenced by <code>dest</code>. The number of components copied is
     * equal to the <code>length</code> argument. The components at
     * positions <code>srcPos</code> through
     * <code>srcPos+length-1</code> in the source array are copied into
     * positions <code>destPos</code> through
     * <code>destPos+length-1</code>, respectively, of the destination
     * array.
     * <p>
     * If the <code>src</code> and <code>dest</code> arguments refer to the
     * same array object, then the copying is performed as if the
     * components at positions <code>srcPos</code> through
     * <code>srcPos+length-1</code> were first copied to a temporary
     * array with <code>length</code> components and then the contents of
     * the temporary array were copied into positions
     * <code>destPos</code> through <code>destPos+length-1</code> of the
     * destination array.
     * <p>
     * If <code>dest</code> is <code>null</code>, then a
     * <code>NullPointerException</code> is thrown.
     * <p>
     * If <code>src</code> is <code>null</code>, then a
     * <code>NullPointerException</code> is thrown and the destination
     * array is not modified.
     * <p>
     * Otherwise, if any of the following is true, an
     * <code>ArrayStoreException</code> is thrown and the destination is
     * not modified:
     * <ul>
     * <li>The <code>src</code> argument refers to an object that is not an
     *     array.
     * <li>The <code>dest</code> argument refers to an object that is not an
     *     array.
     * <li>The <code>src</code> argument and <code>dest</code> argument refer
     *     to arrays whose component types are different primitive types.
     * <li>The <code>src</code> argument refers to an array with a primitive
     *    component type and the <code>dest</code> argument refers to an array
     *     with a reference component type.
     * <li>The <code>src</code> argument refers to an array with a reference
     *    component type and the <code>dest</code> argument refers to an array
     *     with a primitive component type.
     * </ul>
     * <p>
     * Otherwise, if any of the following is true, an
     * <code>IndexOutOfBoundsException</code> is
     * thrown and the destination is not modified:
     * <ul>
     * <li>The <code>srcPos</code> argument is negative.
     * <li>The <code>destPos</code> argument is negative.
     * <li>The <code>length</code> argument is negative.
     * <li><code>srcPos+length</code> is greater than
     *     <code>src.length</code>, the length of the source array.
     * <li><code>destPos+length</code> is greater than
     *     <code>dest.length</code>, the length of the destination array.
     * </ul>
     * <p>
     * Otherwise, if any actual component of the source array from
     * position <code>srcPos</code> through
     * <code>srcPos+length-1</code> cannot be converted to the component
     * type of the destination array by assignment conversion, an
     * <code>ArrayStoreException</code> is thrown. In this case, let
     * <b><i>k</i></b> be the smallest nonnegative integer less than
     * length such that <code>src[srcPos+</code><i>k</i><code>]</code>
     * cannot be converted to the component type of the destination
     * array; when the exception is thrown, source array components from
     * positions <code>srcPos</code> through
     * <code>srcPos+</code><i>k</i><code>-1</code>
     * will already have been copied to destination array positions
     * <code>destPos</code> through
     * <code>destPos+</code><i>k</I><code>-1</code> and no other
     * positions of the destination array will have been modified.
     * (Because of the restrictions already itemized, this
     * paragraph effectively applies only to the situation where both
     * arrays have component types that are reference types.)
     *
     * @param src     the source array.
     * @param srcPos  starting position in the source array.
     * @param dest    the destination array.
     * @param destPos starting position in the destination data.
     * @param length  the number of array elements to be copied.
     * @throws IndexOutOfBoundsException if copying would cause
     *                                   access of data outside array bounds.
     * @throws ArrayStoreException       if an element in the <code>src</code>
     *                                   array could not be stored into the <code>dest</code> array
     *                                   because of a type mismatch.
     * @throws NullPointerException      if either <code>src</code> or
     *                                   <code>dest</code> is <code>null</code>.
     */
    @SuppressWarnings({"unused"})
    public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
        if (src == null || dest == null) {
            throw new NullPointerException();
        }

        if (srcPos < 0 || destPos < 0 || length < 0) {
            throw new IndexOutOfBoundsException();
        }

        if (src instanceof boolean[]) {
            if (!(dest instanceof boolean[])) {
                throw new ArrayStoreException();
            }

            boolean[] srcArray = (boolean[]) src;
            boolean[] destArray = (boolean[]) dest;

            if (srcPos + length > srcArray.length) {
                throw new IndexOutOfBoundsException();
            }

            UtArrayMock.arraycopy(srcArray, srcPos, destArray, destPos, length);
        } else if (src instanceof byte[]) {
            if (!(dest instanceof byte[])) {
                throw new ArrayStoreException();
            }

            byte[] srcArray = (byte[]) src;
            byte[] destArray = (byte[]) dest;

            if (srcPos + length > srcArray.length) {
                throw new IndexOutOfBoundsException();
            }

            UtArrayMock.arraycopy(srcArray, srcPos, destArray, destPos, length);
        } else if (src instanceof char[]) {
            if (!(dest instanceof char[])) {
                throw new ArrayStoreException();
            }

            char[] srcArray = (char[]) src;
            char[] destArray = (char[]) dest;

            if (srcPos + length > srcArray.length) {
                throw new IndexOutOfBoundsException();
            }

            UtArrayMock.arraycopy(srcArray, srcPos, destArray, destPos, length);
        } else if (src instanceof int[]) {
            if (!(dest instanceof int[])) {
                throw new ArrayStoreException();
            }

            int[] srcArray = (int[]) src;
            int[] destArray = (int[]) dest;

            if (srcPos + length > srcArray.length) {
                throw new IndexOutOfBoundsException();
            }

            UtArrayMock.arraycopy(srcArray, srcPos, destArray, destPos, length);
        } else if (src instanceof long[]) {
            if (!(dest instanceof long[])) {
                throw new ArrayStoreException();
            }

            long[] srcArray = (long[]) src;
            long[] destArray = (long[]) dest;

            if (srcPos + length > srcArray.length) {
                throw new IndexOutOfBoundsException();
            }

            UtArrayMock.arraycopy(srcArray, srcPos, destArray, destPos, length);
        } else if (src instanceof short[]) {
            if (!(dest instanceof short[])) {
                throw new ArrayStoreException();
            }

            short[] srcArray = (short[]) src;
            short[] destArray = (short[]) dest;

            if (srcPos + length > srcArray.length) {
                throw new IndexOutOfBoundsException();
            }

            UtArrayMock.arraycopy(srcArray, srcPos, destArray, destPos, length);
        } else if (src instanceof float[]) {
            if (!(dest instanceof float[])) {
                throw new ArrayStoreException();
            }

            float[] srcArray = (float[]) src;
            float[] destArray = (float[]) dest;

            if (srcPos + length > srcArray.length) {
                throw new IndexOutOfBoundsException();
            }

            UtArrayMock.arraycopy(srcArray, srcPos, destArray, destPos, length);
        } else if (src instanceof double[]) {
            if (!(dest instanceof double[])) {
                throw new ArrayStoreException();
            }

            double[] srcArray = (double[]) src;
            double[] destArray = (double[]) dest;

            if (srcPos + length > srcArray.length) {
                throw new IndexOutOfBoundsException();
            }

            UtArrayMock.arraycopy(srcArray, srcPos, destArray, destPos, length);
        } else {
            if (!(dest instanceof Object[])) {
                throw new ArrayStoreException();
            }

            Object[] srcArray = (Object[]) src;
            Object[] destArray = (Object[]) dest;

            if (srcPos + length > srcArray.length) {
                throw new IndexOutOfBoundsException();
            }

            UtArrayMock.arraycopy(srcArray, srcPos, destArray, destPos, length);
        }
    }
}
