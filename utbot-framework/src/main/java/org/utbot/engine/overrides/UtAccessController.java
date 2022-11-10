package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;
import org.utbot.api.mock.UtMock;

import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

@UtClassMock(target = AccessController.class, internalUsage = true)
public class UtAccessController {
    public static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action) {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action,
                                     AccessControlContext context) {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action,
                                     AccessControlContext context, Permission... perms) {
        if (perms == null) {
            throw new NullPointerException();
        }

        for (Permission permission : perms) {
            if (permission == null) {
                throw new NullPointerException();
            }
        }

        return action.run();
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action,
                                                 AccessControlContext context, Permission... perms) {
        return doPrivileged(action, context, perms);
    }

    public static <T> T doPrivileged(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        try {
            return action.run();
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        }
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        return doPrivileged(action);
    }

    public static <T> T doPrivileged(
            PrivilegedExceptionAction<T> action,
            AccessControlContext context
    ) throws PrivilegedActionException {
        return doPrivileged(action);
    }

    public static <T> T doPrivileged(
            PrivilegedExceptionAction<T> action,
            AccessControlContext context,
            Permission... perms
    ) throws PrivilegedActionException {
        if (perms == null) {
            throw new NullPointerException();
        }

        for (Permission permission : perms) {
            if (permission == null) {
                throw new NullPointerException();
            }
        }

        try {
            return action.run();
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        }
    }

    public static <T> T doPrivilegedWithCombiner(
            PrivilegedExceptionAction<T> action,
            AccessControlContext context,
            Permission... perms
    ) throws PrivilegedActionException {
        return doPrivileged(action, context, perms);
    }

    public static void checkPermission(Permission perm) throws AccessControlException {
        if (perm == null) {
            throw new NullPointerException();
        }

        // TODO AccessControlException
    }

}
