package org.utbot.engine.overrides.collections;

import org.utbot.engine.overrides.UtArrayMock;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.utbot.engine.overrides.stream.UtStream;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.api.mock.UtMock.assumeOrExecuteConcretely;
import static org.utbot.engine.ResolverKt.MAX_LIST_SIZE;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;


/**
 * Class represents hybrid implementation (java + engine instructions) of List interface for {@link org.utbot.engine.Traverser}.
 * <p>
 * Implementation is based on org.utbot.engine.overrides.collections.RangeModifiableArray.
 * Should behave similar to {@link java.util.ArrayList}.
 */
public class UtArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable, UtGenericStorage<E> {
    private final RangeModifiableUnlimitedArray<E> elementData;
    private static final long serialVersionUID = 8683452581122892189L;

    public UtArrayList(int initialCapacity) {
        // mark this as visited to not traverse preconditionCheck next time
        visit(this);
        if (initialCapacity >= 0) {
            elementData = new RangeModifiableUnlimitedArray<>();
        } else {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
    }

    @SuppressWarnings({"unused"})
    public UtArrayList() {
        // mark this as visited to not traverse preconditionCheck next time
        visit(this);
        elementData = new RangeModifiableUnlimitedArray<>();
    }

    @SuppressWarnings({"unused"})
    public UtArrayList(Collection<? extends E> c) {
        this(0);
        // mark this as visited to not traverse preconditionCheck next time
        visit(this);
        addAll(c);
    }

    public UtArrayList(E[] data) {
        visit(this);
        int length = data.length;
        elementData = new RangeModifiableUnlimitedArray<>();
        elementData.setRange(0, data, 0, length);
        elementData.end = length;
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
    void preconditionCheck() {
        if (alreadyVisited(this)) {
            return;
        }
        setEqualGenericType(elementData);

        assume(elementData != null);
        assume(elementData.storage != null);

        parameter(elementData);
        parameter(elementData.storage);

        int size = elementData.end;
        assume(elementData.begin == 0);
        assume(size >= 0);
        assumeOrExecuteConcretely(size <= MAX_LIST_SIZE);

        visit(this);
    }

    private void rangeCheckForAdd(int index) {
        if (index < 0 || index > elementData.end) {
            throw new IndexOutOfBoundsException();
        }
    }

    private void rangeCheck(int index) {
        if (index < 0 || index >= elementData.end) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int size() {
        preconditionCheck();
        return elementData.end;
    }

    @Override
    public boolean isEmpty() {
        preconditionCheck();
        return elementData.end == 0;
    }

    @Override
    public boolean contains(Object o) {
        preconditionCheck();
        if (o == null) {
            for (int i = 0; i < elementData.end; i++) {
                if (elementData.get(i) == null) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < elementData.end; i++) {
                if (o.equals(elementData.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    @Override
    public Object[] toArray() {
        preconditionCheck();
        return elementData.toArray(0, elementData.end);
    }

    @SuppressWarnings({"unchecked"})
    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        Objects.requireNonNull(a);
        preconditionCheck();
        if (a.length < elementData.end) {
            return (T[]) toArray();
        }
        Object[] tmpArray = elementData.toArray(0, elementData.end);
        UtArrayMock.arraycopy(tmpArray, 0, a, 0, elementData.end);
        if (a.length > elementData.end) {
            a[elementData.end] = null;
        }
        return a;
    }

    @Override
    public boolean add(E e) {
        preconditionCheck();
        elementData.set(elementData.end++, e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        preconditionCheck();
        if (o == null) {
            for (int i = 0; i < elementData.end; i++) {
                if (elementData.get(i) == null) {
                    elementData.end--;
                    elementData.remove(i);
                    return true;
                }
            }
        } else {
            for (int i = 0; i < elementData.end; i++) {
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
    public boolean addAll(@NotNull Collection<? extends E> c) {
        preconditionCheck();
        elementData.setRange(elementData.end += c.size(), c.toArray(), 0, c.size());
        return true;
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        preconditionCheck();
        rangeCheckForAdd(index);
        elementData.insertRange(index, c.toArray(), 0, c.size());
        elementData.end += c.size();
        return true;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        preconditionCheck();
        boolean changed = false;
        for (Object o : c) {
            if (remove(o)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        preconditionCheck();
        boolean changed = false;
        for (int i = 0; i < elementData.end; i++) {
            if (!c.contains(elementData.get(i))) {
                elementData.end--;
                elementData.remove(i);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        preconditionCheck();
        elementData.end = 0;
    }

    @Override
    public E get(int index) {
        preconditionCheck();
        rangeCheck(index);
        return elementData.get(index);
    }

    @Override
    public E set(int index, E element) {
        preconditionCheck();
        rangeCheck(index);
        E oldElement = elementData.get(index);
        elementData.set(index, element);
        return oldElement;
    }

    @Override
    public void add(int index, E element) {
        preconditionCheck();
        rangeCheckForAdd(index);
        elementData.end++;
        elementData.insert(index, element);
    }

    @Override
    public E remove(int index) {
        preconditionCheck();
        rangeCheck(index);
        E element = elementData.get(index);
        elementData.end--;
        elementData.remove(index);
        return element;
    }

    @Override
    public int indexOf(Object o) {
        preconditionCheck();
        if (o == null) {
            for (int i = 0; i < elementData.end; i++) {
                if (elementData.get(i) == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < elementData.end; i++) {
                if (o.equals(elementData.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        preconditionCheck();
        if (o == null) {
            for (int i = elementData.end - 1; i >= 0; i--) {
                if (elementData.get(i) == null) {
                    return i;
                }
            }
        } else {
            for (int i = elementData.end - 1; i >= 0; i--) {
                if (o.equals(elementData.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }


    @NotNull
    @Override
    public Iterator<E> iterator() {
        preconditionCheck();
        return new UtArrayListIterator(0);
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        preconditionCheck();
        return new UtArrayListIterator(0);
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator(int index) {
        preconditionCheck();
        return new UtArrayListIterator(index);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        preconditionCheck();
        for (int i = 0; i < elementData.end; i++) {
            action.accept(elementData.get(i));
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        preconditionCheck();
        boolean removed = false;
        for (int i = 0; i < elementData.end; i++) {
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
        for (int i = 0; i < elementData.end; i++) {
            elementData.set(i, operator.apply(elementData.get(i)));
        }
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

    /**
     * Auxiliary method, that should be only executed concretely
     * @return new ArrayList with all the elements from this.
     */
    private List<E> toList() {
        return new ArrayList<>(this);
    }

    // TODO: ADD symbolic subList
    @NotNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        preconditionCheck();

        executeConcretely();
        return this.toList().subList(fromIndex, toIndex);
    }

    public class UtArrayListIterator implements ListIterator<E> {
        int index;
        int prevIndex = -1;

        UtArrayListIterator(int index) {
            rangeCheckForAdd(index);
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
            return index != 0;
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
            if (prevIndex == -1) {
                throw new IllegalStateException();
            }
            elementData.end--;
            elementData.remove(prevIndex);
            prevIndex = -1;
        }

        @Override
        public void set(E e) {
            preconditionCheck();
            if (prevIndex == -1) {
                throw new IllegalStateException();
            }
            elementData.set(prevIndex, e);
        }

        @Override
        public void add(E e) {
            preconditionCheck();
            elementData.end++;
            elementData.insert(index++, e);
            prevIndex = -1;
        }
    }
}