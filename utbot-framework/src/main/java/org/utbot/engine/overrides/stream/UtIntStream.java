package org.utbot.engine.overrides.stream;

import org.jetbrains.annotations.NotNull;
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

import java.util.Collection;
import java.util.IntSummaryStatistics;
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
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

// TODO we can use method implementations from UtStream after wrappers inheritance support https://github.com/UnitTestBot/UTBotJava/issues/819
public class UtIntStream implements IntStream, UtGenericStorage<Integer> {
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

    public UtIntStream() {
        this(new Integer[]{}, 0, 0);
    }

    @SuppressWarnings("unused")
    public UtIntStream(Integer[] data) {
        this(data, 0, data.length);
    }

    public UtIntStream(Integer[] data, int length) {
        this(data, 0, length);
    }

    public UtIntStream(Integer[] data, int startInclusive, int endExclusive) {
        this(new UtArrayList<>(data, startInclusive, endExclusive));
    }

    public UtIntStream(UtIntStream other) {
        visit(this);

        origin = other.origin;
        actions = other.actions;
        isParallel = other.isParallel;
        closeHandlers = other.closeHandlers;

        // new stream should be opened
        isClosed = false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public UtIntStream(UtStream other) {
        visit(this);

        origin = other.origin;
        actions = other.actions;
        isParallel = other.isParallel;
        closeHandlers = other.closeHandlers;

        // new stream should be opened
        isClosed = false;
    }

    public UtIntStream(UtLongStream other) {
        visit(this);

        origin = other.origin;
        actions = other.actions;
        isParallel = other.isParallel;
        closeHandlers = other.closeHandlers;

        // new stream should be opened
        isClosed = false;
    }

    public UtIntStream(UtDoubleStream other) {
        visit(this);

        origin = other.origin;
        actions = other.actions;
        isParallel = other.isParallel;
        closeHandlers = other.closeHandlers;

        // new stream should be opened
        isClosed = false;
    }

    @SuppressWarnings("rawtypes")
    private UtIntStream(Collection collection) {
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

    public IntStream filter(IntPredicate predicate) {
        preconditionCheckWithClosingStream();

        final IntFilterAction filterAction = new IntFilterAction(predicate);
        actions.insert(actions.end++, filterAction);

        return new UtIntStream(this);
    }

    @Override
    public IntStream map(IntUnaryOperator mapper) {
        preconditionCheckWithClosingStream();

        final IntMapAction mapAction = new IntMapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtIntStream(this);
    }

    @Override
    public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
        preconditionCheckWithClosingStream();

        final IntToObjMapAction mapAction = new IntToObjMapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtStream<>(this);
    }

    @Override
    public LongStream mapToLong(IntToLongFunction mapper) {
        preconditionCheckWithClosingStream();

        final IntToLongMapAction mapAction = new IntToLongMapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtLongStream(this);
    }

    @Override
    public DoubleStream mapToDouble(IntToDoubleFunction mapper) {
        preconditionCheckWithClosingStream();

        final IntToDoubleMapAction mapAction = new IntToDoubleMapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtDoubleStream(this);
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

        final DistinctAction distinctAction = new DistinctAction();
        actions.insert(actions.end++, distinctAction);

        return new UtIntStream(this);
    }

    // TODO choose the best sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    @Override
    public IntStream sorted() {
        preconditionCheckWithClosingStream();

        final NaturalSortingAction naturalSortingAction = new NaturalSortingAction();
        actions.insert(actions.end++, naturalSortingAction);

        return new UtIntStream(this);
    }

    @Override
    public IntStream peek(IntConsumer action) {
        preconditionCheckWithoutClosing();

        final IntConsumerAction consumerAction = new IntConsumerAction(action);
        actions.insert(actions.end++, consumerAction);

        return new UtIntStream(this);
    }

    @Override
    public IntStream limit(long maxSize) {
        preconditionCheckWithClosingStream();

        if (maxSize < 0) {
            throw new IllegalArgumentException();
        }

        assumeOrExecuteConcretely(maxSize <= Integer.MAX_VALUE);

        final LimitAction limitAction = new LimitAction((int) maxSize);
        actions.set(actions.end++, limitAction);

        return new UtIntStream(this);
    }

    @Override
    public IntStream skip(long n) {
        preconditionCheckWithClosingStream();

        if (n < 0) {
            throw new IllegalArgumentException();
        }

        assumeOrExecuteConcretely(n <= Integer.MAX_VALUE);

        final SkipAction skipAction = new SkipAction((int) n);
        actions.insert(actions.end++, skipAction);

        return new UtIntStream(this);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void forEach(IntConsumer action) {
        peek(action);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void forEachOrdered(IntConsumer action) {
        peek(action);
    }

    @Override
    public int[] toArray() {
        preconditionCheckWithClosingStream();

        final Object[] objects = applyActions(origin.toArray());

        final int[] result = new int[objects.length];
        int i = 0;
        for (Object object : objects) {
            result[i++] = (Integer) object;
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
    public int reduce(int identity, IntBinaryOperator op) {
        int result = identity;

        for (int element : toArray()) {
            result = op.applyAsInt(result, element);
        }

        return result;
    }

    @Override
    public OptionalInt reduce(IntBinaryOperator op) {
        int[] finalElements = toArray();

        int size = finalElements.length;
        if (size == 0) {
            return OptionalInt.empty();
        }

        Integer result = null;

        for (int element : finalElements) {
            if (result == null) {
                result = element;
            } else {
                result = op.applyAsInt(result, element);
            }
        }

        return OptionalInt.of(result);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        int[] finalElements = toArray();

        // since this implementation is always sequential, we do not need to use the combiner
        R result = supplier.get();

        for (int element : finalElements) {
            accumulator.accept(result, element);
        }

        return result;
    }

    @Override
    public int sum() {
        int[] finalElements = toArray();

        int sum = 0;
        for (int element : finalElements) {
            sum += element;
        }

        return sum;
    }

    @Override
    public OptionalInt min() {
        int[] finalElements = toArray();

        final int length = finalElements.length;
        if (length == 0) {
            return OptionalInt.empty();
        }

        int min = finalElements[0];
        for (int element : finalElements) {
            if (element < min) {
                min = element;
            }
        }

        return OptionalInt.of(min);
    }

    @Override
    public OptionalInt max() {
        int[] finalElements = toArray();

        final int length = finalElements.length;
        if (length == 0) {
            return OptionalInt.empty();
        }

        int max = finalElements[0];
        for (int element : finalElements) {
            if (element > max) {
                max = element;
            }
        }

        return OptionalInt.of(max);
    }

    @Override
    public long count() {
        int[] finalElements = toArray();

        return finalElements.length;
    }

    @Override
    public OptionalDouble average() {
        int[] finalElements = toArray();

        final int length = finalElements.length;
        if (length == 0) {
            return OptionalDouble.empty();
        }

        int sum = 0;
        for (int element : finalElements) {
            sum += element;
        }

        final double average = ((double) sum) / length;

        return OptionalDouble.of(average);
    }

    @Override
    public IntSummaryStatistics summaryStatistics() {
        int[] finalElements = toArray();

        IntSummaryStatistics statistics = new IntSummaryStatistics();

        for (int element : finalElements) {
            statistics.accept(element);
        }

        return statistics;
    }

    @Override
    public boolean anyMatch(IntPredicate predicate) {
        int[] finalElements = toArray();

        for (int element : finalElements) {
            if (predicate.test(element)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean allMatch(IntPredicate predicate) {
        int[] finalElements = toArray();

        for (int element : finalElements) {
            if (!predicate.test(element)) {
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
        int[] finalElements = toArray();

        if (finalElements.length == 0) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(finalElements[0]);
    }

    @Override
    public OptionalInt findAny() {
        // since this implementation is always sequential, we can just return the first element
        return findFirst();
    }

    @Override
    public LongStream asLongStream() {
        preconditionCheckWithClosingStream();

        return new UtLongStream(this);
    }

    @Override
    public DoubleStream asDoubleStream() {
        preconditionCheckWithClosingStream();

        return new UtDoubleStream(this);
    }

    @Override
    public Stream<Integer> boxed() {
        preconditionCheckWithClosingStream();

        return new UtStream<>(this);
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
        int[] finalElements = toArray();

        return new UtIntStreamIterator(finalElements);
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

    public static class UtIntStreamIterator implements PrimitiveIterator.OfInt {
        private final int[] data;

        private final int lastIndex;

        int index;

        public UtIntStreamIterator(int[] data) {
            this.data = data;
            lastIndex = data.length - 1;
        }

        @Override
        public boolean hasNext() {
            return index <= lastIndex;
        }

        @Override
        public int nextInt() {
            return data[index++];
        }

        @Override
        public Integer next() {
            return data[index++];
        }
    }
}
