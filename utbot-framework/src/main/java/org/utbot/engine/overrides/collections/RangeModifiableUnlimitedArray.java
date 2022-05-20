package org.utbot.engine.overrides.collections;

/**
 * Interface shows API for UtExpressions of infinite modifiable array.
 * <p>
 * Elements are placed in {@link #storage} in interval [{@link #begin}, {@link #end})
 */
public class RangeModifiableUnlimitedArray<E> {
    // the first index of range where elements are stored
    public int begin;
    // the index after the last index of range where elements are stored
    // end - begin = size of array
    public int end;
    // dummy array field to reserve place in memory in engine for array
    public E[] storage;

    public RangeModifiableUnlimitedArray() {

    }

    @SuppressWarnings("unused")
    public RangeModifiableUnlimitedArray(RangeModifiableUnlimitedArray<E> other, int begin, int end) { }

    /**
     * insert an element into specified index in array and move all the elements with greater or equal index
     * by one position to the right.
     * <p>
     * Engine will translate invoke of this method as new UtExpression:
     * <p>
     * UtArrayInsert(UtExpression this, UtExpression index, UtExpression element) : UtArrayExpression
     *
     * @param index   - index in list, in which specified element needs to be inserted
     * @param element - element to be inserted
     */
    @SuppressWarnings("unused")
    public void insert(int index, E element) {
    }

    /**
     * remove an element with specified index from array moving all the elements with greater index
     * by one position to the left.
     * <p>
     * Engine will translate invoke of this method as new UtExpression:
     * <p>
     * UtArrayRemove(UtExpression this, UtExpression index): UtArrayExpression
     *
     * @param index - index in list, in which element need to be removed
     */
    @SuppressWarnings("unused")
    public void remove(int index) {
    }

    /**
     * remove element with indexes in range [index, index + length) from array moving all the elements with greater
     * index than left border of range by length position to the left.
     * <p>
     * Engine will translate invoke of this method as new UtExpression:
     * <p>
     * UtArrayRemoveRange(UtExpression this, UtExpression index, UtExpression length): UtArrayExpression
     *
     * @param index  - first index, where the range of elements need to be removed
     * @param length - length of range of indices
     */
    @SuppressWarnings("unused")
    public void removeRange(int index, int length) {
    }

    /**
     * insert elements of specified collection that are stored by indexes [from..from + length]
     * into specified index in array and move all the elements with greater or equal index by
     * length positions to the right
     * <p>
     * Engine will translate invoke of this method as new UtExpression:
     * <p>
     * UtListAddRange(UtExpression this, UtExpression index, UtExpression index, UtExpression length, UtListExpression elements) : UtArrayExpression
     *
     * @param index  - index in list, in which collection of elements need to be inserted
     * @param length - length of collection
     * @param array  - elements to be inserted. Invariant: elements.size() == length.
     */
    @SuppressWarnings("unused")
    public void insertRange(int index, Object[] array, int from, int length) {
    }

    /**
     * Set elements of array that are stored by indexes [index..index + length]
     * with elements of specified array, stored by indexes [from..from + length] correspondingly
     *
     * <p>
     * Engine will translate invoke of this method as new UtExpression:
     * <p>
     * UtListSetRange(
     *      UtExpression this,
     *      UtExpression index,
     *      UtExpression elements,
     *      UtExpression from,
     *      UtExpression length
     * ) : UtArrayExpression
     *
     * @param index  - index in this array, in which collection of elements need to be set
     * @param array  - elements to be inserted.
     * @param from   - index in specified array, that defines the beginning of range
     * @param length - length of range
     */
    @SuppressWarnings("unused")
    public void setRange(int index, Object[] array, int from, int length) {
    }

    /**
     * Returns an representation of this array by array of Object,
     * where indexes are shifted by offset to the left.
     */
    @SuppressWarnings("unused")
    public Object[] toArray(int offset, int length) {
        return null;
    }

    /**
     * set specified value to the element with specified index in array.
     * <p>
     * Engine will translate invoke of this method as new UtExpression:
     * <p>
     * UtArrayMultiStoreExpression(UtExpression this, UtExpression index, UtExpression value) : UtArrayExpression
     *
     * @param index - index in list, where element needs to be changed
     * @param value - value to be set in specified position
     */
    @SuppressWarnings("unused")
    public void set(int index, E value) {
    }

    /**
     * returns the element of this array on specified index
     * <p>
     * UtArraySelectExpression(UtExpression this, UtExpression index) : UtExpression
     * <p>
     *
     * @param i - index in list with element, that needs to be returned
     */
    @SuppressWarnings("unused")
    public E get(int i) {
        return null;
    }
}