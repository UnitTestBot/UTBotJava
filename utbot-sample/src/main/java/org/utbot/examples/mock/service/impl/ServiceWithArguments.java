package org.utbot.examples.mock.service.impl;

import org.utbot.examples.mock.provider.impl.ProviderImpl;

public class ServiceWithArguments {

    public int callMultipleMethods(ProviderImpl provider) {
        if (provider.provideInteger() < provider.provideLong()) {
            return 1;
        } else {
            return 0;
        }
    }

    public int calculateBasedOnIntArgument(ProviderImpl provider, int i) {
        if (provider.provideGiven(i) < provider.provideGiven(i + 1)) {
            return 1;
        } else {
            return 0;
        }
    }

    public int calculateBasedOnBoolean(ProviderImpl provider) {
        return provider.provideBoolean() ? 1 : 0;
    }

    public int inconsistentBoolean(ProviderImpl provider) {
        if (provider.provideBoolean() && !provider.provideBoolean()) {
            return 1;
        } else {
            return 0;
        }
    }

    public int calculateBasedOnCharacter(ProviderImpl provider) {
        final char ret = provider.provideCharacter();
        return ret > 'a' ? 1 : 0;
    }

    public int calculateBasedOnByte(ProviderImpl provider) {
        final byte ret = provider.provideByte();
        return ret > 5 ? 1 : 0;
    }

    public int calculateBasedOnShort(ProviderImpl provider) {
        final short ret = provider.provideShort();
        return ret > 5 ? 1 : 0;
    }

    public int calculateBasedOnInteger(ProviderImpl provider) {
        final int ret = provider.provideInteger();
        return ret > 5 ? 1 : 0;
    }

    public int calculateBasedOnLong(ProviderImpl provider) {
        final long ret = provider.provideLong();
        return ret > 5 ? 1 : 0;
    }

    public int calculateBasedOnFloat(ProviderImpl provider) {
        final float ret = provider.provideFloat();
        return ret > 1f ? 1 : 0;
    }

    public int calculateBasedOnDouble(ProviderImpl provider) {
        final double ret = provider.provideDouble();
        return ret > 1d ? 1 : 0;
    }

    public int calculateBasedOnObject(ProviderImpl provider) {
        ExampleClass object = provider.provideObject();
        if (object.field == 0) {
            object.field = 1;
        } else {
            object.field = 0;
        }
        return object.field;
    }

    public int calculateBasedOnOverloadedMethods(ProviderImpl provider, int i) {
        int a = provider.provideOverloaded();
        int b = provider.provideOverloaded(i);
        if (a < b) {
            return 1;
        } else {
            return 0;
        }
    }

    public int calculateBasedOnObjectArgument(ProviderImpl provider, ExampleClass object) {
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
