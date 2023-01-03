package org.utbot.engine.overrides.threads;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class UtCompletableFuture<T> implements ScheduledFuture<T>, CompletionStage<T> {
    T result;

    Throwable exception;

    public UtCompletableFuture(T result) {
        this.result = result;
    }

    public UtCompletableFuture() {}

    public UtCompletableFuture(Throwable exception) {
        this.exception = exception;
    }

    public UtCompletableFuture(UtCompletableFuture<T> future) {
        result = future.result;
        exception = future.exception;
    }

    public void eqGenericType(T ignoredValue) {
        // Will be processed symbolically
    }

    public void preconditionCheck() {
        eqGenericType(result);
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        preconditionCheck();

        final U nextResult;
        try {
            nextResult = fn.apply(result);
        } catch (Throwable e) {
            return new UtCompletableFuture<U>(e).toCompletableFuture();
        }

        return new UtCompletableFuture<>(nextResult).toCompletableFuture();
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return thenApply(fn);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return thenApply(fn);
    }

    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        preconditionCheck();

        try {
            action.accept(result);
        } catch (Throwable e) {
            return new UtCompletableFuture<Void>(e).toCompletableFuture();
        }

        return new UtCompletableFuture<Void>().toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return thenAccept(action);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return thenAccept(action);
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        preconditionCheck();

        try {
            action.run();
        } catch (Throwable e) {
            return new UtCompletableFuture<Void>(e).toCompletableFuture();
        }

        return new UtCompletableFuture<Void>().toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return thenRun(action);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return thenRun(action);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        preconditionCheck();

        final CompletableFuture<? extends U> completableFuture = other.toCompletableFuture();
        if (fn == null || completableFuture == null) {
            throw new NullPointerException();
        }

        U otherResult;
        try {
            otherResult = completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        final V nextResult;
        try {
            nextResult = fn.apply(result, otherResult);
        } catch (Throwable e) {
            return new UtCompletableFuture<V>(e).toCompletableFuture();
        }

        return new UtCompletableFuture<>(nextResult).toCompletableFuture();
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return thenCombine(other, fn);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return thenCombine(other, fn);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        preconditionCheck();

        final CompletableFuture<? extends U> completableFuture = other.toCompletableFuture();
        if (action == null || completableFuture == null) {
            throw new NullPointerException();
        }

        U otherResult;
        try {
            otherResult = completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        try {
            action.accept(result, otherResult);
        } catch (Throwable e) {
            return new UtCompletableFuture<Void>(e).toCompletableFuture();
        }

        return new UtCompletableFuture<Void>().toCompletableFuture();
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return thenAcceptBoth(other, action);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        return thenAcceptBoth(other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        preconditionCheck();

        final CompletableFuture<?> completableFuture = other.toCompletableFuture();
        if (action == null || completableFuture == null) {
            throw new NullPointerException();
        }

        try {
            action.run();
        } catch (Throwable e) {
            return new UtCompletableFuture<Void>(e).toCompletableFuture();
        }

        return new UtCompletableFuture<Void>().toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return runAfterBoth(other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return runAfterBoth(other, action);
    }

    @Override
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        preconditionCheck();

        final CompletableFuture<? extends T> completableFuture = other.toCompletableFuture();
        if (fn == null || completableFuture == null) {
            throw new NullPointerException();
        }

        final T eitherResult;
        try {
            eitherResult = (result != null) ? result : completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        final U newResult;
        try {
            newResult = fn.apply(eitherResult);
        } catch (Throwable e) {
            return new UtCompletableFuture<U>(e).toCompletableFuture();
        }

        return new UtCompletableFuture<>(newResult).toCompletableFuture();
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return applyToEither(other, fn);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
        return applyToEither(other, fn);
    }

    @Override
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        preconditionCheck();

        final CompletableFuture<? extends T> completableFuture = other.toCompletableFuture();
        if (action == null || completableFuture == null) {
            throw new NullPointerException();
        }

        final T eitherResult;
        try {
            eitherResult = (result != null) ? result : completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        try {
            action.accept(eitherResult);
        } catch (Throwable e) {
            return new UtCompletableFuture<Void>(e).toCompletableFuture();
        }

        return new UtCompletableFuture<Void>().toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return acceptEither(other, action);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        return acceptEither(other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        preconditionCheck();

        try {
            action.run();
        } catch (Throwable e) {
            return new UtCompletableFuture<Void>(e).toCompletableFuture();
        }

        return new UtCompletableFuture<Void>().toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return runAfterEither(other, action);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return runAfterEither(other, action);
    }

    @Override
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        preconditionCheck();

        return fn.apply(result).toCompletableFuture();
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return thenCompose(fn);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return thenCompose(fn);
    }

    @Override
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        preconditionCheck();

        U newResult;
        try {
            newResult = fn.apply(result, exception);
        } catch (Throwable e) {
            return new UtCompletableFuture<U>(e).toCompletableFuture();
        }

        return new UtCompletableFuture<>(newResult).toCompletableFuture();
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return handle(fn);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return handle(fn);
    }

    @Override
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        preconditionCheck();

        final UtCompletableFuture<T> next = new UtCompletableFuture<>(this);
        try {
            action.accept(next.result, next.exception);
        } catch (Throwable e) {
            return new UtCompletableFuture<T>(e).toCompletableFuture();
        }

        return next.toCompletableFuture();
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return whenComplete(action);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return whenComplete(action);
    }

    @Override
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        preconditionCheck();

        if (fn == null) {
            throw new NullPointerException();
        }

        if (exception != null) {
            try {
                final T exceptionalResult = fn.apply(exception);
                return new UtCompletableFuture<>(exceptionalResult).toCompletableFuture();
            } catch (Throwable e) {
                return new UtCompletableFuture<T>(e).toCompletableFuture();
            }
        }

        return new UtCompletableFuture<>(result).toCompletableFuture();
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        // Will be processed symbolically
        return null;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        preconditionCheck();
        // Tasks could not be canceled since they are supposed to be executed immediately
        return false;
    }

    @Override
    public boolean isCancelled() {
        preconditionCheck();
        // Tasks could not be canceled since they are supposed to be executed immediately
        return false;
    }

    @Override
    public boolean isDone() {
        preconditionCheck();

        return true;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        preconditionCheck();

        if (exception != null) {
            throw new ExecutionException(exception);
        }

        return result;
    }

    @Override
    public T get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return get();
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return 0;
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        if (o == this) { // compare zero if same object{
            return 0;
        }

        long diff = getDelay(NANOSECONDS) - o.getDelay(NANOSECONDS);
        return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
    }
}
