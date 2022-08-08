package org.utbot.runtime.utils;

import java.lang.reflect.Method;

final class MockUtils {
    private MockUtils() {}

    // TODO: for now we have only Mockito but it can be changed in the future

    /**
     * This method tries to return the following value: `org.mockito.Mockito.mockingDetails(obj).isMock()`,
     * but via reflection, because we do not know if Mockito library is on the classpath.
     * @param obj an object that we check for being a mock object.
     * @return true if we were able to access `isMock()` method, and it returned true, false otherwise
     */
    public static boolean isMock(Object obj) {
        try {
            Class<?> mockitoClass = Class.forName("org.mockito.Mockito");
            Method mockingDetailsMethod = mockitoClass.getDeclaredMethod("mockingDetails", Object.class);

            Object mockingDetails = mockingDetailsMethod.invoke(null, obj);

            Class<?> mockingDetailsClass = Class.forName("org.mockito.MockingDetails");
            Method isMockMethod = mockingDetailsClass.getDeclaredMethod("isMock");

            return (boolean) isMockMethod.invoke(mockingDetails);
        } catch (Exception e) {
            return false;
        }
    }
}
