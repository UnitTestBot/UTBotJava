package org.utbot.engine.overrides.stream;

import org.jetbrains.annotations.NotNull;
import org.utbot.engine.overrides.collections.RangeModifiableUnlimitedArray;
import org.utbot.engine.overrides.collections.UtArrayList;
import org.utbot.engine.overrides.collections.UtGenericStorage;
import org.utbot.engine.overrides.stream.actions.*;
import org.utbot.engine.overrides.stream.actions.primitives.doubles.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.api.mock.UtMock.assumeOrExecuteConcretely;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

public class UtDoubleStream implements DoubleStream, UtGenericStorage<Double> {
    /**
     * A reference to the original collection. The default collection is {@link UtArrayList}.
     */
    @SuppressWarnings("rawtypes")
    final Collection origin;

    final RangeModifiableUnlimitedArray<StreamAction> actions;

    final RangeModifiableUnlimitedArray<Runnable> closeHandlers;

    boolean isParallel = false;

    /**
     * {@code false} by default, assigned to {@code true} after performing any operation on this stream. Any operation,
     * performed on a closed UtStream, throws the {@link IllegalStateException}.
     */
    private boolean isClosed = false;

    /**
     * Stores original array if this stream was created using
     * {@link java.util.Arrays#stream)} or {@code of} or @code empty}
     * method invocations (not from collection or another stream), and {@code null} otherwise.
     * <p>
     * Used only during resolving for creating stream assemble model.
     */
    @SuppressWarnings("unused")
    Object[] originArray;

    /**
     * {@code true} if this stream was created from primitive array, and false otherwise.
     */
    boolean isCreatedFromPrimitiveArray;

    public UtDoubleStream() {
        this(new Double[]{}, 0, 0);
    }

    @SuppressWarnings("unused")
    public UtDoubleStream(Double[] data) {
        this(data, 0, data.length);
    }

    public UtDoubleStream(Double[] data, int length) {
        this(data, 0, length);
    }

    public UtDoubleStream(Double[] data, int startInclusive, int endExclusive) {
        this(new UtArrayList<>(data, startInclusive, endExclusive));

        originArray = data;
        isCreatedFromPrimitiveArray = true;
    }

    public UtDoubleStream(UtDoubleStream other) {
        visit(this);

        origin = other.origin;
        actions = other.actions;
        isParallel = other.isParallel;
        closeHandlers = other.closeHandlers;

        // new stream should be opened
        isClosed = false;

        originArray = other.originArray;
        isCreatedFromPrimitiveArray = other.isCreatedFromPrimitiveArray;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public UtDoubleStream(UtStream other) {
        visit(this);

        origin = other.origin;
        actions = other.actions;
        isParallel = other.isParallel;
        closeHandlers = other.closeHandlers;

        // new stream should be opened
        isClosed = false;

        originArray = other.originArray;
        isCreatedFromPrimitiveArray = other.isCreatedFromPrimitiveArray;
    }

    public UtDoubleStream(UtIntStream other) {
        visit(this);

        origin = other.origin;
        actions = other.actions;
        isParallel = other.isParallel;
        closeHandlers = other.closeHandlers;

        // new stream should be opened
        isClosed = false;

        originArray = other.originArray;
        isCreatedFromPrimitiveArray = other.isCreatedFromPrimitiveArray;
    }

    public UtDoubleStream(UtLongStream other) {
        visit(this);

        origin = other.origin;
        actions = other.actions;
        isParallel = other.isParallel;
        closeHandlers = other.closeHandlers;

        // new stream should be opened
        isClosed = false;

        originArray = other.originArray;
        isCreatedFromPrimitiveArray = other.isCreatedFromPrimitiveArray;
    }

    @SuppressWarnings("rawtypes")
    private UtDoubleStream(Collection collection) {
        visit(this);

        actions = new RangeModifiableUnlimitedArray<>();
        closeHandlers = new RangeModifiableUnlimitedArray<>();

        origin = collection;

        originArray = null;
        isCreatedFromPrimitiveArray = false;
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

        assume(origin != null);

        parameter(origin);

        visit(this);
    }

    private void preconditionCheckWithoutClosing() {
        preconditionCheck();

        if (isClosed) {
            throw new IllegalStateException();
        }
    }

    private void preconditionCheckWithClosingStream() {
        preconditionCheckWithoutClosing();

        // Even if exception occurs in the body of a stream operation, this stream could not be used later.
        isClosed = true;
    }

    public DoubleStream filter(DoublePredicate predicate) {
        preconditionCheckWithClosingStream();

        final DoubleFilterAction filterAction = new DoubleFilterAction(predicate);
        actions.insert(actions.end++, filterAction);

        return new UtDoubleStream(this);
    }

    @Override
    public DoubleStream map(DoubleUnaryOperator mapper) {
        preconditionCheckWithClosingStream();

        final DoubleMapAction mapAction = new DoubleMapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtDoubleStream(this);
    }

    @Override
    public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        preconditionCheckWithClosingStream();

        final DoubleToObjMapAction mapAction = new DoubleToObjMapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtStream<>(this);
    }

    @Override
    public IntStream mapToInt(DoubleToIntFunction mapper) {
        preconditionCheckWithClosingStream();

        final DoubleToIntMapAction mapAction = new DoubleToIntMapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtIntStream(this);
    }

    @Override
    public LongStream mapToLong(DoubleToLongFunction mapper) {
        preconditionCheckWithClosingStream();

        final DoubleToLongMapAction mapAction = new DoubleToLongMapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtLongStream(this);
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

        final DistinctAction distinctAction = new DistinctAction();
        actions.insert(actions.end++, distinctAction);

        return new UtDoubleStream(this);
    }

    // TODO choose the best sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    @Override
    public DoubleStream sorted() {
        preconditionCheckWithClosingStream();

        final NaturalSortingAction naturalSortingAction = new NaturalSortingAction();
        actions.insert(actions.end++, naturalSortingAction);

        return new UtDoubleStream(this);
    }

    @Override
    public DoubleStream peek(DoubleConsumer action) {
        preconditionCheckWithoutClosing();

        final DoubleConsumerAction consumerAction = new DoubleConsumerAction(action);
        actions.insert(actions.end++, consumerAction);

        return new UtDoubleStream(this);
    }

    @Override
    public DoubleStream limit(long maxSize) {
        preconditionCheckWithClosingStream();

        if (maxSize < 0) {
            throw new IllegalArgumentException();
        }

        assumeOrExecuteConcretely(maxSize <= Integer.MAX_VALUE);

        final LimitAction limitAction = new LimitAction((int) maxSize);
        actions.set(actions.end++, limitAction);

        return new UtDoubleStream(this);
    }

    @Override
    public DoubleStream skip(long n) {
        preconditionCheckWithClosingStream();

        if (n < 0) {
            throw new IllegalArgumentException();
        }

        assumeOrExecuteConcretely(n <= Integer.MAX_VALUE);

        final SkipAction skipAction = new SkipAction((int) n);
        actions.insert(actions.end++, skipAction);

        return new UtDoubleStream(this);
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

        final Object[] objects = applyActions(origin.toArray());

        final double[] result = new double[objects.length];
        int i = 0;
        for (Object object : objects) {
            result[i++] = (Double) object;
        }

        return result;
    }

    @NotNull
    private Object[] applyActions(Object[] originArray) {
        int actionsNumber = actions.end;

        for (int i = 0; i < actionsNumber; i++) {
            originArray = actions.get(i).applyAction(originArray);
        }

        return originArray;
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
        double result = identity;

        for (double element : toArray()) {
            result = op.applyAsDouble(result, element);
        }

        return result;
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
        double[] finalElements = toArray();

        int size = finalElements.length;
        if (size == 0) {
            return OptionalDouble.empty();
        }

        Double result = null;

        for (double element : finalElements) {
            if (result == null) {
                result = element;
            } else {
                result = op.applyAsDouble(result, element);
            }
        }

        return OptionalDouble.of(result);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        double[] finalElements = toArray();

        // since this implementation is always sequential, we do not need to use the combiner
        R result = supplier.get();

        for (double element : finalElements) {
            accumulator.accept(result, element);
        }

        return result;
    }

    @Override
    public double sum() {
        double[] finalElements = toArray();

        long sum = 0;
        for (double element : finalElements) {
            sum += element;
        }

        return sum;
    }

    @Override
    public OptionalDouble min() {
        double[] finalElements = toArray();

        final int length = finalElements.length;
        if (length == 0) {
            return OptionalDouble.empty();
        }

        double min = finalElements[0];
        for (double element : finalElements) {
            if (element < min) {
                min = element;
            }
        }

        return OptionalDouble.of(min);
    }

    @Override
    public OptionalDouble max() {
        double[] finalElements = toArray();

        final int length = finalElements.length;
        if (length == 0) {
            return OptionalDouble.empty();
        }

        double max = finalElements[0];
        for (double element : finalElements) {
            if (element > max) {
                max = element;
            }
        }

        return OptionalDouble.of(max);
    }

    @Override
    public long count() {
        double[] finalElements = toArray();

        return finalElements.length;
    }

    @Override
    public OptionalDouble average() {
        double[] finalElements = toArray();

        final int length = finalElements.length;
        if (length == 0) {
            return OptionalDouble.empty();
        }

        long sum = 0;
        for (double element : finalElements) {
            sum += element;
        }

        final double average = ((double) sum) / length;

        return OptionalDouble.of(average);
    }

    @Override
    public DoubleSummaryStatistics summaryStatistics() {
        double[] finalElements = toArray();

        DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();

        for (double element : finalElements) {
            statistics.accept(element);
        }

        return statistics;
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
        double[] finalElements = toArray();

        for (double element : finalElements) {
            if (predicate.test(element)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean allMatch(DoublePredicate predicate) {
        double[] finalElements = toArray();

        for (double element : finalElements) {
            if (!predicate.test(element)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean noneMatch(DoublePredicate predicate) {
        return !anyMatch(predicate);
    }

    @Override
    public OptionalDouble findFirst() {
        double[] finalElements = toArray();

        if (finalElements.length == 0) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(finalElements[0]);
    }

    @Override
    public OptionalDouble findAny() {
        // since this implementation is always sequential, we can just return the first element
        return findFirst();
    }

    @Override
    public Stream<Double> boxed() {
        preconditionCheckWithClosingStream();

        return new UtStream<>(this);
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
        double[] finalElements = toArray();

        return new UtDoubleStreamIterator(finalElements);
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

    public static class UtDoubleStreamIterator implements PrimitiveIterator.OfDouble {
        private final double[] data;

        private final int lastIndex;

        int index;

        public UtDoubleStreamIterator(double[] data) {
            this.data = data;
            lastIndex = data.length - 1;
        }

        @Override
        public boolean hasNext() {
            return index <= lastIndex;
        }

        @Override
        public double nextDouble() {
            return data[index++];
        }

        @Override
        public Double next() {
            return data[index++];
        }
    }
}
