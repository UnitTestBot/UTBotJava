package org.utbot.examples.mock.others;

/**
 * Simple interface, used in scenarios:
 * - mock classes/interfaces from another package;
 * - mock fields, static or non-static.
 */
public interface Generator {
    int generateInt();
}