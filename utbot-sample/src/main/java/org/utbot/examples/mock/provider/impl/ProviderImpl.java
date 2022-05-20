package org.utbot.examples.mock.provider.impl;


import org.utbot.examples.mock.provider.Provider;
import org.utbot.examples.mock.service.impl.ExampleClass;

public class ProviderImpl implements Provider {

    @Override
    public ExampleClass provideObject() {
        return new ExampleClass(0);
    }

    @Override
    public boolean provideBoolean() {
        return true;
    }

    @Override
    public char provideCharacter() {
        return 'a';
    }

    @Override
    public byte provideByte() {
        return 1;
    }

    @Override
    public short provideShort() {
        return 1;
    }

    @Override
    public int provideInteger() {
        return 1;
    }

    @Override
    public long provideLong() {
        return 1;
    }

    @Override
    public float provideFloat() {
        return 1f;
    }

    @Override
    public double provideDouble() {
        return 1d;
    }

    @Override
    public int provideGiven(int i) {
        return i;
    }

    @Override
    public int provideOverloaded() {
        return 0;
    }

    @Override
    public int provideOverloaded(int i) {
        return 0;
    }

    @Override
    public int provideGivenObject(ExampleClass object) {
        return object.field;
    }
}
