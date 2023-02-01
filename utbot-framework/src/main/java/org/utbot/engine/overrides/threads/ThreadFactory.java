package org.utbot.engine.overrides.threads;

import org.jetbrains.annotations.NotNull;
import org.utbot.api.annotation.UtClassMock;

@UtClassMock(target = ThreadFactory.class, internalUsage = true)
public class ThreadFactory implements java.util.concurrent.ThreadFactory {
    @Override
    public Thread newThread(@NotNull Runnable r) {
        return new Thread(r);
    }
}
