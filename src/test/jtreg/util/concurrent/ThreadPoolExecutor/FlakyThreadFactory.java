/*
 * Written by Martin Buchholz and Doug Lea with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @summary Should be able to shutdown a pool when worker creation failed.
 * @library /lib/testlibrary/
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jdk.testlibrary.Utils;

public class FlakyThreadFactory {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    void test(String[] args) throws Throwable {
        test(NullPointerException.class,
             new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    throw new NullPointerException();
                }});
        test(OutOfMemoryError.class,
             new ThreadFactory() {
                 @SuppressWarnings("DeadThread")
                 public Thread newThread(Runnable r) {
                     // We expect this to throw OOME, but ...
                     new Thread(null, r, "a natural OOME", 1L << 60);
                     // """On some platforms, the value of the stackSize
                     // parameter may have no effect whatsoever."""
                     throw new OutOfMemoryError("artificial OOME");
                 }});
        test(null,
             new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    return null;
                }});
    }

    void test(final Class<?> exceptionClass,
              final ThreadFactory failingThreadFactory)
            throws Throwable {
        ThreadFactory flakyThreadFactory = new ThreadFactory() {
            int seq = 0;
            public Thread newThread(Runnable r) {
                if (seq++ < 4)
                    return new Thread(r);
                else
                    return failingThreadFactory.newThread(r);
            }};
        ThreadPoolExecutor pool =
            new ThreadPoolExecutor(10, 10,
                                   0L, TimeUnit.SECONDS,
                                   new LinkedBlockingQueue(),
                                   flakyThreadFactory);
        try {
            for (int i = 0; i < 8; i++)
                pool.submit(new Runnable() { public void run() {} });
            check(exceptionClass == null);
        } catch (Throwable t) {
            /* t.printStackTrace(); */
            check(exceptionClass.isInstance(t));
        }
        pool.shutdown();
        check(pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new FlakyThreadFactory().instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
