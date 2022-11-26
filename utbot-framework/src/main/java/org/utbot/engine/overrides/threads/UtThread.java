package org.utbot.engine.overrides.threads;

import org.utbot.api.mock.UtMock;

import java.security.AccessControlContext;
import java.util.Map;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

@SuppressWarnings("unused")
public class UtThread {
    private String name;
    private int priority;

    private boolean daemon;

    private final Runnable target;

    private final ThreadGroup group;

    /* The context ClassLoader for this thread */
    private ClassLoader contextClassLoader;

    /* For autonumbering anonymous threads. */
    private static int threadInitNumber = 0;

    private static int nextThreadNum() {
        return threadInitNumber++;
    }

    /*
     * Thread ID
     */
    private final long tid;

    /* For generating thread ID */
    private static long threadSeqNumber;

    private static long nextThreadID() {
        return ++threadSeqNumber;
    }

    private boolean isInterrupted;

    // This field is required by ThreadLocal class. The real type is a package-private type ThreadLocal.ThreadLocalMap
    Object threadLocals;

    // This field is required by InheritableThreadLocal class. The real type is a package-private type ThreadLocal.ThreadLocalMap
    Object inheritableThreadLocals;

    /**
     * The minimum priority that a thread can have.
     */
    public static final int MIN_PRIORITY = 1;

    /**
     * The maximum priority that a thread can have.
     */
    public static final int MAX_PRIORITY = 10;

    // null unless explicitly set
    private volatile Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    // null unless explicitly set
    private static volatile Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    public static UtThread currentThread() {
        return UtMock.makeSymbolic();
    }

    public static void yield() {
        // Do nothing
    }

    @SuppressWarnings("RedundantThrows")
    public static void sleep(long ignoredMillis) throws InterruptedException {
        // Do nothing
    }

    @SuppressWarnings("RedundantThrows")
    public static void sleep(long millis, int nanos) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException();
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException();
        }
    }

    public UtThread() {
        this(null, null, UtMock.makeSymbolic(), 0);
    }

    public UtThread(Runnable target) {
        this(null, target, UtMock.makeSymbolic(), 0);
    }

    public UtThread(ThreadGroup group, Runnable target) {
        this(group, target, UtMock.makeSymbolic(), 0);
    }

    public UtThread(String name) {
        this(null, null, name, 0);
    }

    public UtThread(ThreadGroup group, String name) {
        this(group, null, name, 0);
    }

    public UtThread(Runnable target, String name) {
        this(null, target, name, 0);
    }

    public UtThread(ThreadGroup group, Runnable target, String name) {
        this(group, target, name, 0);
    }

    public UtThread(ThreadGroup group, Runnable target, String name,
                    long stackSize) {
        this(group, target, name, stackSize, null, true);
    }

    public UtThread(ThreadGroup group, Runnable target, String name,
                    long stackSize, boolean inheritUtThreadLocals) {
        this(group, target, name, stackSize, null, inheritUtThreadLocals);
    }

    private UtThread(ThreadGroup g, Runnable target, String name,
                     long stackSize, AccessControlContext ignoredAcc,
                     boolean ignoredInheritUtThreadLocals) {
        visit(this);

        if (name == null) {
            throw new NullPointerException();
        }

        this.name = name;

        if (g == null) {
            g = UtMock.makeSymbolic();
        }

        this.group = g;
        this.daemon = UtMock.makeSymbolic();

        final Integer priority = UtMock.makeSymbolic();
        assume(priority >= MIN_PRIORITY);
        assume(priority <= MIN_PRIORITY);
        this.priority = priority;
        setPriority(this.priority);

        this.contextClassLoader = UtMock.makeSymbolic();
        this.target = target;

        this.tid = nextThreadID();

        // We need to make it possible to cast these fields to the ThreadLocal.ThreadLocalMap type
        UtMock.disableClassCastExceptionCheck(threadLocals);
        UtMock.disableClassCastExceptionCheck(inheritableThreadLocals);
    }

    public void preconditionCheck() {
        if (alreadyVisited(this)) {
            return;
        }

        assume(name != null);

        assume(priority >= MIN_PRIORITY);
        assume(priority <= MIN_PRIORITY);

        assume(group != null);
        assume(contextClassLoader != null);

        assume(threadInitNumber >= 0);
        assume(tid >= 0);
        assume(threadSeqNumber >= 0);

        visit(this);
    }

    public void start() {
        run();
    }

    public void run() {
        preconditionCheck();

        if (target != null) {
            target.run();
        }
    }

    public final void stop() {
        preconditionCheck();
        // DO nothing
    }

    public void interrupt() {
        preconditionCheck();
        // Set interrupted status
        isInterrupted = true;
    }

    public static boolean interrupted() {
        return UtMock.makeSymbolic();
    }

    public boolean isInterrupted() {
        preconditionCheck();

        return isInterrupted;
    }

    private boolean isInterrupted(boolean clearInterrupted) {
        preconditionCheck();

        boolean result = isInterrupted;

        if (clearInterrupted) {
            isInterrupted = false;
        }

        return result;
    }

    public final boolean isAlive() {
        preconditionCheck();

        return UtMock.makeSymbolic();
    }

    public final void suspend() {
        preconditionCheck();
        // Do nothing
    }

    public final void resume() {
        preconditionCheck();
        // Do nothing
    }

    public final void setPriority(int newPriority) {
        preconditionCheck();

        if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }

        if (group != null) {
            if (newPriority > group.getMaxPriority()) {
                newPriority = group.getMaxPriority();
            }

            priority = newPriority;
        }
    }

    public final int getPriority() {
        preconditionCheck();

        return priority;
    }

    public final void setName(String name) {
        preconditionCheck();

        if (name == null) {
            throw new NullPointerException();
        }

        this.name = name;
    }

    public final String getName() {
        preconditionCheck();

        return name;
    }

    public final ThreadGroup getThreadGroup() {
        preconditionCheck();

        return group;
    }

    public static int activeCount() {
        final Integer result = UtMock.makeSymbolic();
        assume(result >= 0);

        return result;
    }

    public static int enumerate(UtThread[] tarray) {
        Integer length = UtMock.makeSymbolic();
        assume(length >= 0);
        assume(length <= tarray.length);

        for (int i = 0; i < length; i++) {
            tarray[i] = UtMock.makeSymbolic();
        }

        return length;
    }

    public int countStackFrames() {
        preconditionCheck();

        return UtMock.makeSymbolic();
    }

    public final void join(long millis) throws InterruptedException {
        preconditionCheck();

        if (millis < 0) {
            throw new IllegalArgumentException();
        }

        // Do nothing
    }

    public final void join(long millis, int nanos) throws InterruptedException {
        preconditionCheck();

        if (millis < 0) {
            throw new IllegalArgumentException();
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException();
        }

        // Do nothing
    }

    public final void join() throws InterruptedException {
        preconditionCheck();

        join(0);
    }

    public static void dumpStack() {
        // Do nothing
    }

    public final void setDaemon(boolean on) {
        preconditionCheck();

        daemon = on;
    }

    public final boolean isDaemon() {
        preconditionCheck();

        return daemon;
    }

    public String toString() {
        preconditionCheck();

        if (group != null) {
            return "Thread[" + getName() + "," + getPriority() + "," +
                    group.getName() + "]";
        } else {
            return "Thread[" + getName() + "," + getPriority() + "," +
                    "" + "]";
        }
    }

    public ClassLoader getContextClassLoader() {
        preconditionCheck();

        return contextClassLoader;
    }

    public void setContextClassLoader(ClassLoader cl) {
        preconditionCheck();

        contextClassLoader = cl;
    }

    public static boolean holdsLock(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }

        return UtMock.makeSymbolic();
    }

    public StackTraceElement[] getStackTrace() {
        preconditionCheck();

        return UtMock.makeSymbolic();
    }

    public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
        return UtMock.makeSymbolic();
    }

    public long getId() {
        preconditionCheck();

        return tid;
    }

    public Thread.State getState() {
        preconditionCheck();

        return UtMock.makeSymbolic();
    }

    public static void setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler eh) {
        defaultUncaughtExceptionHandler = eh;
    }

    public static Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultUncaughtExceptionHandler;
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        preconditionCheck();

        return uncaughtExceptionHandler != null ? uncaughtExceptionHandler : group;
    }

    public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler eh) {
        preconditionCheck();

        uncaughtExceptionHandler = eh;
    }
}
