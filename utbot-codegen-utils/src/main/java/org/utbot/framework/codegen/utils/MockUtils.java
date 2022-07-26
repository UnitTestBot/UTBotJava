package org.utbot.framework.codegen.utils;

final class MockUtils {
    private MockUtils() {}

    // TODO: for now we have only Mockito but it can be changed in the future
    public static boolean isMock(Object obj) {
        return org.mockito.Mockito.mockingDetails(obj).isMock();
    }
}
