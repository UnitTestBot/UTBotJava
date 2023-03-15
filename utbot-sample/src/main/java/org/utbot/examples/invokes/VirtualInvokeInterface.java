package org.utbot.examples.invokes;

public interface VirtualInvokeInterface<T1, T2> {
    int narrowParameterTypeInInheritorObjectCast(T1 object);

    int narrowParameterTypeInInheritorArrayCast(T2 object);
}
