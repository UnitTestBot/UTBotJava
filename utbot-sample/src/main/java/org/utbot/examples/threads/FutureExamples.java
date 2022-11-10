package org.utbot.examples.threads;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class FutureExamples {
    void throwingRunnableExample() throws ExecutionException, InterruptedException {
        final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            throw new IllegalStateException();
        });

        future.get();
    }
}
