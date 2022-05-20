package org.utbot.instrumentation.samples.mock;

public interface IProvider {
    default int provideIntDefault() {
        return 0;
    }

    int provideInt();

    default String provideString() {
        return "default";
    }
}
