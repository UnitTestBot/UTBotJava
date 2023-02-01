package org.utbot.engine.overrides.threads;

import org.utbot.api.annotation.UtClassMock;
import org.utbot.api.mock.UtMock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

@UtClassMock(target = java.util.concurrent.Executors.class, internalUsage = true)
public class Executors {
    public static ExecutorService newFixedThreadPool(int nThreads) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException();
        }

        return new UtExecutorService();
    }

    public static ExecutorService newWorkStealingPool() {
        return new UtExecutorService();
    }

    public static ExecutorService newWorkStealingPool(int parallelism) {
        if (parallelism <= 0) {
            throw new IllegalArgumentException();
        }

        return new UtExecutorService();
    }

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }

        return newFixedThreadPool(nThreads);
    }

    public static ExecutorService newSingleThreadExecutor() {
        return new UtExecutorService();
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }

        return new UtExecutorService();
    }

    public static ExecutorService newCachedThreadPool() {
        return new UtExecutorService();
    }

    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }

        return new UtExecutorService();
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return new UtExecutorService();
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }

        return new UtExecutorService();
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException();
        }

        return new UtExecutorService();
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException();
        }

        if (threadFactory == null) {
            throw new NullPointerException();
        }

        return new UtExecutorService();
    }

    public static ExecutorService unconfigurableExecutorService(ExecutorService executor) {
        if (executor == null) {
            throw new NullPointerException();
        }

        return new UtExecutorService();
    }

    public static ScheduledExecutorService unconfigurableScheduledExecutorService(ScheduledExecutorService executor) {
        if (executor == null) {
            throw new NullPointerException();
        }

        return new UtExecutorService();
    }

    public static ThreadFactory defaultThreadFactory() {
        // TODO make a wrapper?
        return UtMock.makeSymbolic();
    }

    public static ThreadFactory privilegedThreadFactory() {
        return defaultThreadFactory();
    }
}
