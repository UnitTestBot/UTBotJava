package org.utbot.engine.overrides.threads;

import org.utbot.api.annotation.UtClassMock;

import java.util.concurrent.Executor;

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
}
