package org.utbot.engine.overrides.stream;

import org.jetbrains.annotations.NotNull;
import org.utbot.engine.overrides.collections.RangeModifiableUnlimitedArray;
import org.utbot.engine.overrides.collections.UtGenericStorage;
import org.utbot.framework.plugin.api.visible.UtStreamConsumingException;

import java.util.IntSummaryStatistics;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.api.mock.UtMock.assumeOrExecuteConcretely;
import static org.utbot.engine.ResolverKt.getMaxStreamSize;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

public class UtIntStream implements IntStream, UtGenericStorage<Integer> {
    private final RangeModifiableUnlimitedArray<Integer> elementData;

    private final RangeModifiableUnlimitedArray<Runnable> closeHandlers = new RangeModifiableUnlimitedArray<>();

    private boolean isParallel = false;

    /**
     * {@code false} by default, assigned to {@code true} after performing any operation on this stream. Any operation,
     * performed on a closed UtStream, throws the {@link IllegalStateException}.
     */
    private boolean isClosed = false;

    public UtIntStream() {
        visit(this);
        elementData = new RangeModifiableUnlimitedArray<>();
    }

    public UtIntStream(Integer[] data, int length) {
        this(data, 0, length);
    }

    public UtIntStream(Integer[] data, int startInclusive, int endExclusive) {
        visit(this);

        int size = endExclusive - startInclusive;

        elementData = new RangeModifiableUnlimitedArray<>();
        elementData.setRange(0, data, startInclusive, size);
        elementData.end = endExclusive;
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
        setGenericTypeToTypeOfValue(elementData, 0);

        assume(elementData != null);
        assume(elementData.storage != null);

        parameter(elementData);
        parameter(elementData.storage);

        assume(elementData.begin == 0);

        assume(elementData.end >= 0);
        // we can create a stream for an array using Stream.of
        assumeOrExecuteConcretely(elementData.end <= getMaxStreamSize());

        // As real primitive streams contain primitives, we cannot accept nulls.
        for (int i = 0; i < elementData.end; i++) {
            assume(elementData.get(i) != null);
        }

        // Do not assume that firstly used stream may be already closed to prevent garbage branches
        isClosed = false;

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

    public IntStream filter(IntPredicate predicate) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Integer[] filtered = new Integer[size];
        int j = 0;
        for (int i = 0; i < size; i++) {
            int element = elementData.get(i);

            try {
                if (predicate.test(element)) {
                    filtered[j++] = element;
                }
            } catch (Exception e) {
                throw new UtStreamConsumingException(e);
            }
        }

        return new UtIntStream(filtered, j);
    }

    @Override
    public IntStream map(IntUnaryOperator mapper) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Integer[] mapped = new Integer[size];
        for (int i = 0; i < size; i++) {
            try {
                mapped[i] = mapper.applyAsInt(elementData.get(i));
            } catch (Exception e) {
                throw new UtStreamConsumingException(e);
            }
        }

        return new UtIntStream(mapped, size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
        // Here we assume that this mapping does not produce infinite streams
        // - otherwise it should always be executed concretely.
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        U[] mapped = (U[]) new Object[size];
        for (int i = 0; i < size; i++) {
            try {
                mapped[i] = mapper.apply(elementData.get(i));
            } catch (Exception e) {
                throw new UtStreamConsumingException(e);
            }
        }

        return new UtStream<>(mapped, size);
    }

    @Override
    public LongStream mapToLong(IntToLongFunction mapper) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Long[] mapped = new Long[size];
        for (int i = 0; i < size; i++) {
            try {
                mapped[i] = mapper.applyAsLong(elementData.get(i));
            } catch (Exception e) {
                throw new UtStreamConsumingException(e);
            }
        }

        return new UtLongStream(mapped, size);
    }

    @Override
    public DoubleStream mapToDouble(IntToDoubleFunction mapper) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Double[] mapped = new Double[size];
        for (int i = 0; i < size; i++) {
            try {
                mapped[i] = mapper.applyAsDouble(elementData.get(i));
            } catch (Exception e) {
                throw new UtStreamConsumingException(e);
            }
        }

        return new UtDoubleStream(mapped, size);
    }

    @Override
    public IntStream flatMap(IntFunction<? extends IntStream> mapper) {
        preconditionCheckWithClosingStream();
        // as mapper can produce infinite streams, we cannot process it symbolically
        executeConcretely();
        return null;
    }

    @Override
    public IntStream distinct() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Integer[] distinctElements = new Integer[size];
        int distinctSize = 0;
        for (int i = 0; i < size; i++) {
            int element = elementData.get(i);
            boolean isDuplicate = false;

            for (int j = 0; j < distinctSize; j++) {
                int alreadyProcessedElement = distinctElements[j];
                if (element == alreadyProcessedElement) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                distinctElements[distinctSize++] = element;
            }
        }

        return new UtIntStream(distinctElements, distinctSize);
    }

    // TODO choose the best sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    @Override
    public IntStream sorted() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;

        if (size == 0) {
            return new UtIntStream();
        }

        Integer[] sortedElements = new Integer[size];
        for (int i = 0; i < size; i++) {
            sortedElements[i] = elementData.get(i);
        }

        // bubble sort
        for (int i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - i - 1; j++) {
                if (sortedElements[j] > sortedElements[j + 1]) {
                    Integer tmp = sortedElements[j];
                    sortedElements[j] = sortedElements[j + 1];
                    sortedElements[j + 1] = tmp;
                }
            }
        }

        return new UtIntStream(sortedElements, size);
    }

    @Override
    public IntStream peek(IntConsumer action) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        for (int i = 0; i < size; i++) {
            try {
                action.accept(elementData.get(i));
            } catch (Exception e) {
                throw new UtStreamConsumingException(e);
            }
        }

        // returned stream should be opened, so we "reopen" this stream to return it
        isClosed = false;

        return this;
    }

    @Override
    public IntStream limit(long maxSize) {
        preconditionCheckWithClosingStream();

        if (maxSize < 0) {
            throw new IllegalArgumentException();
        }

        if (maxSize == 0) {
            return new UtIntStream();
        }

        assumeOrExecuteConcretely(maxSize <= Integer.MAX_VALUE);

        int newSize = (int) maxSize;
        int curSize = elementData.end;

        if (newSize > curSize) {
            newSize = curSize;
        }

        Integer[] newData = elementData.toCastedArray(0, newSize);

        return new UtIntStream(newData, newSize);
    }

    @Override
    public IntStream skip(long n) {
        preconditionCheckWithClosingStream();

        if (n < 0) {
            throw new IllegalArgumentException();
        }

        int curSize = elementData.end;
        if (n >= curSize) {
            return new UtIntStream();
        }

        // n is 1...(Integer.MAX_VALUE - 1) here
        int newSize = (int) (curSize - n);

        Integer[] newData = elementData.toCastedArray((int) n, newSize);

        return new UtIntStream(newData, newSize);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void forEach(IntConsumer action) {
        try {
            peek(action);
        } catch (UtStreamConsumingException e) {
            // Since this is a terminal operation, we should throw an original exception
        }
    }

    @Override
    public void forEachOrdered(IntConsumer action) {
        forEach(action);
    }

    @Override
    public int[] toArray() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        int[] result = new int[size];

        for (int i = 0; i < size; i++) {
            result[i] = elementData.get(i);
        }

        return result;
    }

    @Override
    public int reduce(int identity, IntBinaryOperator op) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        int result = identity;
        for (int i = 0; i < size; i++) {
            result = op.applyAsInt(result, elementData.get(i));
        }

        return result;
    }

    @Override
    public OptionalInt reduce(IntBinaryOperator op) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        if (size == 0) {
            return OptionalInt.empty();
        }

        int result = elementData.get(0);
        for (int i = 1; i < size; i++) {
            int element = elementData.get(i);
            result = op.applyAsInt(result, element);
        }

        return OptionalInt.of(result);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
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
    public int sum() {
        preconditionCheckWithClosingStream();

        final int size = elementData.end;

        if (size == 0) {
            return 0;
        }

        int sum = 0;

        for (int i = 0; i < size; i++) {
            int element = elementData.get(i);
            sum += element;
        }

        return sum;
    }

    @SuppressWarnings("ManualMinMaxCalculation")
    @Override
    public OptionalInt min() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        if (size == 0) {
            return OptionalInt.empty();
        }

        int min = elementData.get(0);
        for (int i = 1; i < size; i++) {
            final int element = elementData.get(i);
            min = (element < min) ? element : min;
        }

        return OptionalInt.of(min);
    }

    @SuppressWarnings("ManualMinMaxCalculation")
    @Override
    public OptionalInt max() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        if (size == 0) {
            return OptionalInt.empty();
        }

        int max = elementData.get(0);
        for (int i = 1; i < size; i++) {
            final int element = elementData.get(i);
            max = (element > max) ? element : max;
        }

        return OptionalInt.of(max);
    }

    @Override
    public long count() {
        preconditionCheckWithClosingStream();

        return elementData.end;
    }

    @Override
    public OptionalDouble average() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        if (size == 0) {
            return OptionalDouble.empty();
        }

        // "reopen" this stream to use sum and count
        isClosed = false;
        final double sum = sum();
        isClosed = false;
        final long count = count();

        double average = sum / count;

        return OptionalDouble.of(average);
    }

    @Override
    public IntSummaryStatistics summaryStatistics() {
        preconditionCheckWithClosingStream();

        IntSummaryStatistics statistics = new IntSummaryStatistics();

        int size = elementData.end;
        for (int i = 0; i < size; i++) {
            int element = elementData.get(i);
            statistics.accept(element);
        }

        return statistics;
    }

    @Override
    public boolean anyMatch(IntPredicate predicate) {
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
    public boolean allMatch(IntPredicate predicate) {
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
    public boolean noneMatch(IntPredicate predicate) {
        return !anyMatch(predicate);
    }

    @Override
    public OptionalInt findFirst() {
        preconditionCheckWithClosingStream();

        if (elementData.end == 0) {
            return OptionalInt.empty();
        }

        int first = elementData.get(0);

        return OptionalInt.of(first);
    }

    @Override
    public OptionalInt findAny() {
        preconditionCheckWithClosingStream();

        // since this implementation is always sequential, we can just return the first element
        return findFirst();
    }

    @Override
    public LongStream asLongStream() {
        preconditionCheckWithClosingStream();

        final int size = elementData.end;

        if (size == 0) {
            return new UtLongStream();
        }

        // "open" stream to use toArray method
        final int[] elements = copyData();

        Long[] longs = new Long[size];

        for (int i = 0; i < size; i++) {
            longs[i] = (long) elements[i];
        }

        return new UtLongStream(longs, size);
    }

    @Override
    public DoubleStream asDoubleStream() {
        preconditionCheckWithClosingStream();

        final int size = elementData.end;

        if (size == 0) {
            return new UtDoubleStream();
        }

        final int[] elements = copyData();

        Double[] doubles = new Double[size];

        for (int i = 0; i < size; i++) {
            doubles[i] = (double) elements[i];
        }

        return new UtDoubleStream(doubles, size);
    }

    @Override
    public Stream<Integer> boxed() {
        preconditionCheckWithClosingStream();

        final int size = elementData.end;
        if (size == 0) {
            return new UtStream<>();
        }

        Integer[] elements = new Integer[size];
        for (int i = 0; i < size; i++) {
            elements[i] = elementData.get(i);
        }

        return new UtStream<>(elements, size);
    }

    @Override
    public IntStream sequential() {
        // this method does not "close" this stream
        preconditionCheck();

        isParallel = false;

        return this;
    }

    @Override
    public IntStream parallel() {
        // this method does not "close" this stream
        preconditionCheck();

        isParallel = true;

        return this;
    }

    @Override
    public PrimitiveIterator.OfInt iterator() {
        preconditionCheckWithClosingStream();

        return new UtIntStreamIterator(0);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Spliterator.OfInt spliterator() {
        preconditionCheckWithClosingStream();

        // each implementation is extremely difficult and almost impossible to analyze
        executeConcretely();
        return null;
    }

    @Override
    public boolean isParallel() {
        // this method does not "close" this stream
        preconditionCheck();

        return isParallel;
    }

    @NotNull
    @Override
    public IntStream unordered() {
        // this method does not "close" this stream
        preconditionCheck();

        return this;
    }

    @NotNull
    @Override
    public IntStream onClose(Runnable closeHandler) {
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

    // Copies data to int array. Might be used on already "closed" stream. Marks this stream as closed.
    private int[] copyData() {
        // "open" stream to use toArray method
        isClosed = false;

        return toArray();
    }

    public class UtIntStreamIterator implements PrimitiveIterator.OfInt {
        int index;

        UtIntStreamIterator(int index) {
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
        public int nextInt() {
            return next();
        }

        @Override
        public Integer next() {
            preconditionCheck();

            if (index == elementData.end) {
                throw new NoSuchElementException();
            }

            return elementData.get(index++);
        }
    }
}
