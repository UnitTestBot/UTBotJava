package org.utbot.engine.overrides.stream;

import org.utbot.engine.overrides.UtArrayMock;
import org.utbot.engine.overrides.collections.RangeModifiableUnlimitedArray;
import org.utbot.engine.overrides.collections.UtArrayList;
import org.utbot.engine.overrides.collections.UtGenericStorage;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
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
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

public class UtStream<E> implements Stream<E>, UtGenericStorage<E> {
    /**
     * The default collection is {@link UtArrayList}.
     */
    private final Collection<E> elementData;

    private final RangeModifiableUnlimitedArray<Runnable> closeHandlers = new RangeModifiableUnlimitedArray<>();

    private boolean isParallel = false;

    /**
     * {@code false} by default, assigned to {@code true} after performing any operation on this stream. Any operation,
     * performed on a closed UtStream, throws the {@link IllegalStateException}.
     */
    private boolean isClosed = false;

    public UtStream() {
        visit(this);

        elementData = new UtArrayList<>();
    }

    public UtStream(Collection<E> collection) {
        visit(this);

        elementData = collection;
    }

    public UtStream(E[] data) {
        this(data, 0, data.length);
    }

    public UtStream(E[] data, int length) {
        this(data, 0, length);
    }

    public UtStream(E[] data, int startInclusive, int endExclusive) {
        visit(this);

        elementData = new UtArrayList<>(data, startInclusive, endExclusive);
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
        // TODO ?
//        setEqualGenericType(elementData);

        assume(elementData != null);

        parameter(elementData);

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

        int size = elementData.size();
        Object[] filtered = new Object[size];
        int i = 0;

        for (E element : elementData) {
            if (predicate.test(element)) {
                filtered[i++] = element;
            }
        }

        return new UtStream<>((E[]) filtered, i);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Stream<R> map(Function<? super E, ? extends R> mapper) {
        preconditionCheckWithClosingStream();

        int size = elementData.size();
        Object[] mapped = new Object[size];
        int i = 0;

        for (E element : elementData) {
            mapped[i++] = mapper.apply(element);
        }

        return new UtStream<>((R[]) mapped);
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super E> mapper) {
        preconditionCheckWithClosingStream();

        int size = elementData.size();
        Integer[] data = new Integer[size];
        int i = 0;

        for (E element : elementData) {
            data[i++] = mapper.applyAsInt(element);
        }

        return new UtIntStream(data, size);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super E> mapper) {
        preconditionCheckWithClosingStream();

        int size = elementData.size();
        Long[] data = new Long[size];
        int i = 0;

        for (E element : elementData) {
            data[i++] = mapper.applyAsLong(element);
        }

        return new UtLongStream(data, size);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super E> mapper) {
        preconditionCheckWithClosingStream();

        int size = elementData.size();
        Double[] data = new Double[size];
        int i = 0;

        for (E element : elementData) {
            data[i++] = mapper.applyAsDouble(element);
        }

        return new UtDoubleStream(data, size);
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
        // as mapper can produce infinite streams, we cannot process it symbolically
        executeConcretely();
        return null;
    }

    @Override
    public LongStream flatMapToLong(Function<? super E, ? extends LongStream> mapper) {
        preconditionCheckWithClosingStream();
        // as mapper can produce infinite streams, we cannot process it symbolically
        executeConcretely();
        return null;
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super E, ? extends DoubleStream> mapper) {
        preconditionCheckWithClosingStream();
        // as mapper can produce infinite streams, we cannot process it symbolically
        executeConcretely();
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<E> distinct() {
        preconditionCheckWithClosingStream();

        int size = elementData.size();
        Object[] distinctElements = new Object[size];
        int distinctSize = 0;

        for (E element : elementData) {
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

        int size = elementData.size();

        if (size == 0) {
            return new UtStream<>();
        }

        E[] sortedElements = (E[]) new Object[size];
        int i = 0;

        for (E element : elementData) {
            sortedElements[i++] = element;
        }

        // bubble sort
        for (i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - i - 1; j++) {
                if (((Comparable<E>) sortedElements[j]).compareTo(sortedElements[j + 1]) > 0) {
                    E tmp = sortedElements[j];
                    sortedElements[j] = sortedElements[j + 1];
                    sortedElements[j + 1] = tmp;
                }
            }
        }

        return new UtStream<>(sortedElements);
    }

    // TODO choose the best sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    @SuppressWarnings("unchecked")
    @Override
    public Stream<E> sorted(Comparator<? super E> comparator) {
        preconditionCheckWithClosingStream();

        int size = elementData.size();

        if (size == 0) {
            return new UtStream<>();
        }

        E[] sortedElements = (E[]) new Object[size];
        int i = 0;

        for (E element : elementData) {
            sortedElements[i++] = element;
        }

        // bubble sort
        for (i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - i - 1; j++) {
                if (comparator.compare(sortedElements[j], sortedElements[j + 1]) > 0) {
                    E tmp = sortedElements[j];
                    sortedElements[j] = sortedElements[j + 1];
                    sortedElements[j + 1] = tmp;
                }
            }
        }

        return new UtStream<>(sortedElements);
    }

    @Override
    public Stream<E> peek(Consumer<? super E> action) {
        preconditionCheckWithClosingStream();

        for (E element : elementData) {
            action.accept(element);
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

        if (maxSize == 0) {
            return new UtStream<>();
        }

        assumeOrExecuteConcretely(maxSize <= Integer.MAX_VALUE);

        int newSize = (int) maxSize;
        int curSize = elementData.size();

        if (newSize > curSize) {
            newSize = curSize;
        }

        E[] elements = (E[]) new Object[newSize];
        int i = 0;

        for (E element : elementData) {
            if (i >= newSize) {
                break;
            }

            elements[i++] = element;
        }

        return new UtStream<>(elements);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<E> skip(long n) {
        preconditionCheckWithClosingStream();

        if (n < 0) {
            throw new IllegalArgumentException();
        }

        int curSize = elementData.size();
        if (n > curSize) {
            return new UtStream<>();
        }

        // n is 1...Integer.MAX_VALUE here
        int newSize = (int) (curSize - n);

        if (newSize == 0) {
            return new UtStream<>();
        }

        E[] elements = (E[]) new Object[newSize];
        int i = 0;
        int j = 0;

        for (E element : elementData) {
            if (i++ < n) {
                break;
            }

            elements[j++] = element;
        }

        return new UtStream<>(elements);
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

        return elementData.toArray();
    }

    @NotNull
    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        preconditionCheckWithClosingStream();

        // TODO untracked ArrayStoreException - JIRA:1089
        int size = elementData.size();
        A[] array = generator.apply(size);

        UtArrayMock.arraycopy(elementData.toArray(), 0, array, 0, size);

        return array;
    }

    @Override
    public E reduce(E identity, BinaryOperator<E> accumulator) {
        preconditionCheckWithClosingStream();

        E result = identity;

        for (E element : elementData) {
            result = accumulator.apply(result, element);
        }

        return result;
    }

    @NotNull
    @Override
    public Optional<E> reduce(BinaryOperator<E> accumulator) {
        preconditionCheckWithClosingStream();

        int size = elementData.size();
        if (size == 0) {
            return Optional.empty();
        }

        E result = null;

        for (E element : elementData) {
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
        U result = identity;

        for (E element : elementData) {
            result = accumulator.apply(result, element);
        }

        return result;
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super E> accumulator, BiConsumer<R, R> combiner) {
        preconditionCheckWithClosingStream();

        // since this implementation is always sequential, we do not need to use the combiner
        R result = supplier.get();

        for (E element : elementData) {
            accumulator.accept(result, element);
        }

        return result;
    }

    @Override
    public <R, A> R collect(Collector<? super E, A, R> collector) {
        preconditionCheckWithClosingStream();

        // since this implementation is always sequential, we do not need to use the combiner
        A result = collector.supplier().get();

        for (E element : elementData) {
            collector.accumulator().accept(result, element);
        }

        return collector.finisher().apply(result);
    }

    @NotNull
    @Override
    public Optional<E> min(Comparator<? super E> comparator) {
        preconditionCheckWithClosingStream();

        int size = elementData.size();
        if (size == 0) {
            return Optional.empty();
        }

        final Iterator<E> iterator = elementData.iterator();
        E min = iterator.next();

        while (iterator.hasNext()) {
            E element = iterator.next();
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

        int size = elementData.size();
        if (size == 0) {
            return Optional.empty();
        }

        final Iterator<E> iterator = elementData.iterator();
        E max = iterator.next();

        while (iterator.hasNext()) {
            E element = iterator.next();
            if (comparator.compare(max, element) < 0) {
                max = element;
            }
        }

        return Optional.of(max);
    }

    @Override
    public long count() {
        preconditionCheckWithClosingStream();

        return elementData.size();
    }

    @Override
    public boolean anyMatch(Predicate<? super E> predicate) {
        preconditionCheckWithClosingStream();

        for (E element : elementData) {
            if (predicate.test(element)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean allMatch(Predicate<? super E> predicate) {
        preconditionCheckWithClosingStream();

        for (E element : elementData) {
            if (!predicate.test(element)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean noneMatch(Predicate<? super E> predicate) {
        return !anyMatch(predicate);
    }

    @NotNull
    @Override
    public Optional<E> findFirst() {
        preconditionCheckWithClosingStream();

        if (elementData.size() == 0) {
            return Optional.empty();
        }

        E first = elementData.iterator().next();

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

        return elementData.iterator();
    }

    @NotNull
    @Override
    public Spliterator<E> spliterator() {
        preconditionCheckWithClosingStream();
        // implementation from AbstractList
        return Spliterators.spliterator(elementData.toArray(), Spliterator.ORDERED);
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

        // clear handlers
        closeHandlers.end = 0;
    }
//
//    private final List<Object> actions = new UtArrayList<>();
//
//    private Object apply() {
//        Object[] previousElements = elementData.toArray();
//        int size = previousElements.length;
//
//        for (Object action : actions) {
//            Object[] newElements = new Object[size];
//            int newSize = 0;
//
//            if (action instanceof Predicate) {
//                for (Object element : previousElements) {
//                    if (((Predicate<Object>) action).test(element)) {
//                        newElements[newSize++] = element;
//                    }
//                }
//            } else if (action instanceof Function) {
//                for (Object element : previousElements) {
//                    newElements[newSize++] = ((Function<Object, Object>) action).apply(element);
//                }
//            }
//
//            size = newSize;
//            previousElements = new Object[size];
//            UtArrayMock.arraycopy(newElements, 0, previousElements, 0, size);
//        }
//
//        return previousElements;
//    }
}
