package org.utbot.engine.overrides.threads;

import org.utbot.api.mock.UtMock;

import java.util.Arrays;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

@SuppressWarnings("unused")
public class UtThreadGroup implements Thread.UncaughtExceptionHandler {
    private final ThreadGroup parent;
    String name;
    int maxPriority;
    boolean destroyed;
    boolean daemon;

    int nUnstartedThreads = 0;
    int nthreads;
    Thread[] threads;

    int ngroups;
    ThreadGroup[] groups;

    public UtThreadGroup(String name) {
        this(UtThread.currentThread().getThreadGroup(), name);
    }

    public UtThreadGroup(ThreadGroup parent, String name) {
        visit(this);

        this.name = name;

        final Integer maxPriority = UtMock.makeSymbolic();
        assume(maxPriority >= UtThread.MIN_PRIORITY);
        assume(maxPriority <= UtThread.MAX_PRIORITY);
        this.maxPriority = maxPriority;

        this.daemon = UtMock.makeSymbolic();
        this.parent = parent;
    }

    public void preconditionCheck() {
        if (alreadyVisited(this)) {
            return;
        }

        assume(parent != null);
        assume(name != null);

        assume(maxPriority >= UtThread.MIN_PRIORITY);
        assume(maxPriority <= UtThread.MAX_PRIORITY);

        assume(nUnstartedThreads >= 0);
        assume(ngroups >= 0);

        visit(this);
    }

    public final String getName() {
        preconditionCheck();

        return name;
    }

    public final ThreadGroup getParent() {
        preconditionCheck();

        return parent;
    }

    public final int getMaxPriority() {
        preconditionCheck();

        return maxPriority;
    }

    public final boolean isDaemon() {
        preconditionCheck();

        return daemon;
    }

    public boolean isDestroyed() {
        preconditionCheck();

        return destroyed;
    }

    public final void setDaemon(boolean daemon) {
        preconditionCheck();

        this.daemon = daemon;
    }

    public final void setMaxPriority(int pri) {
        preconditionCheck();

        if (pri < UtThread.MIN_PRIORITY || pri > UtThread.MAX_PRIORITY) {
            return;
        }

        for (int i = 0; i < ngroups; i++) {
            groups[i].setMaxPriority(pri);
        }
    }

    public final boolean parentOf(ThreadGroup g) {
        preconditionCheck();

        return UtMock.makeSymbolic();
    }

    public final void checkAccess() {
        preconditionCheck();
        // Do nothing
    }

    public int activeCount() {
        preconditionCheck();

        if (destroyed) {
            return 0;
        }

        final Integer result = UtMock.makeSymbolic();
        assume(result >= 0);
        return result;
    }

    public int enumerate(Thread[] list) {
        return enumerate(list, 0, true);
    }

    public int enumerate(Thread[] list, boolean recurse) {
        return enumerate(list, 0, recurse);
    }

    @SuppressWarnings({"SameParameterValue", "ParameterCanBeLocal"})
    private int enumerate(Thread[] list, int ignoredN, boolean ignoredRecurse) {
        preconditionCheck();

        if (destroyed) {
            return 0;
        }

        list = UtMock.makeSymbolic();

        final Integer result = UtMock.makeSymbolic();
        assume(result <= list.length);
        assume(result >= 0);

        return result;
    }

    public int activeGroupCount() {
        preconditionCheck();

        if (destroyed) {
            return 0;
        }

        final Integer result = UtMock.makeSymbolic();
        assume(result >= 0);
        return result;
    }

    public int enumerate(ThreadGroup[] list) {
        return enumerate(list, 0, true);
    }

    public int enumerate(ThreadGroup[] list, boolean recurse) {
        return enumerate(list, 0, recurse);
    }

    @SuppressWarnings({"SameParameterValue", "ParameterCanBeLocal"})
    private int enumerate(ThreadGroup[] list, int ignoredN, boolean ignoredRecurse) {
        preconditionCheck();

        if (destroyed) {
            return 0;
        }

        list = UtMock.makeSymbolic();

        final Integer result = UtMock.makeSymbolic();
        assume(result <= list.length);
        assume(result >= 0);

        return result;
    }

    public final void stop() {
        preconditionCheck();
        // Do nothing
    }

    public final void interrupt() {
        preconditionCheck();

        for (int i = 0; i < nthreads; i++) {
            threads[i].interrupt();
        }

        for (int i = 0; i < ngroups; i++) {
            groups[i].interrupt();
        }
    }

    public final void suspend() {
        preconditionCheck();
        // Do nothing
    }

    public final void resume() {
        preconditionCheck();
        // Do nothing
    }

    public final void destroy() {
        preconditionCheck();

        if (destroyed || nthreads > 0) {
            throw new IllegalThreadStateException();
        }

        if (parent != null) {
            destroyed = true;
            ngroups = 0;
            groups = null;
            nthreads = 0;
            threads = null;
        }
        for (int i = 0; i < ngroups; i += 1) {
            groups[i].destroy();
        }
    }

    private void add(ThreadGroup g) {
        preconditionCheck();

        if (destroyed) {
            throw new IllegalThreadStateException();
        }

        if (groups == null) {
            groups = new ThreadGroup[4];
        } else if (ngroups == groups.length) {
            groups = Arrays.copyOf(groups, ngroups * 2);
        }
        groups[ngroups] = g;

        ngroups++;
    }

    private void remove(ThreadGroup g) {
        preconditionCheck();

        if (destroyed) {
            return;
        }

        for (int i = 0; i < ngroups; i++) {
            if (groups[i] == g) {
                ngroups -= 1;
                System.arraycopy(groups, i + 1, groups, i, ngroups - i);
                groups[ngroups] = null;
                break;
            }
        }

        if (daemon && nthreads == 0 && nUnstartedThreads == 0 && ngroups == 0) {
            destroy();
        }
    }

    void addUnstarted() {
        preconditionCheck();

        if (destroyed) {
            throw new IllegalThreadStateException();
        }

        nUnstartedThreads++;
    }

    void add(Thread t) {
        preconditionCheck();

        if (destroyed) {
            throw new IllegalThreadStateException();
        }

        if (threads == null) {
            threads = new Thread[4];
        } else if (nthreads == threads.length) {
            threads = Arrays.copyOf(threads, nthreads * 2);
        }
        threads[nthreads] = t;

        nthreads++;
        nUnstartedThreads--;
    }

    public void list() {
        preconditionCheck();
        // Do nothing
    }

    public void uncaughtException(Thread t, Throwable e) {
        preconditionCheck();
        // Do nothing
    }

    public boolean allowThreadSuspension(boolean b) {
        preconditionCheck();

        return true;
    }

    public String toString() {
        preconditionCheck();

        return getClass().getName() + "[name=" + getName() + ",maxpri=" + maxPriority + "]";
    }
}
