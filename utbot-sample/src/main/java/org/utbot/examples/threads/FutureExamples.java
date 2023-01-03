package org.utbot.examples.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class FutureExamples {
    public void throwingRunnableExample() throws ExecutionException, InterruptedException {
        final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            throw new IllegalStateException();
        });

        future.get();
    }

    public int resultFromGet() throws ExecutionException, InterruptedException {
        final CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 42);

        return future.get();
    }

    public int changingCollectionInFuture() throws ExecutionException, InterruptedException {
        List<Integer> values = new ArrayList<>();

        final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> values.add(42));

        future.get();

        return values.get(0);
    }

    // NOTE: this tests looks similar as the test above BUT it is important to check correctness of the wrapper
    // for CompletableFuture - an actions is executed regardless of invoking `get` method.
    @SuppressWarnings("unused")
    public int changingCollectionInFutureWithoutGet() {
        List<Integer> values = new ArrayList<>();

        final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> values.add(42));

        return values.get(0);
    }
}
