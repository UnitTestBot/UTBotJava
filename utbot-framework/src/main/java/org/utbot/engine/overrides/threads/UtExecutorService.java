package org.utbot.engine.overrides.threads;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

public class UtExecutorService implements ExecutorService, ScheduledExecutorService {
    private boolean isShutdown;
    private boolean isTerminated;

    public UtExecutorService() {
        visit(this);
    }

    private void preconditionCheck() {
        if (alreadyVisited(this)) {
            return;
        }

        visit(this);
    }

    @Override
    public void shutdown() {
        preconditionCheck();

        isShutdown = true;
        isTerminated = true;
    }

    @NotNull
    @Override
    public List<Runnable> shutdownNow() {
        preconditionCheck();

        shutdown();
        // Since all tasks are processed immediately, there are no waiting tasks
        return new ArrayList<>();
    }

    @Override
    public boolean isShutdown() {
        preconditionCheck();

        return isShutdown;
    }

    @Override
    public boolean isTerminated() {
        preconditionCheck();

        return isTerminated;
    }

    @Override
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) {
        // No need to wait tasks
        return true;
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Callable<T> task) {
        preconditionCheck();

        try {
            T result = task.call();
            return new UtCompletableFuture<>(result);
        } catch (Exception e) {
            return new UtCompletableFuture<>(e);
        }
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Runnable task, T result) {
        preconditionCheck();

        try {
            task.run();
            return new UtCompletableFuture<>(result);
        } catch (Exception e) {
            return new UtCompletableFuture<>(e);
        }
    }

    @NotNull
    @Override
    public Future<?> submit(@NotNull Runnable task) {
        return submit(task, null);
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) {
        preconditionCheck();

        List<Future<T>> results = new ArrayList<>();
        for (Callable<T> task : tasks) {
            results.add(submit(task));
        }

        return results;
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) {
        return invokeAll(tasks);
    }

    @NotNull
    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws ExecutionException {
        preconditionCheck();

        for (Callable<T> task : tasks) {
            try {
                return task.call();
            } catch (Exception e) {
                // Do nothing
            }
        }

        // ExecutionException no-parameters constructor is protected
        throw new ExecutionException(new RuntimeException());
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws ExecutionException {
        return invokeAny(tasks);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        preconditionCheck();

        command.run();
    }

    @NotNull
    @Override
    public ScheduledFuture<?> schedule(@NotNull Runnable command, long delay, @NotNull TimeUnit unit) {
        preconditionCheck();

        try {
            command.run();
            return new UtCompletableFuture<>();
        } catch (Exception e) {
            return new UtCompletableFuture<>(e);
        }
    }

    @NotNull
    @Override
    public <V> ScheduledFuture<V> schedule(@NotNull Callable<V> callable, long delay, @NotNull TimeUnit unit) {
        preconditionCheck();

        try {
            V result = callable.call();
            return new UtCompletableFuture<>(result);
        } catch (Exception e) {
            return new UtCompletableFuture<>(e);
        }
    }

    @NotNull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit) {
        preconditionCheck();

        if (period <= 0) {
            throw new IllegalArgumentException();
        }

        return schedule(command, initialDelay, unit);
    }

    @NotNull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull Runnable command, long initialDelay, long delay, @NotNull TimeUnit unit) {
        preconditionCheck();

        if (delay <= 0) {
            throw new IllegalArgumentException();
        }

        return schedule(command, initialDelay, unit);
    }
}
