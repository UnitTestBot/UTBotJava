package org.utbot.engine.overrides.threads;

import org.utbot.api.mock.UtMock;

import java.util.Arrays;

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
        this.name = name;
        this.maxPriority = UtMock.makeSymbolic();
        this.daemon = UtMock.makeSymbolic();
        this.parent = parent;
    }

    public final String getName() {
        return name;
    }

    public final ThreadGroup getParent() {
        return parent;
    }

    public final int getMaxPriority() {
        return maxPriority;
    }

    public final boolean isDaemon() {
        return daemon;
    }

    public synchronized boolean isDestroyed() {
        return destroyed;
    }

    public final void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public final void setMaxPriority(int pri) {
        if (pri < UtThread.MIN_PRIORITY || pri > UtThread.MAX_PRIORITY) {
            return;
        }

        for (int i = 0 ; i < ngroups ; i++) {
            groups[i].setMaxPriority(pri);
        }
    }

    public final boolean parentOf(ThreadGroup g) {
        return UtMock.makeSymbolic();
    }

    public final void checkAccess() {
        // Do nothing
    }

    public int activeCount() {
        if (destroyed) {
            return 0;
        }

        final Integer result = UtMock.makeSymbolic();
        UtMock.assume(result >= 0);
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
        if (destroyed) {
            return 0;
        }

        list = UtMock.makeSymbolic();

        final Integer result = UtMock.makeSymbolic();
        UtMock.assume(result <= list.length);
        UtMock.assume(result >= 0);

        return result;
    }

    public int activeGroupCount() {
        if (destroyed) {
            return 0;
        }

        final Integer result = UtMock.makeSymbolic();
        UtMock.assume(result >= 0);
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
        if (destroyed) {
            return 0;
        }

        list = UtMock.makeSymbolic();

        final Integer result = UtMock.makeSymbolic();
        UtMock.assume(result <= list.length);
        UtMock.assume(result >= 0);

        return result;
    }

    public final void stop() {
        // Do nothing
    }

    public final void interrupt() {
        for (int i = 0 ; i < nthreads ; i++) {
            threads[i].interrupt();
        }

        for (int i = 0 ; i < ngroups ; i++) {
            groups[i].interrupt();
        }
    }

    public final void suspend() {
        // Do nothing
    }

    public final void resume() {
        // Do nothing
    }

    public final void destroy() {
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
        for (int i = 0 ; i < ngroups ; i += 1) {
            groups[i].destroy();
        }
    }

    private void add(ThreadGroup g){
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
        if (destroyed) {
            return;
        }

        for (int i = 0 ; i < ngroups ; i++) {
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
        if (destroyed) {
            throw new IllegalThreadStateException();
        }

        nUnstartedThreads++;
    }

    void add(Thread t) {
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
        // Do nothing
    }

    public void uncaughtException(Thread t, Throwable e) {
        // Do nothing
    }

    public boolean allowThreadSuspension(boolean b) {
        return true;
    }

    public String toString() {
        return getClass().getName() + "[name=" + getName() + ",maxpri=" + maxPriority + "]";
    }
}
