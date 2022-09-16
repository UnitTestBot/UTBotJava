package org.utbot.engine.overrides.collections;

import org.utbot.engine.overrides.UtArrayMock;
import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.utbot.engine.overrides.stream.UtStream;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.engine.ResolverKt.MAX_LIST_SIZE;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;
import static org.utbot.api.mock.UtMock.assumeOrExecuteConcretely;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

/**
 * @param <E>
 * @see java.util.LinkedList
 * @see org.utbot.engine.ListWrapper
 */
public class UtLinkedList<E> extends AbstractSequentialList<E>
        implements List<E>, Deque<E>, Cloneable, java.io.Serializable, UtGenericStorage<E> {

    private static final long serialVersionUID = 876323262645176354L;


    RangeModifiableUnlimitedArray<E> elementData;

    @SuppressWarnings({"unused"})
    UtLinkedList(RangeModifiableUnlimitedArray<E> elementData, int fromIndex, int toIndex) {
        visit(this);
        this.elementData = new RangeModifiableUnlimitedArray<>(elementData, fromIndex, toIndex);
    }

    @SuppressWarnings({"unused"})
    public UtLinkedList() {
        visit(this);
        elementData = new RangeModifiableUnlimitedArray<>();
        elementData.begin = 0;
        elementData.end = 0;
    }

    @SuppressWarnings({"unused"})
    public UtLinkedList(Collection<? extends E> c) {
        visit(this);
        elementData = new RangeModifiableUnlimitedArray<>();
        elementData.begin = 0;
        elementData.end = 0;
        addAll(c);
    }


    /**
     * Precondition check is called only once by object,
     * if it was passed as parameter to method under test.
     * <p>
     * Preconditions that are must be satisfied:
     * <li> elementData.size in 0..MAXIMUM_LIST_SIZE. </li>
     * <li> elementData is marked as parameter </li>
     * <li> elementData.storage and it's elements are marked as parameters </li>
     */
    protected void preconditionCheck() {
        if (alreadyVisited(this)) {
            return;
        }
        setEqualGenericType(elementData);
        assume(elementData != null);
        assume(elementData.storage != null);
        parameter(elementData);
        parameter(elementData.storage);
        assume(elementData.begin == 0);
        assume(elementData.end >= 0);
        assumeOrExecuteConcretely(elementData.end <= MAX_LIST_SIZE);

        visit(this);
    }

    protected void rangeCheck(int index) {
        if (index < 0 || index >= elementData.end - elementData.begin) {
            throw new IndexOutOfBoundsException();
        }
    }

    protected void rangeCheckForAdd(int index) {
        if (index < 0 || index > elementData.end - elementData.begin) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        preconditionCheck();
        return new UtLinkedListIterator(index + elementData.begin);
    }

    @Override
    public boolean isEmpty() {
        preconditionCheck();
        return elementData.end == elementData.begin;
    }

    @Override
    public int size() {
        preconditionCheck();
        return elementData.end - elementData.begin;
    }

    @Override
    public void addFirst(E e) {
        preconditionCheck();
        elementData.set(--elementData.begin, e);
    }

    @Override
    public void addLast(E e) {
        preconditionCheck();
        elementData.set(elementData.end++, e);
    }

    @Override
    public boolean offerFirst(E e) {
        preconditionCheck();
        elementData.set(--elementData.begin, e);
        return true;
    }

    @Override
    public boolean offerLast(E e) {
        preconditionCheck();
        elementData.set(elementData.end++, e);
        return true;
    }

    private void throwIfEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }


    @Override
    public E removeFirst() {
        preconditionCheck();
        throwIfEmpty();
        return elementData.get(elementData.begin++);
    }

    @Override
    public E removeLast() {
        preconditionCheck();
        throwIfEmpty();
        return elementData.get(--elementData.end);
    }

    @Override
    public E pollFirst() {
        preconditionCheck();
        if (isEmpty()) {
            return null;
        }
        return elementData.get(elementData.begin++);
    }

    @Override
    public E pollLast() {
        preconditionCheck();
        if (isEmpty()) {
            return null;
        }
        return elementData.get(--elementData.end);
    }

    @Override
    public E getFirst() {
        preconditionCheck();
        throwIfEmpty();
        return elementData.get(elementData.begin);
    }

    @Override
    public E getLast() {
        preconditionCheck();
        throwIfEmpty();
        return elementData.get(elementData.end - 1);
    }

    @Override
    public E peekFirst() {
        preconditionCheck();
        if (isEmpty()) {
            return null;
        }
        return elementData.get(elementData.begin);
    }

    @Override
    public E peekLast() {
        preconditionCheck();
        if (isEmpty()) {
            return null;
        }
        return elementData.get(elementData.end - 1);
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        preconditionCheck();
        if (o == null) {
            for (int i = elementData.begin; i < elementData.end; i++) {
                if (elementData.get(i) == null) {
                    elementData.end--;
                    elementData.remove(i);
                    return true;
                }
            }
        } else {
            for (int i = elementData.begin; i < elementData.end; i++) {
                if (o.equals(elementData.get(i))) {
                    elementData.end--;
                    elementData.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        preconditionCheck();
        if (o == null) {
            for (int i = elementData.end - 1; i >= elementData.begin; i--) {
                if (elementData.get(i) == null) {
                    elementData.end--;
                    elementData.remove(i);
                    return true;
                }
            }
        } else {
            for (int i = elementData.end - 1; i >= elementData.begin; i--) {
                if (o.equals(elementData.get(i))) {
                    elementData.end--;
                    elementData.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean offer(E e) {
        return offerLast(e);
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public E poll() {
        return pollFirst();
    }

    @Override
    public E element() {
        return getFirst();
    }

    @Override
    public E peek() {
        return peekFirst();
    }

    @Override
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    @Override
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    @Override
    public void clear() {
        preconditionCheck();
        elementData.removeRange(elementData.begin, elementData.end - elementData.begin);
        elementData.end = elementData.begin;
    }

    public E get(int index) {
        preconditionCheck();
        rangeCheck(index);
        return elementData.get(index + elementData.begin);
    }

    @Override
    public E set(int index, E element) {
        preconditionCheck();
        rangeCheck(index);
        E oldElement = elementData.get(index + elementData.begin);
        elementData.set(index + elementData.begin, element);
        return oldElement;
    }

    @Override
    public void add(int index, E element) {
        preconditionCheck();
        rangeCheckForAdd(index);
        elementData.end++;
        elementData.insert(index + elementData.begin, element);
    }

    @Override
    public E remove(int index) {
        preconditionCheck();
        rangeCheck(index);
        E oldElement = elementData.get(index + elementData.begin);
        elementData.end--;
        elementData.remove(index + elementData.begin);
        return oldElement;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        preconditionCheck();
        elementData.setRange(elementData.end, c.toArray(), 0, c.size());
        elementData.end += c.size();
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        preconditionCheck();
        rangeCheckForAdd(index);
        elementData.insertRange(index + elementData.begin, c.toArray(), 0, c.size());
        elementData.end += c.size();
        return true;
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        preconditionCheck();
        for (int i = elementData.begin; i < elementData.end; i++) {
            action.accept(elementData.get(i));
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        preconditionCheck();
        boolean removed = false;
        for (int i = elementData.begin; i < elementData.end; i++) {
            if (filter.test(elementData.get(i))) {
                elementData.remove(i);
                elementData.end--;
                removed = true;
            }
        }
        return removed;
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        preconditionCheck();
        for (int i = elementData.begin; i < elementData.end; i++) {
            elementData.set(i, elementData.get(i));
        }
    }

    @NotNull
    @Override
    public Object[] toArray() {
        preconditionCheck();
        return elementData.toArray(elementData.begin, elementData.end - elementData.begin);
    }

    @SuppressWarnings({"unchecked"})
    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        Objects.requireNonNull(a);
        preconditionCheck();
        if (a.length < elementData.end - elementData.begin) {
            return (T[]) toArray();
        }
        Object[] tmpArray = elementData.toArray(elementData.begin, elementData.end - elementData.begin);
        UtArrayMock.arraycopy(tmpArray, 0, a, 0, elementData.end - elementData.begin);
        if (a.length > elementData.end - elementData.begin) {
            a[elementData.end - elementData.begin] = null;
        }
        return a;
    }

    /**
     * Auxiliary method, that should be only executed concretely
     * @return new LinkedList with all the elements from this.
     */
    private List<E> toList() {
        return new LinkedList<>(this);
    }

    // TODO add symbolic subList
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        preconditionCheck();
        executeConcretely();
        return toList().subList(fromIndex, toIndex);
    }

    @Override
    public Iterator<E> iterator() {
        preconditionCheck();
        return new UtLinkedListIterator(elementData.begin);
    }


    @Override
    public ListIterator<E> listIterator() {
        preconditionCheck();
        return new UtLinkedListIterator(elementData.begin);
    }

    @NotNull
    @Override
    public Iterator<E> descendingIterator() {
        preconditionCheck();
        return new ReverseIteratorWrapper(elementData.end);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<E> stream() {
        preconditionCheck();

        int size = elementData.end;
        Object[] data = elementData.toArray(0, size);

        return new UtStream<>((E[]) data, size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<E> parallelStream() {
        preconditionCheck();

        int size = elementData.end;
        Object[] data = elementData.toArray(0, size);

        return new UtStream<>((E[]) data, size);
    }

    public class ReverseIteratorWrapper implements ListIterator<E> {

        int index;
        int prevIndex = -1;

        ReverseIteratorWrapper(int index) {
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            preconditionCheck();
            return index != elementData.begin;
        }

        @Override
        public E next() {
            preconditionCheck();
            if (index == elementData.begin) {
                throw new NoSuchElementException();
            }
            prevIndex = index - 1;
            return elementData.get(--index);
        }

        @Override
        public boolean hasPrevious() {
            preconditionCheck();
            return index != elementData.end;
        }

        @Override
        public E previous() {
            preconditionCheck();
            if (index == 0) {
                throw new NoSuchElementException();
            }
            prevIndex = index;
            return elementData.get(index++);
        }

        @Override
        public int nextIndex() {
            preconditionCheck();
            return index - 1;
        }

        @Override
        public int previousIndex() {
            preconditionCheck();
            return index;
        }

        @Override
        public void remove() {
            preconditionCheck();
            if (prevIndex == elementData.begin - 1) {
                throw new IllegalStateException();
            }
            elementData.end--;
            elementData.remove(prevIndex);
            prevIndex = elementData.begin - 1;
        }

        @Override
        public void set(E e) {
            preconditionCheck();
            if (prevIndex == elementData.begin - 1) {
                throw new IllegalStateException();
            }
            elementData.set(prevIndex, e);
        }

        @Override
        public void add(E e) {
            preconditionCheck();
            elementData.end++;
            elementData.insert(index++, e);
            prevIndex = elementData.begin - 1;
        }
    }

    public class UtLinkedListIterator implements ListIterator<E> {
        int index;
        int prevIndex = -1;

        UtLinkedListIterator(int index) {
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            preconditionCheck();
            return index != elementData.end;
        }

        @Override
        public E next() {
            preconditionCheck();
            if (index == elementData.end) {
                throw new NoSuchElementException();
            }
            prevIndex = index;
            return elementData.get(index++);
        }

        @Override
        public boolean hasPrevious() {
            preconditionCheck();
            return index != elementData.begin;
        }

        @Override
        public E previous() {
            preconditionCheck();
            if (index == 0) {
                throw new NoSuchElementException();
            }
            prevIndex = index - 1;
            return elementData.get(--index);
        }

        @Override
        public int nextIndex() {
            preconditionCheck();
            return index;
        }

        @Override
        public int previousIndex() {
            preconditionCheck();
            return index - 1;
        }

        @Override
        public void remove() {
            preconditionCheck();
            if (prevIndex == elementData.begin - 1) {
                throw new IllegalStateException();
            }
            elementData.end--;
            elementData.remove(prevIndex);
            prevIndex = elementData.begin - 1;
        }

        @Override
        public void set(E e) {
            preconditionCheck();
            if (prevIndex == elementData.begin - 1) {
                throw new IllegalStateException();
            }
            elementData.set(prevIndex, e);
        }

        @Override
        public void add(E e) {
            preconditionCheck();
            elementData.end++;
            elementData.insert(index++, e);
            prevIndex = elementData.begin - 1;
        }
    }
}
