package org.utbot.engine.overrides.collections;

import org.utbot.engine.overrides.UtArrayMock;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.utbot.engine.overrides.stream.UtStream;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.doesntThrow;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

/**
 * Class represents hybrid implementation (java + engine instructions) of Set interface for {@link org.utbot.engine.Traverser}.
 * <p>
 * Implementation is based on RangedModifiableArray, and all operations are linear.
 * Should behave similar to
 *
 * @see java.util.HashSet
 * @see org.utbot.engine.SetWrapper
 *
 * NOTE: permits null elements in the same way as {@link java.util.HashSet}.
 */
public class UtHashSet<E> extends AbstractSet<E> implements UtGenericStorage<E> {
    RangeModifiableUnlimitedArray<E> elementData;

    @SuppressWarnings({"unused"})
    UtHashSet() {
        visit(this);
        elementData = new RangeModifiableUnlimitedArray<>();
        elementData.begin = 0;
        elementData.end = 0;
    }

    @SuppressWarnings({"unused"})
    UtHashSet(Collection<E> c) {
        visit(this);
        elementData = new RangeModifiableUnlimitedArray<>();
        elementData.begin = 0;
        elementData.end = c.size();
        elementData.setRange(0, c.toArray(), 0, c.size());
    }



    /**
     * Assume preconditions for elements in this Set.
     *
     * <li> array.size in 0..3. </li>
     * <li> All elements in set are distinct. </li>
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void preconditionCheck() {
        if (alreadyVisited(this)) {
            setEqualGenericType(elementData);
            return;
        }
        // assume that size is always less or equal to 3
        setEqualGenericType(elementData);
        assume(elementData != null);
        assume(elementData.storage != null);
        assume(elementData.begin == 0);
        assume(elementData.end >= 0 & elementData.end <= 3);

        parameter(elementData);
        parameter(elementData.storage);
        doesntThrow();

        // check that all elements are distinct.
        for (int i = elementData.begin; i < elementData.end; i++) {
            E element = elementData.get(i);
            parameter(element);
            // make element address non-positive

            // if key is not null, check its hashCode for exception
            if (element != null) {
                element.hashCode();
            }

            // check that there are no duplicate values
            // we can start with a next value, as all previous values are already processed
            for (int j = i + 1; j < elementData.end; j++) {
                // we use Objects.equals to process null case too
                assume(!Objects.equals(element, elementData.get(j)));
            }
        }

        visit(this);
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
        if (a.length > elementData.end)
            a[elementData.end] = null;
        return a;
    }

    @Override
    public boolean add(E e) {
        preconditionCheck();
        if (contains(e)) {
            return false;
        } else {
            elementData.set(elementData.end++, e);
            return true;
        }
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
        Objects.requireNonNull(c);
        preconditionCheck();
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        Objects.requireNonNull(c);
        preconditionCheck();
        boolean modified = false;
        for (int i = 0; i < elementData.end; i++) {
            if (!c.contains(elementData.get(i))) {
                elementData.remove(i);
                elementData.end--;
                modified = true;
            }
        }
        return modified;
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
    public boolean removeAll(@NotNull Collection<?> c) {
        Objects.requireNonNull(c);
        preconditionCheck();
        boolean modified = false;
        for (Object o : c) {
            if (remove(o)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        preconditionCheck();
        elementData.end = 0;
        elementData.removeRange(0, elementData.end);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        preconditionCheck();
        return new UtHashSetIterator();
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

    public class UtHashSetIterator implements Iterator<E> {
        int index = 0;

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
            return elementData.get(index++);
        }

        @Override
        public void remove() {
            preconditionCheck();
            if (index == 0) {
                throw new IllegalStateException();
            }
            elementData.end--;
            elementData.remove(index - 1);
        }
    }
}
