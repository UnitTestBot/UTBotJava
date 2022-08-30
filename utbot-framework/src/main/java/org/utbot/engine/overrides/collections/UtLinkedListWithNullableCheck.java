package org.utbot.engine.overrides.collections;

import java.util.Collection;

/**
 * This list forbids inserting null elements to support some implementations of {@link java.util.Deque} like
 * {@link java.util.ArrayDeque}.
 *
 * TODO: <a href="https://github.com/UnitTestBot/UTBotJava/issues/819">Support super calls in inherited wrappers</a>
 *
 * @see UtLinkedList
 * @param <E>
 */
public class UtLinkedListWithNullableCheck<E> extends UtLinkedList<E> {
    @SuppressWarnings("unused")
    UtLinkedListWithNullableCheck(RangeModifiableUnlimitedArray<E> elementData, int fromIndex, int toIndex) {
        super(elementData, fromIndex, toIndex);
        for (int i = elementData.begin; i < elementData.end; i++) {
            if (elementData.get(i) == null) {
                throw new NullPointerException();
            }
        }
    }

    @SuppressWarnings("unused")
    public UtLinkedListWithNullableCheck() {
        super();
    }

    @SuppressWarnings("unused")
    public UtLinkedListWithNullableCheck(Collection<? extends E> c) {
        super(c);
    }

    @Override
    public E set(int index, E element) {
        if (element == null) {
            throw new NullPointerException();
        }
        preconditionCheck();
        rangeCheck(index);
        E oldElement = elementData.get(index + elementData.begin);
        elementData.set(index + elementData.begin, element);
        return oldElement;
    }

    @Override
    public void addFirst(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        preconditionCheck();
        elementData.set(--elementData.begin, e);
    }

    @Override
    public void addLast(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        preconditionCheck();
        elementData.set(elementData.end++, e);
    }

    @Override
    public boolean offerFirst(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        preconditionCheck();
        elementData.set(--elementData.begin, e);
        return true;
    }

    @Override
    public boolean offerLast(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        preconditionCheck();
        elementData.set(elementData.end++, e);
        return true;
    }

    @Override
    public void add(int index, E element) {
        if (element == null) {
            throw new NullPointerException();
        }
        preconditionCheck();
        rangeCheckForAdd(index);
        elementData.end++;
        elementData.insert(index + elementData.begin, element);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (Object elem : c.toArray()) {
            if (elem == null) {
                throw new NullPointerException();
            }
        }
        preconditionCheck();
        elementData.setRange(elementData.end, c.toArray(), 0, c.size());
        elementData.end += c.size();
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        for (Object elem : c.toArray()) {
            if (elem == null) {
                throw new NullPointerException();
            }
        }
        preconditionCheck();
        rangeCheckForAdd(index);
        elementData.insertRange(index + elementData.begin, c.toArray(), 0, c.size());
        elementData.end += c.size();
        return true;
    }
}
