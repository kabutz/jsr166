/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @test
 * @bug 8254350
 * @run main SwallowedInterruptedException
 */

// TODO: Rewrite for readability and test execution speed

public class SwallowedInterruptedException {
    static final int MAX_RUNS = 100; // 1000 more likely to repro
    static final AtomicReference<Throwable> fail = new AtomicReference<>();

    public static void main(String[] args) throws Throwable {
        for (int i = 1; i <= MAX_RUNS; i++) {
            long sleepMillis = ThreadLocalRandom.current().nextLong(10);
            final String sleepString = sleepMillis == 0 ? "--------" : String.format("sleep(%d)", sleepMillis);
            final String prefix = String.format("%4d/%d interrupt-%s-complete", i, MAX_RUNS, sleepString);

            CompletableFuture<Void> future = new CompletableFuture<>();

            CountDownLatch waitingFutureLatch = new CountDownLatch(1);

            Thread futureGetThread = new Thread(() -> {
                try {
                    waitingFutureLatch.countDown();
                    future.get();
                    // XXX: Test whether interrupt status was lost.
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.format("%s: future.get completes, Thread.isInterrupted returns true\n", prefix);
                    } else {
                        String msg = String.format("%s: future.get completes, Thread.isInterrupted returns false\n", prefix);
                        fail.set(new AssertionError(msg));
                        return;
                    }
                } catch (InterruptedException interrupted) {
                    System.out.format("%s: future.get is interrupted.\n", prefix);
                    try {
                        future.get();
                    } catch (Throwable ex) {
                        fail.set(ex);
                        return;
                    }
                } catch (Throwable ex) {
                    fail.set(ex);
                    return;
                }
            }, String.format("future-get-thread-%d", i));
            futureGetThread.setDaemon(true);
            futureGetThread.start();

            waitingFutureLatch.await();
            Thread.sleep(1);

            try {
                futureGetThread.interrupt();
                if (sleepMillis > 0) {
                    Thread.sleep(sleepMillis);
                }
                future.complete(null);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            futureGetThread.join();

            if (fail.get() != null) throw fail.get();
        }
    }
}
