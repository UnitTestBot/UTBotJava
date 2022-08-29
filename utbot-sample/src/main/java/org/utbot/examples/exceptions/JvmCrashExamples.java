package org.utbot.examples.exceptions;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import sun.misc.Unsafe;

public class JvmCrashExamples {
    public int exit(int i) {
        if (i == 0) {
            System.exit(-1);
            throw new RuntimeException("Exit");
        }

        return i;
    }

    public int crash(int i) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
        unsafe.putAddress(0, 0);

        if (i == 0) {
            return i;
        }

        return 1;
    }

    // this method crashes JVM and uses [AccessController.doPrivileged] to obtain privileges
    public int crashPrivileged(int i) {
        return AccessController.doPrivileged((PrivilegedAction<Integer>) () -> {
            Field f;
            try {
                f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                Unsafe unsafe = (Unsafe) f.get(null);
                unsafe.putAddress(0, 0);

                if (i == 0) {
                    return i;
                }

                return 1;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
