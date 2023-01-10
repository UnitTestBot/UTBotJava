package org.utbot.examples.threads;

import java.util.ArrayList;
import java.util.List;

public class ThreadExamples {
    public void explicitExceptionInStart() {
        new Thread(() -> {
            throw new IllegalStateException();
        }).start();
    }

    public int changingCollectionInThread() {
        List<Integer> values = new ArrayList<>();

        new Thread(() -> values.add(42)).start();

        return values.get(0);
    }

    @SuppressWarnings("unused")
    public int changingCollectionInThreadWithoutStart() {
        List<Integer> values = new ArrayList<>();

        final Thread thread = new Thread(() -> values.add(42));

        return values.get(0);
    }
}
