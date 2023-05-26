package org.utbot.engine.api;

import org.utbot.engine.state.ExecutionState;
import org.utbot.framework.plugin.api.MethodId;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This iterator encapsulates the event-based listener of UTBot traversing events
 * and gives sequel access to symbolic states using the default iterator interface.
 */
public class UTBotIterator implements Iterator<ExecutionState>, AutoCloseable {

    private final SynchronousQueue<TransferData<ExecutionState>> queue = new SynchronousQueue<>();
    private final Semaphore continueEngineListening = new Semaphore(0);
    private final UTBotForkJoinPool forkJoinPool;
    private final UTBotTask callable;
    private TransferData<ExecutionState> value = null;
    private final AtomicReference<State> state;

    public UTBotIterator(UTBotForkJoinPool forkJoinPool, MethodId method, String classpath) {
        this.forkJoinPool = forkJoinPool;

        callable = new UTBotTask(
                method,
                classpath,
                symbolicState -> {
                    try {
                        if (!checkCancelled(symbolicState)) {
                            queue.put(symbolicState);
                        }
                        if (symbolicState != NULL) {
                            checkCancelled(symbolicState);
                            continueEngineListening.acquire();
                            checkCancelled(symbolicState);
                        }
                    } catch (InterruptedException e) {
                        cancel(e);
                    }
                }
        );
        state = new AtomicReference<>(State.READY);
    }

    public static Stream<ExecutionState> stream(MethodId method, String classpath) {
        return stream(method, classpath, new UTBotForkJoinPool());
    }

    public static Stream<ExecutionState> stream(MethodId method, String classpath, UTBotForkJoinPool fjp) {
        UTBotIterator iterator = new UTBotIterator(fjp, method, classpath);
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, 0), false
        ).onClose(iterator::close);
    }

    private void tryLoad() throws InterruptedException {
        if (state.get() == State.READY) {
            forkJoinPool.submit(callable);
            state.set(State.RUNNING);
        } else {
            continueEngineListening.release();
        }
        value = queue.take();
    }

    @Override
    public boolean hasNext() {
        if (value == null) {
            try {
                tryLoad();
            } catch (InterruptedException e) {
                return false;
            }
        }
        return value != UTBotIterator.NULL;
    }

    @Override
    public ExecutionState next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        ExecutionState result = value.executionState;
        value = null;
        return result;
    }

    private boolean checkCancelled(TransferData<ExecutionState> value) {
        if (state.get() == State.DONE) {
            if (value != UTBotIterator.NULL) {
                cancel(null);
            }
            return true;
        }
        return false;
    }

    private void cancel(Throwable reason) {
        CancellationException ce = new CancellationException();
        if (reason != null) {
            ce.addSuppressed(reason);
        }
        throw ce;
    }

    @Override
    public void close() {
        if (state.getAndSet(State.DONE) != State.DONE) {
            // Release the task from being waiting smth:
            // - cancel the callable
            callable.cancel(true);
            // - clear the queue if needed
            queue.poll();
            // - release the lock for next utbot engine round
            continueEngineListening.release();
        }
    }

    private enum State {
        READY, RUNNING, DONE
    }

    static class TransferData<V> {
        private final V executionState;

        public TransferData(V executionState) {
            this.executionState = executionState;
        }

        public V getExecutionState() {
            return executionState;
        }
    }

    static final TransferData<ExecutionState> NULL = new TransferData<>(null);
}
