package org.utbot.examples.mock.others;

/**
 * Simple interface, used in scenarios:
 * - mock classes/interfaces from another package;
 * - mock object returned by another mock;
 * - mock fields, static or non-static.
 */
public interface Locator {
    Generator locate();
}