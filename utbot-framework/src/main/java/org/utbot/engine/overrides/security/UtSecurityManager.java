package org.utbot.engine.overrides.security;

import org.utbot.api.mock.UtMock;

import java.security.Permission;

/**
 * Overridden implementation for [java.lang.SecurityManager] class
 */
public class UtSecurityManager {
    public void checkPermission(Permission perm) {
        // Do nothing to allow everything
    }

    public void checkPackageAccess(String pkg) {
        // Do nothing to allow everything
    }

    public ThreadGroup getThreadGroup() {
        return new ThreadGroup(UtMock.makeSymbolic());
    }
}
