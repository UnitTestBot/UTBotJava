package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;

import java.security.AccessControlContext;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

@UtClassMock(target = java.security.AccessController.class, internalUsage = true)
public class AccessController {
    public static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedAction<T> action) {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action,
                                     AccessControlContext ignoredContext) {
        return action.run();
    }

    public static <T> T doPrivileged(PrivilegedAction<T> action,
                                     AccessControlContext ignoredContext, Permission... perms) {
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
        } catch (RuntimeException e) {
            // If the action's run method throws an unchecked exception, it will propagate through this method.
            throw e;
        } catch (Exception e) {
            // Exception is wrapped with the PrivilegedActionException ONLY if this exception is CHECKED
            throw new PrivilegedActionException(e);
        }
    }

    public static <T> T doPrivilegedWithCombiner(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        return doPrivileged(action);
    }

    public static <T> T doPrivileged(
            PrivilegedExceptionAction<T> action,
            AccessControlContext ignoredContext
    ) throws PrivilegedActionException {
        return doPrivileged(action);
    }

    public static <T> T doPrivileged(
            PrivilegedExceptionAction<T> action,
            AccessControlContext ignoredContext,
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

        return doPrivileged(action);
    }

    public static <T> T doPrivilegedWithCombiner(
            PrivilegedExceptionAction<T> action,
            AccessControlContext context,
            Permission... perms
    ) throws PrivilegedActionException {
        return doPrivileged(action, context, perms);
    }

    public static void checkPermission(Permission perm) {
        if (perm == null) {
            throw new NullPointerException();
        }

        // We cannot check real permission do determines whether we should throw an AccessControlException,
        // so do nothing more.
    }
}
