package org.utbot.examples.threads;

public class ThreadStartExample {
    public void explicitExceptionInStart() {
        new Thread(() -> {
            throw new IllegalStateException();
        }).start();
    }
}
