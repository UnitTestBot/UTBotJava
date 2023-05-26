package org.utbot.engine.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

public final class UTBotForkJoinPool extends ForkJoinPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(UTBotForkJoinPool.class);

    public UTBotForkJoinPool() {
        this(
                Runtime.getRuntime().availableProcessors(),
                ClassLoader.getSystemClassLoader()
        );
    }

    @SuppressWarnings("unused") // used in API
    public UTBotForkJoinPool(UTBotClassLoader classLoader) {
        this(
                Runtime.getRuntime().availableProcessors(),
                classLoader
        );
    }

    private UTBotForkJoinPool(int parallelism, ClassLoader classLoader) {
        super(
                parallelism,
                new UTBotWorkerThreadFactory(classLoader),
                (t, e) -> {
                    if (!(e instanceof CancellationException)) {
                        LOGGER.error("Internal Error in thread " + t, e);
                    }
                },
                true
        );
    }

    private static class UTBotWorkerThreadFactory implements ForkJoinWorkerThreadFactory {

        private final ClassLoader utBotClassloader;

        public UTBotWorkerThreadFactory(ClassLoader utBotClassloader) {
            this.utBotClassloader = utBotClassloader;
        }

        @Override
        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return new UTBotForkJoinWorkerThread(pool, utBotClassloader);
        }

        private static class UTBotForkJoinWorkerThread extends ForkJoinWorkerThread {

            private UTBotForkJoinWorkerThread(final ForkJoinPool pool, ClassLoader classloader) {
                super(pool);
                setContextClassLoader(classloader);
            }
        }
    }

}
