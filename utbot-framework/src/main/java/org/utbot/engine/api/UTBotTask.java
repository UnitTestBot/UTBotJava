package org.utbot.engine.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utbot.engine.EngineController;
import org.utbot.engine.MockStrategy;
import org.utbot.engine.UtBotSymbolicEngine;
import org.utbot.engine.state.ExecutionState;
import org.utbot.framework.plugin.api.ApplicationContext;
import org.utbot.framework.plugin.api.ClassId;
import org.utbot.framework.plugin.api.MethodId;
import org.utbot.framework.plugin.api.util.UtContext;
import org.utbot.framework.plugin.api.util.UtContextKt;
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider;
import org.utbot.framework.util.SootUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class UTBotTask extends RecursiveTask<Void> {
    /**
     * Use this lock to prevent from UTBotEngine been accessed simultaneously from other places.
     * <p>
     * Usually, you don't need to use this lock, but it's marked as public to debug cases,
     * when the engine crashes because of concurrency.
     */
    public static final Lock GLOBAL_LOCK = new ReentrantLock();
    private final MethodId method;
    private final String classpath;

    public static final Set<ClassId> globalMockClasses = new HashSet<>();

    private final Consumer<UTBotIterator.TransferData<ExecutionState>> stateConsumer;

    private static final Logger LOGGER = LoggerFactory.getLogger(UTBotTask.class);

    public UTBotTask(MethodId method, String classpath, Consumer<UTBotIterator.TransferData<ExecutionState>> stateConsumer) {
        this.method = method;
        this.classpath = classpath;
        this.stateConsumer = stateConsumer;
    }

    @Override
    public Void compute() {
        try {
            // init Soot if it is not yet
            SootUtils.INSTANCE.runSoot(Arrays.stream(
                    classpath.split(File.pathSeparator)).map(Paths::get).collect(Collectors.toList()
            ), classpath, false, new JdkInfoDefaultProvider().getInfo());

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            UtContext utContext = new UtContext(classLoader);
            UtContextKt.withUtContext(utContext, () -> {
                // Do not use try-catch because of JVM crash
                @SuppressWarnings("resource") UtBotSymbolicEngine engine;
                try {
                    GLOBAL_LOCK.lockInterruptibly();
                    engine = new UtBotSymbolicEngine(
                            new EngineController(),
                            method,
                            classpath,
                            "", // it is unused right now
                            MockStrategy.NO_MOCKS,
                            new HashSet<>(globalMockClasses),
                            new ApplicationContext(),
                            500
                    );
                } catch (InterruptedException e) {
                    CancellationException ce = new CancellationException();
                    ce.addSuppressed(e);
                    throw ce;
                } finally {
                    GLOBAL_LOCK.unlock();
                }
                engine.addListener((graph, executionState) -> {
                    try {
                        stateConsumer.accept(new UTBotIterator.TransferData<>(executionState));
                    } catch (Throwable t) {
                        if (t instanceof CancellationException) {
                            throw t;
                        } else {
                            LOGGER.error("Error in state consumer occurred", t);
                            CancellationException ce = new CancellationException();
                            ce.addSuppressed(t);
                            throw ce;
                        }
                    }
                });
                engine.traverseAndConsumeAllStates();
                stateConsumer.accept(UTBotIterator.NULL);
                return null;
            });
        } catch (Throwable t) {
            LOGGER.error("Error in callable occurred", t);
            cancel(true);
            stateConsumer.accept(UTBotIterator.NULL);
        }
        return null;
    }
}
