package org.utbot.examples.mock.service;

import org.utbot.examples.mock.provider.impl.ProviderImpl;

public interface Service {

    int calculateBasedOnShort(ProviderImpl provider);

    int inconsistentBoolean(ProviderImpl provider);

    int calculateBasedOnInteger(ProviderImpl provider);

    int calculateBasedOnBoolean(ProviderImpl provider);

    int calculateBasedOnCharacter(ProviderImpl provider);

    int calculateBasedOnByte(ProviderImpl provider);

    int calculateBasedOnLong(ProviderImpl provider);

    int calculateBasedOnFloat(ProviderImpl provider);

    int calculateBasedOnDouble(ProviderImpl provider);
}
