package org.utbot.engine.overrides.stream;

import org.utbot.api.mock.UtMock;
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
import org.utbot.engine.overrides.stream.actions.ConsumerAction;
import org.utbot.engine.overrides.stream.actions.DistinctAction;
import org.utbot.engine.overrides.stream.actions.FilterAction;
import org.utbot.engine.overrides.stream.actions.LimitAction;
import org.utbot.engine.overrides.stream.actions.MapAction;
import org.utbot.engine.overrides.stream.actions.NaturalSortingAction;
import org.utbot.engine.overrides.stream.actions.SkipAction;
import org.utbot.engine.overrides.stream.actions.SortingAction;
import org.utbot.engine.overrides.stream.actions.StreamAction;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.api.mock.UtMock.assumeOrExecuteConcretely;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.executeConcretely;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

@SuppressWarnings({"rawtypes", "unchecked"})
public class UtStream<E> implements Stream<E>, UtGenericStorage<E> {
    /**
     * A reference to the original collection. The default collection is {@link UtArrayList}.
     */
    private final Collection<E> origin;

    private final RangeModifiableUnlimitedArray<StreamAction> actions;

    private final RangeModifiableUnlimitedArray<Runnable> closeHandlers;

    private boolean isParallel = false;

    /**
     * {@code false} by default, assigned to {@code true} after performing any operation on this stream. Any operation,
     * performed on a closed UtStream, throws the {@link IllegalStateException}.
     */
    private boolean isClosed = false;

    public UtStream(Collection<E> collection) {
        visit(this);

        actions = new RangeModifiableUnlimitedArray<>();
        closeHandlers = new RangeModifiableUnlimitedArray<>();

        origin = collection;
    }

    public UtStream() {
        this(new UtArrayList<>());
    }

    public UtStream(E[] data) {
        this(data, 0, data.length);
    }

    public UtStream(E[] data, int length) {
        this(data, 0, length);
    }

    public UtStream(E[] data, int startInclusive, int endExclusive) {
        this(new UtArrayList<>(data, startInclusive, endExclusive));
    }

    public UtStream(UtStream other) {
        visit(this);

        origin = other.origin;

        actions = other.actions;

        isParallel = other.isParallel;

        // new stream should be opened
        isClosed = false;

        closeHandlers = other.closeHandlers;
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

    @Override
    public Stream<E> filter(Predicate<? super E> predicate) {
        preconditionCheckWithClosingStream();

        final FilterAction filterAction = new FilterAction((Predicate<Object>) predicate);
        actions.insert(actions.end++, filterAction);

        return new UtStream<>(this);
    }

    private int filterInvocation(Object[] originArray, Object[] filtered, Predicate filter) {
        int newSize = 0;

        for (Object o : originArray) {
            if (filter.test(o)) {
                filtered[newSize++] = o;
            }
        }

        return newSize;
    }

    @Override
    public <R> Stream<R> map(Function<? super E, ? extends R> mapper) {
        preconditionCheckWithClosingStream();

        final MapAction mapAction = new MapAction(mapper);
        actions.insert(actions.end++, mapAction);

        return new UtStream<>(this);
    }

    private void mapInvocation(Object[] originArray, Object[] transformed, Function mapping) {
        int newSize = 0;

        for (Object o : originArray) {
            transformed[newSize++] = mapping.apply(o);
        }
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super E> mapper) {
        // TODO
        preconditionCheckWithClosingStream();

        int size = origin.size();
        Integer[] data = new Integer[size];
        int i = 0;

        for (E element : origin) {
            data[i++] = mapper.applyAsInt(element);
        }

        return new UtIntStream(data, size);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super E> mapper) {
        // TODO
        preconditionCheckWithClosingStream();

        int size = origin.size();
        Long[] data = new Long[size];
        int i = 0;

        for (E element : origin) {
            data[i++] = mapper.applyAsLong(element);
        }

        return new UtLongStream(data, size);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super E> mapper) {
        // TODO
        preconditionCheckWithClosingStream();

        int size = origin.size();
        Double[] data = new Double[size];
        int i = 0;

        for (E element : origin) {
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

    @Override
    public Stream<E> distinct() {
        preconditionCheckWithClosingStream();

        final DistinctAction distinctAction = new DistinctAction();
        actions.insert(actions.end++, distinctAction);

        return new UtStream<>(this);
    }

    private int distinctInvocation(Object[] curElements, Object[] distinctElements) {
        int distinctSize = 0;

        for (Object element : curElements) {
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

        return distinctSize;
    }

    // TODO choose the best sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    @Override
    public Stream<E> sorted() {
        preconditionCheckWithClosingStream();

        final NaturalSortingAction naturalSortingAction = new NaturalSortingAction();
        actions.insert(actions.end++, naturalSortingAction);

        return new UtStream<>(this);
    }

    // TODO choose the best sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    @Override
    public Stream<E> sorted(Comparator<? super E> comparator) {
        preconditionCheckWithClosingStream();

        final SortingAction sortingAction = new SortingAction((Comparator<Object>) comparator);
        actions.insert(actions.end++, sortingAction);

        return new UtStream<>(this);
    }

    private void sortedInvocation(Object[] originElements, Object[] sortedElements, Comparator comparator, int size) {
        UtArrayMock.arraycopy(originElements, 0, sortedElements, 0, size);

        // bubble sort
        for (int i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - i - 1; j++) {
                if (comparator.compare(sortedElements[j], sortedElements[j + 1]) > 0) {
                    Object tmp = sortedElements[j];
                    sortedElements[j] = sortedElements[j + 1];
                    sortedElements[j + 1] = tmp;
                }
            }
        }
    }

    @Override
    public Stream<E> peek(Consumer<? super E> action) {
        preconditionCheckWithoutClosing();

        final ConsumerAction consumerAction = new ConsumerAction((Consumer<Object>) action);
        actions.insert(actions.end++, consumerAction);

        return new UtStream<>(this);
    }

    public void actionInvocation(Object[] originArray, Consumer action) {
        for (Object element : originArray) {
            action.accept(element);
        }
    }

    @Override
    public Stream<E> limit(long maxSize) {
        preconditionCheckWithClosingStream();

        if (maxSize < 0) {
            throw new IllegalArgumentException();
        }

        assumeOrExecuteConcretely(maxSize <= Integer.MAX_VALUE);

        final LimitAction limitAction = new LimitAction((int) maxSize);
        actions.set(actions.end++, limitAction);

        return new UtStream<>(this);
    }

    private Object[] limitInvocation(Object[] originArray, long maxSize, int curSize) {
        if (maxSize == 0) {
            return new Object[]{};
        }

        assumeOrExecuteConcretely(maxSize <= Integer.MAX_VALUE);

        int newSize = (int) maxSize;

        if (newSize > curSize) {
            newSize = curSize;
        }

        Object[] elements = new Object[newSize];
        int i = 0;

        for (Object element : originArray) {
            if (i >= newSize) {
                break;
            }

            elements[i++] = element;
        }

        return elements;
    }

    @Override
    public Stream<E> skip(long n) {
        preconditionCheckWithClosingStream();

        if (n < 0) {
            throw new IllegalArgumentException();
        }

        if (n == 0) {
            // do nothing
            return new UtStream<>(this);
        }

        final SkipAction skipAction = new SkipAction(n);
        actions.insert(actions.end++, skipAction);

        return new UtStream<>(this);
    }

    private Object[] skipInvocation(Object[] originArray, long n, int curSize) {
        if (n > curSize) {
            return new Object[]{};
        }

        // n is 1...Integer.MAX_VALUE here
        int newSize = (int) (curSize - n);

        if (newSize == 0) {
            return new Object[]{};
        }

        Object[] elements = new Object[newSize];
        int i = 0;
        int j = 0;

        for (Object element : originArray) {
            if (i++ < n) {
                break;
            }

            elements[j++] = element;
        }

        return elements;
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

        Object[] originArray = origin.toArray();
        UtMock.disableClassCastExceptionCheck(originArray);

        originArray = applyActions(originArray);

        return originArray;
    }

    @NotNull
    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        preconditionCheckWithClosingStream();

        // TODO untracked ArrayStoreException - JIRA:1089
        int size = origin.size();
        A[] array = generator.apply(size);

        UtArrayMock.arraycopy(origin.toArray(), 0, array, 0, size);

        final Object[] result = applyActions(array);
        UtMock.disableClassCastExceptionCheck(result);

        return (A[]) result;
    }

    @NotNull
    private Object[] applyActions(Object[] originArray) {
        int actionsNumber = actions.end;

        for (int i = 0; i < actionsNumber; i++) {
            originArray = actions.get(i).applyAction(originArray);
            UtMock.disableClassCastExceptionCheck(originArray); // TODO do we need it?
        }

        return originArray;
    }

    @Override
    public E reduce(E identity, BinaryOperator<E> accumulator) {
        E result = identity;

        for (Object element : toArray()) {
            result = accumulator.apply(result, (E) element);
        }

        return result;
    }

    @NotNull
    @Override
    public Optional<E> reduce(BinaryOperator<E> accumulator) {
        Object[] finalElements = toArray();

        int size = finalElements.length;
        if (size == 0) {
            return Optional.empty();
        }

        E result = null;

        for (Object element : finalElements) {
            if (result == null) {
                result = (E) element;
            } else {
                result = accumulator.apply(result, (E) element);
            }
        }

        return Optional.of(result);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super E, U> accumulator, BinaryOperator<U> combiner) {
        Object[] finalElements = toArray();

        // since this implementation is always sequential, we do not need to use the combiner
        U result = identity;

        for (Object element : finalElements) {
            result = accumulator.apply(result, (E) element);
        }

        return result;
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super E> accumulator, BiConsumer<R, R> combiner) {
        Object[] finalElements = toArray();

        // since this implementation is always sequential, we do not need to use the combiner
        R result = supplier.get();

        for (Object element : finalElements) {
            accumulator.accept(result, (E) element);
        }

        return result;
    }

    @Override
    public <R, A> R collect(Collector<? super E, A, R> collector) {
        Object[] finalElements = toArray();

        // since this implementation is always sequential, we do not need to use the combiner
        A result = collector.supplier().get();

        for (Object element : finalElements) {
            collector.accumulator().accept(result, (E) element);
        }

        return collector.finisher().apply(result);
    }

    @NotNull
    @Override
    public Optional<E> min(Comparator<? super E> comparator) {
        Object[] finalElements = toArray();

        int size = finalElements.length;
        if (size == 0) {
            return Optional.empty();
        }

        E min = (E) finalElements[0];

        for (int i = 1; i < size; i++) {
            E element = (E) finalElements[i];
            if (comparator.compare(min, element) > 0) {
                min = element;
            }
        }

        return Optional.of(min);
    }

    @NotNull
    @Override
    public Optional<E> max(Comparator<? super E> comparator) {
        Object[] finalElements = toArray();

        int size = finalElements.length;
        if (size == 0) {
            return Optional.empty();
        }

        E max = (E) finalElements[0];

        for (int i = 1; i < size; i++) {
            E element = (E) finalElements[i];
            if (comparator.compare(max, element) < 0) {
                max = element;
            }
        }

        return Optional.of(max);
    }

    @Override
    public long count() {
        Object[] finalElements = toArray();

        return finalElements.length;
    }

    @Override
    public boolean anyMatch(Predicate<? super E> predicate) {
        Object[] finalElements = toArray();

        for (Object element : finalElements) {
            if (predicate.test((E) element)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean allMatch(Predicate<? super E> predicate) {
        Object[] finalElements = toArray();

        for (Object element : finalElements) {
            if (!predicate.test((E) element)) {
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
        Object[] finalElements = toArray();

        if (finalElements.length == 0) {
            return Optional.empty();
        }

        return Optional.of((E) finalElements[0]);
    }

    @NotNull
    @Override
    public Optional<E> findAny() {
        // since this implementation is always sequential, we can just return the first element
        return findFirst();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        Object[] finalElements = toArray();

        return new UtArrayList<>((E[]) finalElements).iterator();
    }

    @NotNull
    @Override
    public Spliterator<E> spliterator() {
        // implementation from AbstractList
        return Spliterators.spliterator(toArray(), Spliterator.ORDERED);
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
