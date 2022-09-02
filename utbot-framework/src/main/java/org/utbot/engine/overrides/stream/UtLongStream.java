package org.utbot.engine.overrides.stream;

import org.jetbrains.annotations.NotNull;
import org.utbot.api.mock.UtMock;
import org.utbot.engine.overrides.collections.RangeModifiableUnlimitedArray;
import org.utbot.engine.overrides.collections.UtArrayList;
import org.utbot.engine.overrides.collections.UtGenericStorage;
import org.utbot.engine.overrides.stream.actions.DistinctAction;
import org.utbot.engine.overrides.stream.actions.LimitAction;
import org.utbot.engine.overrides.stream.actions.NaturalSortingAction;
import org.utbot.engine.overrides.stream.actions.SkipAction;
import org.utbot.engine.overrides.stream.actions.StreamAction;
import org.utbot.engine.overrides.stream.actions.primitives.ints.IntConsumerAction;
import org.utbot.engine.overrides.stream.actions.primitives.ints.IntFilterAction;
import org.utbot.engine.overrides.stream.actions.primitives.ints.IntMapAction;
import org.utbot.engine.overrides.stream.actions.primitives.ints.IntToDoubleMapAction;
import org.utbot.engine.overrides.stream.actions.primitives.ints.IntToLongMapAction;
import org.utbot.engine.overrides.stream.actions.primitives.ints.IntToObjMapAction;
import org.utbot.engine.overrides.stream.actions.primitives.longs.LongConsumerAction;
import org.utbot.engine.overrides.stream.actions.primitives.longs.LongFilterAction;
import org.utbot.engine.overrides.stream.actions.primitives.longs.LongMapAction;
import org.utbot.engine.overrides.stream.actions.primitives.longs.LongToDoubleMapAction;
import org.utbot.engine.overrides.stream.actions.primitives.longs.LongToIntMapAction;
import org.utbot.engine.overrides.stream.actions.primitives.longs.LongToObjMapAction;

import java.util.Collection;
import java.util.IntSummaryStatistics;
import java.util.LongSummaryStatistics;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.OptionalLong;
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
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
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

// TODO we can use method implementations from UtStream after wrappers inheritance support https://github.com/UnitTestBot/UTBotJava/issues/819
public class UtLongStream implements LongStream, UtGenericStorage<Long> {
    /**
     * A reference to the original collection. The default collection is {@link UtArrayList}.
     */
    final Collection origin;

    final RangeModifiableUnlimitedArray<StreamAction> actions;

    final RangeModifiableUnlimitedArray<Runnable> closeHandlers;

    boolean isParallel = false;

    /**
     * {@code false} by default, assigned to {@code true} after performing any operation on this stream. Any operation,
     * performed on a closed UtStream, throws the {@link IllegalStateException}.
     */
    private boolean isClosed = false;

    public UtLongStream(Long[] data) {
        this(data, 0, data.length);
    }

    public UtLongStream(Long[] data, int length) {
        this(data, 0, length);
    }

    public UtLongStream(Long[] data, int startInclusive, int endExclusive) {
        this(new UtArrayList<>(data, startInclusive, endExclusive));
    }

    public UtLongStream(UtLongStream other) {
        visit(this);

        origin = other.origin;
        actions = other.actions;
        isParallel = other.isParallel;
        closeHandlers = other.closeHandlers;

        // new stream should be opened
        isClosed = false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public UtLongStream(UtStream other) {
        visit(this);

        origin = other.origin;
        actions = other.actions;
        isParallel = other.isParallel;
        closeHandlers = other.closeHandlers;

        // new stream should be opened
        isClosed = false;
    }

    public UtLongStream(UtIntStream other) {
        visit(this);

        origin = other.origin;
        actions = other.actions;
        isParallel = other.isParallel;
        closeHandlers = other.closeHandlers;

        // new stream should be opened
        isClosed = false;
    }

    public UtLongStream(UtDoubleStream other) {
        visit(this);

        origin = other.origin;
        actions = other.actions;
        isParallel = other.isParallel;
        closeHandlers = other.closeHandlers;

        // new stream should be opened
        isClosed = false;
    }

    @SuppressWarnings("rawtypes")
    private UtLongStream(Collection collection) {
        visit(this);

        actions = new RangeModifiableUnlimitedArray<>();
        closeHandlers = new RangeModifiableUnlimitedArray<>();

        origin = collection;
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

    public LongStream filter(LongPredicate predicate) {
        preconditionCheckWithClosingStream();

        final LongFilterAction filterAction = new LongFilterAction(predicate);
        actions.insert(actions.end++, filterAction);

        return new UtLongStream(this);
    }

    @Override
    public LongStream map(LongUnaryOperator mapper) {
        preconditionCheckWithClosingStream();

        final LongMapAction mapAction = new LongMapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtLongStream(this);
    }

    @Override
    public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
        preconditionCheckWithClosingStream();

        final LongToObjMapAction mapAction = new LongToObjMapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtStream<>(this);
    }

    @Override
    public IntStream mapToInt(LongToIntFunction mapper) {
        preconditionCheckWithClosingStream();

        final LongToIntMapAction mapAction = new LongToIntMapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtIntStream(this);
    }

    @Override
    public DoubleStream mapToDouble(LongToDoubleFunction mapper) {
        preconditionCheckWithClosingStream();

        final LongToDoubleMapAction mapAction = new LongToDoubleMapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtDoubleStream(this);
    }

    @Override
    public LongStream flatMap(LongFunction<? extends LongStream> mapper) {
        preconditionCheckWithClosingStream();
        // as mapper can produce infinite streams, we cannot process it symbolically
        executeConcretely();
        return null;
    }

    @Override
    public LongStream distinct() {
        preconditionCheckWithClosingStream();

        final DistinctAction distinctAction = new DistinctAction();
        actions.insert(actions.end++, distinctAction);

        return new UtLongStream(this);
    }

    // TODO choose the best sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    @Override
    public LongStream sorted() {
        preconditionCheckWithClosingStream();

        final NaturalSortingAction naturalSortingAction = new NaturalSortingAction();
        actions.insert(actions.end++, naturalSortingAction);

        return new UtLongStream(this);
    }

    @Override
    public LongStream peek(LongConsumer action) {
        preconditionCheckWithoutClosing();

        final LongConsumerAction consumerAction = new LongConsumerAction(action);
        actions.insert(actions.end++, consumerAction);

        return new UtLongStream(this);
    }

    @Override
    public LongStream limit(long maxSize) {
        preconditionCheckWithClosingStream();

        if (maxSize < 0) {
            throw new IllegalArgumentException();
        }

        assumeOrExecuteConcretely(maxSize <= Integer.MAX_VALUE);

        final LimitAction limitAction = new LimitAction((int) maxSize);
        actions.set(actions.end++, limitAction);

        return new UtLongStream(this);
    }

    @Override
    public LongStream skip(long n) {
        preconditionCheckWithClosingStream();

        if (n < 0) {
            throw new IllegalArgumentException();
        }

        assumeOrExecuteConcretely(n <= Integer.MAX_VALUE);

        final SkipAction skipAction = new SkipAction((int) n);
        actions.insert(actions.end++, skipAction);

        return new UtLongStream(this);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void forEach(LongConsumer action) {
        peek(action);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void forEachOrdered(LongConsumer action) {
        peek(action);
    }

    @Override
    public long[] toArray() {
        preconditionCheckWithClosingStream();

        final Object[] objects = applyActions(origin.toArray());

        final long[] result = new long[objects.length];
        int i = 0;
        for (Object object : objects) {
            result[i++] = (Long) object;
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
    public long reduce(long identity, LongBinaryOperator op) {
        long result = identity;

        for (long element : toArray()) {
            result = op.applyAsLong(result, element);
        }

        return result;
    }

    @Override
    public OptionalLong reduce(LongBinaryOperator op) {
        long[] finalElements = toArray();

        int size = finalElements.length;
        if (size == 0) {
            return OptionalLong.empty();
        }

        Long result = null;

        for (long element : finalElements) {
            if (result == null) {
                result = element;
            } else {
                result = op.applyAsLong(result, element);
            }
        }

        return OptionalLong.of(result);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        long[] finalElements = toArray();

        // since this implementation is always sequential, we do not need to use the combiner
        R result = supplier.get();

        for (long element : finalElements) {
            accumulator.accept(result, element);
        }

        return result;
    }

    @Override
    public long sum() {
        long[] finalElements = toArray();

        long sum = 0;
        for (long element : finalElements) {
            sum += element;
        }

        return sum;
    }

    @Override
    public OptionalLong min() {
        long[] finalElements = toArray();

        final int length = finalElements.length;
        if (length == 0) {
            return OptionalLong.empty();
        }

        long min = finalElements[0];
        for (long element : finalElements) {
            if (element < min) {
                min = element;
            }
        }

        return OptionalLong.of(min);
    }

    @Override
    public OptionalLong max() {
        long[] finalElements = toArray();

        final int length = finalElements.length;
        if (length == 0) {
            return OptionalLong.empty();
        }

        long max = finalElements[0];
        for (long element : finalElements) {
            if (element > max) {
                max = element;
            }
        }

        return OptionalLong.of(max);
    }

    @Override
    public long count() {
        long[] finalElements = toArray();

        return finalElements.length;
    }

    @Override
    public OptionalDouble average() {
        long[] finalElements = toArray();

        final int length = finalElements.length;
        if (length == 0) {
            return OptionalDouble.empty();
        }

        long sum = 0;
        for (long element : finalElements) {
            sum += element;
        }

        final double average = ((double) sum) / length;

        return OptionalDouble.of(average);
    }

    @Override
    public LongSummaryStatistics summaryStatistics() {
        long[] finalElements = toArray();

        LongSummaryStatistics statistics = new LongSummaryStatistics();

        for (long element : finalElements) {
            statistics.accept(element);
        }

        return statistics;
    }

    @Override
    public boolean anyMatch(LongPredicate predicate) {
        long[] finalElements = toArray();

        for (long element : finalElements) {
            if (predicate.test(element)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean allMatch(LongPredicate predicate) {
        long[] finalElements = toArray();

        for (long element : finalElements) {
            if (!predicate.test(element)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean noneMatch(LongPredicate predicate) {
        return !anyMatch(predicate);
    }

    @Override
    public OptionalLong findFirst() {
        long[] finalElements = toArray();

        if (finalElements.length == 0) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(finalElements[0]);
    }

    @Override
    public OptionalLong findAny() {
        // since this implementation is always sequential, we can just return the first element
        return findFirst();
    }

    @Override
    public DoubleStream asDoubleStream() {
        preconditionCheckWithClosingStream();

        return new UtDoubleStream(this);
    }

    @Override
    public Stream<Long> boxed() {
        preconditionCheckWithClosingStream();

        return new UtStream<>(this);
    }

    @Override
    public LongStream sequential() {
        // this method does not "close" this stream
        preconditionCheck();

        isParallel = false;

        return this;
    }

    @Override
    public LongStream parallel() {
        // this method does not "close" this stream
        preconditionCheck();

        isParallel = true;

        return this;
    }

    @Override
    public PrimitiveIterator.OfLong iterator() {
        long[] finalElements = toArray();

        return new UtLongStreamIterator(finalElements);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Spliterator.OfLong spliterator() {
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
    public LongStream unordered() {
        // this method does not "close" this stream
        preconditionCheck();

        return this;
    }

    @NotNull
    @Override
    public LongStream onClose(Runnable closeHandler) {
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

    public static class UtLongStreamIterator implements PrimitiveIterator.OfLong {
        private final long[] data;

        private final int lastIndex;

        int index;

        public UtLongStreamIterator(long[] data) {
            this.data = data;
            lastIndex = data.length - 1;
        }

        @Override
        public boolean hasNext() {
            return index <= lastIndex;
        }

        @Override
        public long nextLong() {
            return data[index++];
        }

        @Override
        public Long next() {
            return data[index++];
        }
    }
}
