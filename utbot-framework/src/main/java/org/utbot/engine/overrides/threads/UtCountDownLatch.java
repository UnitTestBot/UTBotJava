package org.utbot.engine.overrides.threads;

import java.util.concurrent.TimeUnit;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

public class UtCountDownLatch {
    private int count;

    public UtCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException();
        }

        visit(this);
        this.count = count;
    }

    void preconditionCheck() {
        if (alreadyVisited(this)) {
            return;
        }

        assume(count >= 0);

        visit(this);
    }

    public void await() {
        preconditionCheck();
        // Do nothing
    }

    public boolean await(long ignoredTimeout, TimeUnit ignoredUnit) {
        preconditionCheck();

        return count == 0;
    }

    public void countDown() {
        preconditionCheck();

        if (count != 0) {
            count--;
        }
    }

    public long getCount() {
        preconditionCheck();

        return count;
    }

    @Override
    public String toString() {
        preconditionCheck();
        // Actually, the real string representation also contains some meta-information about this class,
        // but it looks redundant for this  wrapper
        return String.valueOf(count);
    }
}
