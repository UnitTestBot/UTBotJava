package org.utbot.instrumentation.samples.mock

class ClassForMockInterface : IProvider {
    override fun provideInt(): Int {
        return -1
    }

    override fun provideIntDefault(): Int {
        return -2
    }
}