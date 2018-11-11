/*
 * Written by Martin Buchholz with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package jsr166;

import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.invoke.MethodType.methodType;

/**
 * A portability layer to allow running on a variety of JDKs.
 *
 * SharedSecrets was moved to a different package in jdk12.
 *
 * This package should never be integrated into openjdk.
 */
public class Platform {
    /**
     * See JavaObjectInputStreamAccess#checkArray.
     * @param ois ...
     * @param arrayType ...
     * @param arrayLength ...
     * @throws InvalidClassException ...
     */
    public static void checkArray(
            ObjectInputStream ois, Class<?> arrayType, int arrayLength)
        throws InvalidClassException {
        try {
            checkArray.invoke(theJavaObjectInputStreamAccess,
                              ois, arrayType, arrayLength);
        }
        catch (InvalidClassException | RuntimeException x) { throw x; }
        catch (Throwable x) { throw new Error(x); }
    }

    private static final Object theJavaObjectInputStreamAccess;
    private static final MethodHandle checkArray;
    static {
        try {
            Class<?> sharedSecrets, access;
            try {
                sharedSecrets = Class.forName("jdk.internal.misc.SharedSecrets");
                access = Class.forName("jdk.internal.misc.JavaObjectInputStreamAccess");
            } catch (ClassNotFoundException retry) {
                sharedSecrets = Class.forName("jdk.internal.access.SharedSecrets");
                access = Class.forName("jdk.internal.access.JavaObjectInputStreamAccess");
            }
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            theJavaObjectInputStreamAccess = lookup.findStatic(
                    sharedSecrets, "getJavaObjectInputStreamAccess",
                    methodType(access))
                .invoke();
            checkArray = lookup.findVirtual(
                    access, "checkArray",
                    methodType(void.class, ObjectInputStream.class, Class.class, int.class));
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
