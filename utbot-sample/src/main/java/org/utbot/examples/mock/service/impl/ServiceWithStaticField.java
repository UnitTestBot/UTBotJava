package org.utbot.examples.mock.service.impl;

import org.utbot.examples.mock.provider.impl.ProviderImpl;

public class ServiceWithStaticField {

    static ProviderImpl provider;

    public int callMultipleMethods() {
        if (provider.provideInteger() < provider.provideLong()) {
            return 1;
        } else {
            return 0;
        }
    }

    public int calculateBasedOnIntArgument(int i) {
        if (provider.provideGiven(i) < provider.provideGiven(i + 1)) {
            return 1;
        } else {
            return 0;
        }
    }

    public int calculateBasedOnBoolean() {
        return provider.provideBoolean() ? 1 : 0;
    }

    public int inconsistentBoolean() {
        if (provider.provideBoolean() && !provider.provideBoolean()) {
            return 1;
        } else {
            return 0;
        }
    }

    public int calculateBasedOnCharacter() {
        final char ret = provider.provideCharacter();
        return ret > 'a' ? 1 : 0;
    }

    public int calculateBasedOnByte() {
        final byte ret = provider.provideByte();
        return ret > 5 ? 1 : 0;
    }

    public int calculateBasedOnShort() {
        final short ret = provider.provideShort();
        return ret > 5 ? 1 : 0;
    }

    public int calculateBasedOnInteger() {
        final int ret = provider.provideInteger();
        return ret > 5 ? 1 : 0;
    }

    public int calculateBasedOnLong() {
        final long ret = provider.provideLong();
        return ret > 5 ? 1 : 0;
    }

    public int calculateBasedOnFloat() {
        final float ret = provider.provideFloat();
        return ret > 1f ? 1 : 0;
    }

    public int calculateBasedOnDouble() {
        final double ret = provider.provideDouble();
        return ret > 1d ? 1 : 0;
    }

    public int calculateBasedOnObject() {
        ExampleClass object = provider.provideObject();
        if (object.field == 0) {
            object.field = 1;
        } else {
            object.field = 0;
        }
        return object.field;
    }

    public int calculateBasedOnOverloadedMethods(int i) {
        int a = provider.provideOverloaded();
        int b = provider.provideOverloaded(i);
        if (a < b) {
            return 1;
        } else {
            return 0;
        }
    }

    public int calculateBasedOnObjectArgument(ExampleClass object) {
        if (object != null) {
            object.field--;
        }
        int a = provider.provideGivenObject(object);
        if (a < 1) {
            return 1;
        } else {
            return 0;
        }
    }
}
