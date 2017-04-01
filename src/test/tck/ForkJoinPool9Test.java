/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ForkJoinPool9Test extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(ForkJoinPool9Test.class);
    }

    /**
     * Check handling of common pool thread context class loader
     */
    public void testCommonPoolThreadContextClassLoader() throws Throwable {
        if (!testImplementationDetails) return;

        // Ensure common pool has at least one real thread
        String prop = System.getProperty(
            "java.util.concurrent.ForkJoinPool.common.parallelism");
        if ("0".equals(prop)) return;

        VarHandle CCL =
            MethodHandles.privateLookupIn(Thread.class, MethodHandles.lookup())
            .findVarHandle(Thread.class, "contextClassLoader", ClassLoader.class);
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        boolean haveSecurityManager = (System.getSecurityManager() != null);
        CountDownLatch taskStarted = new CountDownLatch(1);
        Runnable runInCommonPool = () -> {
            taskStarted.countDown();
            assertTrue(ForkJoinTask.inForkJoinPool());
            assertSame(ForkJoinPool.commonPool(),
                       ForkJoinTask.getPool());
            assertSame(systemClassLoader,
                       Thread.currentThread().getContextClassLoader());
            assertSame(systemClassLoader,
                       CCL.get(Thread.currentThread()));
            if (haveSecurityManager)
                assertThrows(
                    SecurityException.class,
                    () -> System.getProperty("foo"),
                    () -> Thread.currentThread().setContextClassLoader(null));
            // TODO ?
//          if (haveSecurityManager
//              && Thread.currentThread().getClass().getSimpleName()
//                 .equals("InnocuousForkJoinWorkerThread"))
//              assertThrows(SecurityException.class, /* ?? */);
        };
        Future<?> f = ForkJoinPool.commonPool().submit(runInCommonPool);
        // Ensure runInCommonPool is truly running in the common pool,
        // by giving this thread no opportunity to "help" on get().
        await(taskStarted);
        assertNull(f.get());
    }

}
