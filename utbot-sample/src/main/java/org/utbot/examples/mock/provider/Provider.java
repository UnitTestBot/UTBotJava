package org.utbot.examples.mock.provider;

import org.utbot.examples.mock.service.impl.ExampleClass;

public interface Provider {

    ExampleClass provideObject();

    boolean provideBoolean();

    char provideCharacter();

    byte provideByte();

    short provideShort();

    int provideInteger();

    long provideLong();

    float provideFloat();

    double provideDouble();

    int provideGiven(int i);

    int provideOverloaded();

    int provideOverloaded(int i);

    int provideGivenObject(ExampleClass object);
}
