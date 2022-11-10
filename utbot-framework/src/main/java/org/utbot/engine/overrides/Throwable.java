package org.utbot.engine.overrides;

import org.utbot.api.annotation.*;
import org.utbot.api.mock.*;

@UtClassMock(target = java.lang.Throwable.class, internalUsage = true)
public class Throwable {
    public void printStackTrace() {
        // Do nothing
    }

    public synchronized java.lang.Throwable fillInStackTrace() {
        return UtMock.makeSymbolic();
    }

    public StackTraceElement[] getStackTrace() {
        return UtMock.makeSymbolic();
    }

    public void setStackTrace(StackTraceElement[] stackTrace) {
        // Do nothing
    }

    public final synchronized void addSuppressed(java.lang.Throwable exception) {
        // Do nothing
    }

    public final synchronized java.lang.Throwable[] getSuppressed() {
        return UtMock.makeSymbolic();
    }
}
