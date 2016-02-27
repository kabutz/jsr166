/*
 * Written by Martin Buchholz and Jason Mehrens with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @summary Only one thread should be created when a thread needs to
 * be kept alive to service a delayed task waiting in the queue.
 * @library /lib/testlibrary/
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import jdk.testlibrary.Utils;

public class ThreadRestarts {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    public static void main(String[] args) throws Exception {
        test(false);
        test(true);
    }

    private static void test(boolean allowTimeout) throws Exception {
        CountingThreadFactory ctf = new CountingThreadFactory();
        ScheduledThreadPoolExecutor stpe
            = new ScheduledThreadPoolExecutor(10, ctf);
        try {
            Runnable nop = new Runnable() { public void run() {}};
            stpe.schedule(nop, 10*1000L, MILLISECONDS);
            stpe.setKeepAliveTime(1L, MILLISECONDS);
            stpe.allowCoreThreadTimeOut(allowTimeout);
            MILLISECONDS.sleep(12L);
        } finally {
            stpe.shutdownNow();
            if (!stpe.awaitTermination(LONG_DELAY_MS, MILLISECONDS))
                throw new AssertionError("timed out");
        }
        if (ctf.count.get() > 1)
            throw new AssertionError(
                String.format("%d threads created, 1 expected",
                              ctf.count.get()));
    }

    static class CountingThreadFactory implements ThreadFactory {
        final AtomicLong count = new AtomicLong(0L);

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            count.getAndIncrement();
            return t;
        }
    }
}
