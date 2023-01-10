package org.utbot.engine.overrides.threads;

import org.utbot.api.annotation.UtClassMock;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

@UtClassMock(target = java.util.concurrent.CompletableFuture.class, internalUsage = true)
public class CompletableFuture {
    public static java.util.concurrent.CompletableFuture<Void> runAsync(Runnable runnable) {
        java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();

        return future.thenRun(runnable);
    }

    @SuppressWarnings("unused")
    public static java.util.concurrent.CompletableFuture<Void> runAsync(Runnable runnable, Executor ignoredExecutor) {
        return runAsync(runnable);
    }

    public static <U> java.util.concurrent.CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        try {
            final U value = supplier.get();

            return new UtCompletableFuture<>(value).toCompletableFuture();
        } catch (Throwable e) {
            return new UtCompletableFuture<U>(e).toCompletableFuture();
        }
    }
}
