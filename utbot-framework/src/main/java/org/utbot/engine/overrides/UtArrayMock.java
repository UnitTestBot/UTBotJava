package org.utbot.engine.overrides;

import org.utbot.engine.overrides.collections.RangeModifiableUnlimitedArray;
import java.util.Arrays;

/**
 * Auxiliary class with static methods without implementation.
 * These static methods are just markers for {@link org.utbot.engine.Traverser}.,
 * to do some corresponding behavior, that can't be represent
 * with java instructions.
 * <p>
 * <code>UtArrayMock</code> is used to mock expression with arrays, that
 * can be represented mores simple for smt solver or symbolic engine
 */
@SuppressWarnings("unused")
public class UtArrayMock {
    /**
     * Traversing this instruction by {@link org.utbot.engine.Traverser} should
     * behave similar to call of {@link java.util.Arrays#copyOf(Object[], int)}
     * if length is less or equals to src.length, otherwise first
     * src.length elements are equal to src, but the rest are undefined.
     * <p>
     * After traversing instruction with this invoke array expression
     * under address of the new copy will be equal to array expression
     * under address of src.
     *
     * @param src    the array to be copied
     * @param length the length of the copy to be returned
     * @return if the specified length is less then src.length
     * than method returns a result of {@link java.util.Arrays#copyOf(Object[], int)}
     */
    public static Object[] copyOf(Object[] src, int length) {
        return Arrays.copyOf(src, length);
    }

    /**
     * @param src    the array to be copied
     * @param length the length of the copy to be returned
     * @return if the specified length is less than src.length
     * than method returns a result of {@link java.util.Arrays#copyOf(char[], int)}
     * @see UtArrayMock#copyOf(Object[], int)
     */
    public static char[] copyOf(char[] src, int length) {
        return Arrays.copyOf(src, length);
    }

    /**
     * Traversing this instruction by {@link org.utbot.engine.Traverser} should
     * behave similar to call of
     * {@link java.lang.System#arraycopy(Object, int, Object, int, int)}
     * if all the arguments are valid.
     * <p>
     * After traversing instruction with this invoke the array expression,
     * that is stored under dst address will be equal to
     * <p>
     * <code>UtArraySet(dst#expr, dstPos, src#expr, srcPos, length)</code>
     *
     * @param src     the source array.
     * @param srcPos  starting position in the source array.
     * @param dst     the destination array.
     * @param destPos starting position in the destination data.
     * @param length  the number of array elements to be copied.
     * @see RangeModifiableUnlimitedArray#setRange(int, Object[], int, int)
     */
    public static void arraycopy(Object[] src, int srcPos, Object[] dst, int destPos, int length) {
        System.arraycopy(src, srcPos, dst, destPos, length);
    }

    /**
     * Traversing this instruction by {@link org.utbot.engine.Traverser} should
     * behave similar to call of
     * {@link java.lang.System#arraycopy(Object, int, Object, int, int)}
     * if all the arguments are valid.
     * @param src     the source array.
     * @param srcPos  starting position in the source array.
     * @param dst     the destination array.
     * @param destPos starting position in the destination data.
     * @param length  the number of array elements to be copied.
     * @see #arraycopy(Object[], int, Object[], int, int)
     */
    public static void arraycopy(boolean[] src, int srcPos, boolean[] dst, int destPos, int length) {
        System.arraycopy(src, srcPos, dst, destPos, length);
    }

    /**
     * Traversing this instruction by {@link org.utbot.engine.Traverser} should
     * behave similar to call of
     * {@link java.lang.System#arraycopy(Object, int, Object, int, int)}
     * if all the arguments are valid.
     * @param src     the source array.
     * @param srcPos  starting position in the source array.
     * @param dst     the destination array.
     * @param destPos starting position in the destination data.
     * @param length  the number of array elements to be copied.
     * @see #arraycopy(Object[], int, Object[], int, int)
     */
    public static void arraycopy(byte[] src, int srcPos, byte[] dst, int destPos, int length) {
        System.arraycopy(src, srcPos, dst, destPos, length);
    }

    /**
     * Traversing this instruction by {@link org.utbot.engine.Traverser} should
     * behave similar to call of
     * {@link java.lang.System#arraycopy(Object, int, Object, int, int)}
     * if all the arguments are valid.
     * @param src     the source array.
     * @param srcPos  starting position in the source array.
     * @param dst     the destination array.
     * @param destPos starting position in the destination data.
     * @param length  the number of array elements to be copied.
     * @see #arraycopy(Object[], int, Object[], int, int)
     */
    public static void arraycopy(char[] src, int srcPos, char[] dst, int destPos, int length) {
        System.arraycopy(src, srcPos, dst, destPos, length);
    }

    /**
     * Traversing this instruction by {@link org.utbot.engine.Traverser} should
     * behave similar to call of
     * {@link java.lang.System#arraycopy(Object, int, Object, int, int)}
     * if all the arguments are valid.
     * @param src     the source array.
     * @param srcPos  starting position in the source array.
     * @param dst     the destination array.
     * @param destPos starting position in the destination data.
     * @param length  the number of array elements to be copied.
     * @see #arraycopy(Object[], int, Object[], int, int)
     */
    public static void arraycopy(short[] src, int srcPos, short[] dst, int destPos, int length) {
        System.arraycopy(src, srcPos, dst, destPos, length);
    }

    /**
     * Traversing this instruction by {@link org.utbot.engine.Traverser} should
     * behave similar to call of
     * {@link java.lang.System#arraycopy(Object, int, Object, int, int)}
     * if all the arguments are valid.
     * @param src     the source array.
     * @param srcPos  starting position in the source array.
     * @param dst     the destination array.
     * @param destPos starting position in the destination data.
     * @param length  the number of array elements to be copied.
     * @see #arraycopy(Object[], int, Object[], int, int)
     */
    public static void arraycopy(int[] src, int srcPos, int[] dst, int destPos, int length) {
        System.arraycopy(src, srcPos, dst, destPos, length);
    }

    /**
     * Traversing this instruction by {@link org.utbot.engine.Traverser} should
     * behave similar to call of
     * {@link java.lang.System#arraycopy(Object, int, Object, int, int)}
     * if all the arguments are valid.
     * @param src     the source array.
     * @param srcPos  starting position in the source array.
     * @param dst     the destination array.
     * @param destPos starting position in the destination data.
     * @param length  the number of array elements to be copied.
     * @see #arraycopy(Object[], int, Object[], int, int)
     */
    public static void arraycopy(long[] src, int srcPos, long[] dst, int destPos, int length) {
        System.arraycopy(src, srcPos, dst, destPos, length);
    }

    /**
     * Traversing this instruction by {@link org.utbot.engine.Traverser} should
     * behave similar to call of
     * {@link java.lang.System#arraycopy(Object, int, Object, int, int)}
     * if all the arguments are valid.
     * @param src     the source array.
     * @param srcPos  starting position in the source array.
     * @param dst     the destination array.
     * @param destPos starting position in the destination data.
     * @param length  the number of array elements to be copied.
     * @see #arraycopy(Object[], int, Object[], int, int)
     */
    public static void arraycopy(float[] src, int srcPos, float[] dst, int destPos, int length) {
        System.arraycopy(src, srcPos, dst, destPos, length);
    }

    /**
     * Traversing this instruction by {@link org.utbot.engine.Traverser} should
     * behave similar to call of
     * {@link java.lang.System#arraycopy(Object, int, Object, int, int)}
     * if all the arguments are valid.
     * @param src     the source array.
     * @param srcPos  starting position in the source array.
     * @param dst     the destination array.
     * @param destPos starting position in the destination data.
     * @param length  the number of array elements to be copied.
     * @see #arraycopy(Object[], int, Object[], int, int)
     */
    public static void arraycopy(double[] src, int srcPos, double[] dst, int destPos, int length) {
        System.arraycopy(src, srcPos, dst, destPos, length);
    }
}
