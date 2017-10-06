/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug     6267833
 * @summary Tests for invokeAny, invokeAll
 * @author  Martin Buchholz
 */

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class Invoke {
    static volatile int passed = 0, failed = 0;

    static void fail(String msg) {
        failed++;
        new AssertionError(msg).printStackTrace();
    }

    static void pass() {
        passed++;
    }

    static void unexpected(Throwable t) {
        failed++;
        t.printStackTrace();
    }

    static void check(boolean condition, String msg) {
        if (condition) pass(); else fail(msg);
    }

    static void check(boolean condition) {
        check(condition, "Assertion failure");
    }

    static long secondsElapsedSince(long startTime) {
        return NANOSECONDS.toSeconds(System.nanoTime() - startTime);
    }

    public static void main(String[] args) {
        try {
            testInvokeAll();
            testInvokeAny();
            testInvokeAny_cancellationInterrupt();
        } catch (Throwable t) {  unexpected(t); }

        if (failed > 0)
            throw new Error(
                    String.format("Passed = %d, failed = %d", passed, failed));
    }

    static final long timeoutSeconds = 10L;

    static void testInvokeAll() throws Throwable {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final int nThreads = rnd.nextInt(2, 7);
        final boolean timed = rnd.nextBoolean();
        final ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        final AtomicLong count = new AtomicLong(0);
        class Task implements Callable<Long> {
            public Long call() throws Exception {
                return count.incrementAndGet();
            }
        }

        try {
            final List<Task> tasks = new ArrayList<>();
            for (int n = rnd.nextInt(nThreads); n--> 0; )
                tasks.add(new Task());

            List<Future<Long>> futures;
            long startTime = System.nanoTime();
            if (timed)
                futures = pool.invokeAll(tasks, 2 * timeoutSeconds, SECONDS);
            else
                futures = pool.invokeAll(tasks);
            check(secondsElapsedSince(startTime) < timeoutSeconds);
            check(futures.size() == tasks.size());
            check(count.get() == tasks.size());

            long gauss = 0;
            for (Future<Long> future : futures) gauss += future.get();
            check(gauss == ((tasks.size()+1)*tasks.size())/2);

            pool.shutdown();
            check(pool.awaitTermination(10L, SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    static void testInvokeAny() throws Throwable {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final int nTasks = rnd.nextInt(1, 7);
        final boolean timed = rnd.nextBoolean();
        final ExecutorService pool = Executors.newSingleThreadExecutor();
        final AtomicLong count = new AtomicLong(0);
        class Task implements Callable<Long> {
            public Long call() throws Exception {
                long x = count.incrementAndGet();
                check(x <= 2);
                if (x == 2) {
                    // main thread will interrupt us
                    long startTime = System.nanoTime();
                    try {
                        Thread.sleep(SECONDS.toMillis(2 * timeoutSeconds));
                    } catch (InterruptedException expected) {
                        check(secondsElapsedSince(startTime) < timeoutSeconds);
                    }
                }
                return x;
            }
        }

        try {
            final List<Task> tasks = new ArrayList<>();
            for (int i = nTasks; i--> 0; )
                tasks.add(new Task());

            long startTime = System.nanoTime();
            long val;
            if (timed)
                val = pool.invokeAny(tasks, 2 * timeoutSeconds, SECONDS);
            else
                val = pool.invokeAny(tasks);
            check(val == 1);
            check(secondsElapsedSince(startTime) < timeoutSeconds);

            // inherent race between main thread interrupt and
            // start of second task
            check(count.get() == 1 || count.get() == 2);

            pool.shutdown();
            check(pool.awaitTermination(timeoutSeconds, SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Every remaining running task is sent an interrupt for cancellation.
     */
    static void testInvokeAny_cancellationInterrupt() throws Throwable {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final int nThreads = rnd.nextInt(2, 7);
        final boolean timed = rnd.nextBoolean();
        final ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        final AtomicLong count = new AtomicLong(0);
        final AtomicLong interruptedCount = new AtomicLong(0);
        final CyclicBarrier allStarted = new CyclicBarrier(nThreads);
        class Task implements Callable<Long> {
            public Long call() throws Exception {
                allStarted.await();
                long x = count.incrementAndGet();
                if (x > 1) {
                    // main thread will interrupt us
                    long startTime = System.nanoTime();
                    try {
                        Thread.sleep(SECONDS.toMillis(2 * timeoutSeconds));
                    } catch (InterruptedException expected) {
                        interruptedCount.incrementAndGet();
                        check(secondsElapsedSince(startTime) < timeoutSeconds);
                    }
                }
                return x;
            }
        }

        try {
            final List<Task> tasks = new ArrayList<>();
            for (int i = nThreads; i--> 0; )
                tasks.add(new Task());

            long startTime = System.nanoTime();
            long val;
            if (timed)
                val = pool.invokeAny(tasks, 2 * timeoutSeconds, SECONDS);
            else
                val = pool.invokeAny(tasks);
            check(val == 1);
            check(secondsElapsedSince(startTime) < timeoutSeconds);

            pool.shutdown();
            check(pool.awaitTermination(timeoutSeconds, SECONDS));

            // Check after shutdown to avoid race
            check(count.get() == nThreads);
        } finally {
            pool.shutdownNow();
        }
    }
}
