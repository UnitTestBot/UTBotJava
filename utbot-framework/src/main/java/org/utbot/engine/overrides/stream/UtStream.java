package org.utbot.engine.overrides.stream;

import org.utbot.engine.overrides.UtArrayMock;
import org.utbot.engine.overrides.collections.RangeModifiableUnlimitedArray;
import org.utbot.engine.overrides.collections.UtGenericStorage;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.api.mock.UtMock.assumeOrExecuteConcretely;
import static org.utbot.engine.ResolverKt.HARD_MAX_ARRAY_SIZE;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

public class UtStream<E> implements Stream<E>, UtGenericStorage<E> {
    private final RangeModifiableUnlimitedArray<E> elementData;

    private final RangeModifiableUnlimitedArray<Runnable> closeHandlers = new RangeModifiableUnlimitedArray<>();

    private boolean isParallel = false;

    /**
     * {@code false} by default, assigned to {@code true} after performing any operation on this stream. Any operation,
     * performed on a closed UtStream, throws the {@link IllegalStateException}.
     */
    private boolean isClosed = false;

    public UtStream() {
        visit(this);
        elementData = new RangeModifiableUnlimitedArray<>();
    }

    public UtStream(E[] data, int length) {
        visit(this);
        elementData = new RangeModifiableUnlimitedArray<>();
        elementData.setRange(0, data, 0, length);
        elementData.end = length;
    }

    public UtStream(E[] data, int startInclusive, int endExclusive) {
        visit(this);

        int size = endExclusive - startInclusive;

        elementData = new RangeModifiableUnlimitedArray<>();
        elementData.setRange(0, data, startInclusive, size);
        elementData.end = size;
    }

    /**
     * Precondition check is called only once by object,
     * if it was passed as parameter to method under test.
     * <p>
     * Preconditions that are must be satisfied:
     * <li> elementData.size in 0..HARD_MAX_ARRAY_SIZE. </li>
     * <li> elementData is marked as parameter </li>
     * <li> elementData.storage and it's elements are marked as parameters </li>
     */
    @SuppressWarnings("DuplicatedCode")
    void preconditionCheck() {
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
        // we can create a stream for an array using Stream.of
        assume(elementData.end <= HARD_MAX_ARRAY_SIZE);

        visit(this);
    }

    private void preconditionCheckWithClosingStream() {
        preconditionCheck();

        if (isClosed) {
            throw new IllegalStateException();
        }

        // Even if exception occurs in the body of a stream operation, this stream could not be used later.
        isClosed = true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<E> filter(Predicate<? super E> predicate) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Object[] filtered = new Object[size];
        int j = 0;
        for (int i = 0; i < size; i++) {
            E element = elementData.get(i);
            if (predicate.test(element)) {
                filtered[j++] = element;
            }
        }

        return new UtStream<>((E[]) filtered, j);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Stream<R> map(Function<? super E, ? extends R> mapper) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Object[] mapped = new Object[size];
        for (int i = 0; i < size; i++) {
            mapped[i] = mapper.apply(elementData.get(i));
        }

        return new UtStream<>((R[]) mapped, size);
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super E> mapper) {
        preconditionCheckWithClosingStream();
        // TODO https://github.com/UnitTestBot/UTBotJava/issues/146
        executeConcretely();
        return null;
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super E> mapper) {
        preconditionCheckWithClosingStream();
        // TODO https://github.com/UnitTestBot/UTBotJava/issues/146
        executeConcretely();
        return null;
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super E> mapper) {
        preconditionCheckWithClosingStream();
        // TODO https://github.com/UnitTestBot/UTBotJava/issues/146
        executeConcretely();
        return null;
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super E, ? extends Stream<? extends R>> mapper) {
        preconditionCheckWithClosingStream();
        // as mapper can produce infinite streams, we cannot process it symbolically
        executeConcretely();
        return null;
    }

    @Override
    public IntStream flatMapToInt(Function<? super E, ? extends IntStream> mapper) {
        preconditionCheckWithClosingStream();
        // TODO https://github.com/UnitTestBot/UTBotJava/issues/146
        executeConcretely();
        return null;
    }

    @Override
    public LongStream flatMapToLong(Function<? super E, ? extends LongStream> mapper) {
        preconditionCheckWithClosingStream();
        // TODO https://github.com/UnitTestBot/UTBotJava/issues/146
        executeConcretely();
        return null;
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super E, ? extends DoubleStream> mapper) {
        preconditionCheckWithClosingStream();
        // TODO https://github.com/UnitTestBot/UTBotJava/issues/146
        executeConcretely();
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<E> distinct() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Object[] distinctElements = new Object[size];
        int distinctSize = 0;
        for (int i = 0; i < size; i++) {
            E element = elementData.get(i);
            boolean isDuplicate = false;

            if (element == null) {
                for (int j = 0; j < distinctSize; j++) {
                    Object alreadyProcessedElement = distinctElements[j];
                    if (alreadyProcessedElement == null) {
                        isDuplicate = true;
                        break;
                    }
                }
            } else {
                for (int j = 0; j < distinctSize; j++) {
                    Object alreadyProcessedElement = distinctElements[j];
                    if (element.equals(alreadyProcessedElement)) {
                        isDuplicate = true;
                        break;
                    }
                }
            }

            if (!isDuplicate) {
                distinctElements[distinctSize++] = element;
            }
        }

        return new UtStream<>((E[]) distinctElements, distinctSize);
    }

    // TODO choose the best sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    @SuppressWarnings("unchecked")
    @Override
    public Stream<E> sorted() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;

        if (size == 0) {
            return new UtStream<>();
        }

        Object[] sortedElements = UtArrayMock.copyOf(elementData.toArray(0, size), size);

        // bubble sort
        for (int i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - i - 1; j++) {
                if (((Comparable<E>) sortedElements[j]).compareTo((E) sortedElements[j + 1]) > 0) {
                    Object tmp = sortedElements[j];
                    sortedElements[j] = sortedElements[j + 1];
                    sortedElements[j + 1] = tmp;
                }
            }
        }

        return new UtStream<>((E[]) sortedElements, size);
    }

    // TODO choose the best sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    @SuppressWarnings("unchecked")
    @Override
    public Stream<E> sorted(Comparator<? super E> comparator) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;

        if (size == 0) {
            return new UtStream<>();
        }

        Object[] sortedElements = UtArrayMock.copyOf(elementData.toArray(0, size), size);

        // bubble sort
        for (int i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - i - 1; j++) {
                if (comparator.compare((E) sortedElements[j], (E) sortedElements[j + 1]) > 0) {
                    Object tmp = sortedElements[j];
                    sortedElements[j] = sortedElements[j + 1];
                    sortedElements[j + 1] = tmp;
                }
            }
        }

        return new UtStream<>((E[]) sortedElements, size);
    }

    @Override
    public Stream<E> peek(Consumer<? super E> action) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        for (int i = 0; i < size; i++) {
            action.accept(elementData.get(i));
        }

        // returned stream should be opened, so we "reopen" this stream to return it
        isClosed = false;

        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<E> limit(long maxSize) {
        preconditionCheckWithClosingStream();

        if (maxSize < 0) {
            throw new IllegalArgumentException();
        }

        assumeOrExecuteConcretely(maxSize <= Integer.MAX_VALUE);

        int newSize = (int) maxSize;
        int curSize = elementData.end;

        if (newSize == curSize) {
            return this;
        }

        if (newSize > curSize) {
            newSize = curSize;
        }

        return new UtStream<>((E[]) elementData.toArray(0, newSize), newSize);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<E> skip(long n) {
        preconditionCheckWithClosingStream();

        if (n < 0) {
            throw new IllegalArgumentException();
        }

        if (n == 0) {
            return this;
        }

        int curSize = elementData.end;
        if (n > curSize) {
            return new UtStream<>();
        }

        // n is 1...Integer.MAX_VALUE here
        int newSize = (int) (curSize - n);

        return new UtStream<>((E[]) elementData.toArray((int) n, newSize), newSize);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        peek(action);
    }

    @Override
    public void forEachOrdered(Consumer<? super E> action) {
        peek(action);
    }

    @NotNull
    @Override
    public Object[] toArray() {
        preconditionCheckWithClosingStream();

        return elementData.toArray(0, elementData.end);
    }

    @NotNull
    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        preconditionCheckWithClosingStream();

        // TODO untracked ArrayStoreException - JIRA:1089
        int size = elementData.end;
        A[] array = generator.apply(size);

        UtArrayMock.arraycopy(elementData.toArray(0, size), 0, array, 0, size);

        return array;
    }

    @Override
    public E reduce(E identity, BinaryOperator<E> accumulator) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        E result = identity;
        for (int i = 0; i < size; i++) {
            result = accumulator.apply(result, elementData.get(i));
        }

        return result;
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    @Override
    public Optional<E> reduce(BinaryOperator<E> accumulator) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        if (size == 0) {
            return Optional.empty();
        }

        E result = null;
        for (int i = 0; i < size; i++) {
            E element = elementData.get(i);
            if (result == null) {
                result = element;
            } else {
                result = accumulator.apply(result, element);
            }
        }

        return Optional.of(result);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super E, U> accumulator, BinaryOperator<U> combiner) {
        preconditionCheckWithClosingStream();

        // since this implementation is always sequential, we do not need to use the combiner
        int size = elementData.end;
        U result = identity;
        for (int i = 0; i < size; i++) {
            result = accumulator.apply(result, elementData.get(i));
        }

        return result;
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super E> accumulator, BiConsumer<R, R> combiner) {
        preconditionCheckWithClosingStream();

        // since this implementation is always sequential, we do not need to use the combiner
        int size = elementData.end;
        R result = supplier.get();
        for (int i = 0; i < size; i++) {
            accumulator.accept(result, elementData.get(i));
        }

        return result;
    }

    @Override
    public <R, A> R collect(Collector<? super E, A, R> collector) {
        preconditionCheckWithClosingStream();

        // since this implementation is always sequential, we do not need to use the combiner
        int size = elementData.end;
        A result = collector.supplier().get();
        for (int i = 0; i < size; i++) {
            collector.accumulator().accept(result, elementData.get(i));
        }

        return collector.finisher().apply(result);
    }

    @NotNull
    @Override
    public Optional<E> min(Comparator<? super E> comparator) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        if (size == 0) {
            return Optional.empty();
        }

        E min = elementData.get(0);
        for (int i = 1; i < size; i++) {
            E element = elementData.get(i);
            if (comparator.compare(min, element) > 0) {
                min = element;
            }
        }

        return Optional.of(min);
    }

    @NotNull
    @Override
    public Optional<E> max(Comparator<? super E> comparator) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        if (size == 0) {
            return Optional.empty();
        }

        E max = elementData.get(0);
        for (int i = 1; i < size; i++) {
            E element = elementData.get(i);
            if (comparator.compare(max, element) < 0) {
                max = element;
            }
        }

        return Optional.of(max);
    }

    @Override
    public long count() {
        preconditionCheckWithClosingStream();

        return elementData.end;
    }

    @Override
    public boolean anyMatch(Predicate<? super E> predicate) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        for (int i = 0; i < size; i++) {
            if (predicate.test(elementData.get(i))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean allMatch(Predicate<? super E> predicate) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        for (int i = 0; i < size; i++) {
            if (!predicate.test(elementData.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean noneMatch(Predicate<? super E> predicate) {
        preconditionCheckWithClosingStream();

        return !anyMatch(predicate);
    }

    @NotNull
    @Override
    public Optional<E> findFirst() {
        preconditionCheckWithClosingStream();

        if (elementData.end == 0) {
            return Optional.empty();
        }

        E first = elementData.get(0);

        return Optional.of(first);
    }

    @NotNull
    @Override
    public Optional<E> findAny() {
        preconditionCheckWithClosingStream();

        // since this implementation is always sequential, we can just return the first element
        return findFirst();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        preconditionCheckWithClosingStream();

        return new UtStreamIterator(0);
    }

    @NotNull
    @Override
    public Spliterator<E> spliterator() {
        preconditionCheckWithClosingStream();
        // implementation from AbstractList
        return Spliterators.spliterator(elementData.toArray(0, elementData.end), Spliterator.ORDERED);
    }

    @Override
    public boolean isParallel() {
        // this method does not "close" this stream
        preconditionCheck();

        return isParallel;
    }

    @NotNull
    @Override
    public Stream<E> sequential() {
        // this method does not "close" this stream
        preconditionCheck();

        isParallel = false;

        return this;
    }

    @NotNull
    @Override
    public Stream<E> parallel() {
        // this method does not "close" this stream
        preconditionCheck();

        isParallel = true;

        return this;
    }

    @NotNull
    @Override
    public Stream<E> unordered() {
        // this method does not "close" this stream
        preconditionCheck();

        return this;
    }

    @NotNull
    @Override
    public Stream<E> onClose(Runnable closeHandler) {
        // this method does not "close" this stream
        preconditionCheck();

        // adds closeHandler to existing
        closeHandlers.set(closeHandlers.end++, closeHandler);

        return this;
    }

    @Override
    public void close() {
        // Stream can be closed via this method many times
        preconditionCheck();

        // TODO resources closing https://github.com/UnitTestBot/UTBotJava/issues/189

        // NOTE: this implementation does not care about suppressing and throwing exceptions produced by handlers
        for (int i = 0; i < closeHandlers.end; i++) {
            closeHandlers.get(i).run();
        }
    }

    public class UtStreamIterator implements Iterator<E> {
        int index;

        UtStreamIterator(int index) {
            if (index < 0 || index > elementData.end) {
                throw new IndexOutOfBoundsException();
            }

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

            return elementData.get(index++);
        }
    }
}
