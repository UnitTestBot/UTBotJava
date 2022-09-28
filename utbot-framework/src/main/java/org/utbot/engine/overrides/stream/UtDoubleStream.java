package org.utbot.engine.overrides.stream;

import org.jetbrains.annotations.NotNull;
import org.utbot.engine.overrides.collections.RangeModifiableUnlimitedArray;
import org.utbot.engine.overrides.collections.UtGenericStorage;

import java.util.DoubleSummaryStatistics;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.api.mock.UtMock.assumeOrExecuteConcretely;
import static org.utbot.engine.ResolverKt.HARD_MAX_ARRAY_SIZE;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

public class UtDoubleStream implements DoubleStream, UtGenericStorage<Double> {
    private final RangeModifiableUnlimitedArray<Double> elementData;

    private final RangeModifiableUnlimitedArray<Runnable> closeHandlers = new RangeModifiableUnlimitedArray<>();

    private boolean isParallel = false;

    /**
     * {@code false} by default, assigned to {@code true} after performing any operation on this stream. Any operation,
     * performed on a closed UtStream, throws the {@link IllegalStateException}.
     */
    private boolean isClosed = false;

    public UtDoubleStream() {
        visit(this);
        elementData = new RangeModifiableUnlimitedArray<>();
    }

    public UtDoubleStream(Double[] data, int length) {
        visit(this);
        elementData = new RangeModifiableUnlimitedArray<>();
        elementData.setRange(0, data, 0, length);
        elementData.end = length;
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

    public DoubleStream filter(DoublePredicate predicate) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Double[] filtered = new Double[size];
        int j = 0;
        for (int i = 0; i < size; i++) {
            double element = elementData.get(i);
            if (predicate.test(element)) {
                filtered[j++] = element;
            }
        }

        return new UtDoubleStream(filtered, j);
    }

    @Override
    public DoubleStream map(DoubleUnaryOperator mapper) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Double[] mapped = new Double[size];
        for (int i = 0; i < size; i++) {
            mapped[i] = mapper.applyAsDouble(elementData.get(i));
        }

        return new UtDoubleStream(mapped, size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Object[] mapped = new Object[size];
        for (int i = 0; i < size; i++) {
            mapped[i] = mapper.apply(elementData.get(i));
        }

        return new UtStream<>((U[]) mapped, size);
    }

    @Override
    public IntStream mapToInt(DoubleToIntFunction mapper) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Integer[] mapped = new Integer[size];
        for (int i = 0; i < size; i++) {
            mapped[i] = mapper.applyAsInt(elementData.get(i));
        }

        return new UtIntStream(mapped, size);
    }

    @Override
    public LongStream mapToLong(DoubleToLongFunction mapper) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Long[] mapped = new Long[size];
        for (int i = 0; i < size; i++) {
            mapped[i] = mapper.applyAsLong(elementData.get(i));
        }

        return new UtLongStream(mapped, size);
    }

    @Override
    public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        preconditionCheckWithClosingStream();
        // as mapper can produce infinite streams, we cannot process it symbolically
        executeConcretely();
        return null;
    }

    @Override
    public DoubleStream distinct() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        Double[] distinctElements = new Double[size];
        int distinctSize = 0;
        for (int i = 0; i < size; i++) {
            double element = elementData.get(i);
            boolean isDuplicate = false;

            for (int j = 0; j < distinctSize; j++) {
                double alreadyProcessedElement = distinctElements[j];
                if (element == alreadyProcessedElement) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                distinctElements[distinctSize++] = element;
            }
        }

        return new UtDoubleStream(distinctElements, distinctSize);
    }

    // TODO choose the best sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    @Override
    public DoubleStream sorted() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;

        if (size == 0) {
            return new UtDoubleStream();
        }

        Double[] sortedElements = new Double[size];
        for (int i = 0; i < size; i++) {
            sortedElements[i] = elementData.get(i);
        }

        // bubble sort
        for (int i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - i - 1; j++) {
                if (sortedElements[j] > sortedElements[j + 1]) {
                    Double tmp = sortedElements[j];
                    sortedElements[j] = sortedElements[j + 1];
                    sortedElements[j + 1] = tmp;
                }
            }
        }

        return new UtDoubleStream(sortedElements, size);
    }

    @Override
    public DoubleStream peek(DoubleConsumer action) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        for (int i = 0; i < size; i++) {
            action.accept(elementData.get(i));
        }

        // returned stream should be opened, so we "reopen" this stream to return it
        isClosed = false;

        return this;
    }

    @Override
    public DoubleStream limit(long maxSize) {
        preconditionCheckWithClosingStream();

        if (maxSize < 0) {
            throw new IllegalArgumentException();
        }

        if (maxSize == 0) {
            return new UtDoubleStream();
        }

        assumeOrExecuteConcretely(maxSize <= Integer.MAX_VALUE);

        int newSize = (int) maxSize;
        int curSize = elementData.end;

        if (newSize > curSize) {
            newSize = curSize;
        }

        Double[] elements = new Double[newSize];
        for (int i = 0; i < newSize; i++) {
            elements[i] = elementData.get(i);
        }

        return new UtDoubleStream(elements, newSize);
    }

    @Override
    public DoubleStream skip(long n) {
        preconditionCheckWithClosingStream();

        if (n < 0) {
            throw new IllegalArgumentException();
        }

        int curSize = elementData.end;
        if (n > curSize) {
            return new UtDoubleStream();
        }

        // n is 1...Integer.MAX_VALUE here
        int newSize = (int) (curSize - n);

        if (newSize == 0) {
            return new UtDoubleStream();
        }

        Double[] elements = new Double[newSize];
        for (int i = (int) n; i < newSize; i++) {
            elements[i] = elementData.get(i);
        }

        return new UtDoubleStream(elements, newSize);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void forEach(DoubleConsumer action) {
        peek(action);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void forEachOrdered(DoubleConsumer action) {
        peek(action);
    }

    @Override
    public double[] toArray() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        double[] result = new double[size];
        for (int i = 0; i < size; i++) {
            result[i] = elementData.get(i);
        }

        return result;
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        double result = identity;
        for (int i = 0; i < size; i++) {
            result = op.applyAsDouble(result, elementData.get(i));
        }

        return result;
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        if (size == 0) {
            return OptionalDouble.empty();
        }

        Double result = null;
        for (int i = 0; i < size; i++) {
            double element = elementData.get(i);
            if (result == null) {
                result = element;
            } else {
                result = op.applyAsDouble(result, element);
            }
        }

        return result == null ? OptionalDouble.empty() : OptionalDouble.of(result);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
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
    public double sum() {
        preconditionCheckWithClosingStream();

        final int size = elementData.end;

        if (size == 0) {
            return 0;
        }

        double sum = 0;

        for (int i = 0; i < size; i++) {
            double element = elementData.get(i);
            sum += element;
        }

        return sum;
    }

    @SuppressWarnings("ManualMinMaxCalculation")
    @Override
    public OptionalDouble min() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        if (size == 0) {
            return OptionalDouble.empty();
        }

        double min = elementData.get(0);
        for (int i = 1; i < size; i++) {
            final double element = elementData.get(i);
            min = (element < min) ? element : min;
        }

        return OptionalDouble.of(min);
    }

    @SuppressWarnings("ManualMinMaxCalculation")
    @Override
    public OptionalDouble max() {
        preconditionCheckWithClosingStream();

        int size = elementData.end;
        if (size == 0) {
            return OptionalDouble.empty();
        }

        double max = elementData.get(0);
        for (int i = 1; i < size; i++) {
            final double element = elementData.get(i);
            max = (element > max) ? element : max;
        }

        return OptionalDouble.of(max);
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

        double average = sum() / count();

        return OptionalDouble.of(average);
    }

    @Override
    public DoubleSummaryStatistics summaryStatistics() {
        preconditionCheckWithClosingStream();

        DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();

        int size = elementData.end;
        for (int i = 0; i < size; i++) {
            double element = elementData.get(i);
            statistics.accept(element);
        }

        return statistics;
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
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
    public boolean allMatch(DoublePredicate predicate) {
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
    public boolean noneMatch(DoublePredicate predicate) {
        preconditionCheckWithClosingStream();

        return !anyMatch(predicate);
    }

    @Override
    public OptionalDouble findFirst() {
        preconditionCheckWithClosingStream();

        if (elementData.end == 0) {
            return OptionalDouble.empty();
        }

        double first = elementData.get(0);

        return OptionalDouble.of(first);
    }

    @Override
    public OptionalDouble findAny() {
        preconditionCheckWithClosingStream();

        // since this implementation is always sequential, we can just return the first element
        return findFirst();
    }

    @Override
    public Stream<Double> boxed() {
        preconditionCheckWithClosingStream();

        final int size = elementData.end;
        if (size == 0) {
            return new UtStream<>();
        }

        Double[] elements = new Double[size];
        for (int i = 0; i < size; i++) {
            elements[i] = elementData.get(i);
        }

        return new UtStream<>(elements, size);
    }

    @Override
    public DoubleStream sequential() {
        // this method does not "close" this stream
        preconditionCheck();

        isParallel = false;

        return this;
    }

    @Override
    public DoubleStream parallel() {
        // this method does not "close" this stream
        preconditionCheck();

        isParallel = true;

        return this;
    }

    @Override
    public PrimitiveIterator.OfDouble iterator() {
        preconditionCheckWithClosingStream();

        return new UtDoubleStreamIterator(0);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Spliterator.OfDouble spliterator() {
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
    public DoubleStream unordered() {
        // this method does not "close" this stream
        preconditionCheck();

        return this;
    }

    @NotNull
    @Override
    public DoubleStream onClose(Runnable closeHandler) {
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

    public class UtDoubleStreamIterator implements PrimitiveIterator.OfDouble {
        int index;

        UtDoubleStreamIterator(int index) {
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
        public double nextDouble() {
            return next();
        }

        @Override
        public Double next() {
            preconditionCheck();

            if (index == elementData.end) {
                throw new NoSuchElementException();
            }

            return elementData.get(index++);
        }
    }
}
