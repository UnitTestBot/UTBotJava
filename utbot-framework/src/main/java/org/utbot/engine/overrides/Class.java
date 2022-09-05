package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;

@UtClassMock(target = java.lang.Class.class, internalUsage = true)
public class Class {
    public static boolean desiredAssertionStatus() {
        return true;
    }

    private void checkMemberAccess(SecurityManager sm, int which,
                                   java.lang.Class<?> caller, boolean checkProxyInterfaces) {
        // Do nothing to allow everything
    }

    private void checkPackageAccess(SecurityManager sm, final ClassLoader ccl,
                                    boolean checkProxyInterfaces) {
        // Do nothing to allow everything
    }
}
