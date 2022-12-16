package org.utbot.examples.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ExecutorServiceExamples {
    public void throwingInExecute() {
        Executors.newSingleThreadExecutor().execute(() -> {
            throw new IllegalStateException();
        });
    }

    public int changingCollectionInExecute() {
        List<Integer> list = new ArrayList<>();

        Executors.newSingleThreadExecutor().execute(() -> {
            list.add(42);
        });

        return list.get(0);
    }
}
