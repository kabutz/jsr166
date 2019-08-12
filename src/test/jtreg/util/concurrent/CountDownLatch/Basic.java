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
 * @bug 6332435 8221168
 * @summary Basic tests for CountDownLatch
 * @library /lib/testlibrary/
 * @author Seetharam Avadhanam, Martin Buchholz
 */

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import jdk.testlibrary.Utils;

public class Basic {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    abstract static class Awaiter extends Thread {
        volatile Throwable exception;
        protected void setException(Throwable exception) {
            this.exception = exception;
        }
    }

    static Awaiter awaiter(CountDownLatch latch,
                           CountDownLatch gate) {
        return new Awaiter() { public void run() {
            gate.countDown();
            try {
                latch.await();
            } catch (Throwable ex) { setException(ex); }}};
    }

    static Awaiter awaiter(CountDownLatch latch,
                           CountDownLatch gate,
                           long millis) {
        return new Awaiter() { public void run() {
            gate.countDown();
            try {
                latch.await(millis, TimeUnit.MILLISECONDS);
            } catch (Throwable ex) { setException(ex); }}};
    }

    static Supplier<Awaiter> awaiterSupplier(
            CountDownLatch latch, CountDownLatch gate) {
        return () -> awaiter(latch, gate);
    }

    static Supplier<Awaiter> timedAwaiterSupplier(
            CountDownLatch latch, CountDownLatch gate) {
        return () -> awaiter(latch, gate, LONG_DELAY_MS);
    }

    //----------------------------------------------------------------
    // Normal use
    //----------------------------------------------------------------
    public static void normalUse() throws Throwable {
        int count = 0;
        CountDownLatch latch = new CountDownLatch(3);
        Awaiter[] a = new Awaiter[12];

        for (int i = 0; i < 3; i++) {
            CountDownLatch gate = new CountDownLatch(4);
            Supplier<Awaiter> s1 = awaiterSupplier(latch, gate);
            Supplier<Awaiter> s2 = timedAwaiterSupplier(latch, gate);
            a[count] = s1.get(); a[count++].start();
            a[count] = s1.get(); a[count++].start();
            a[count] = s2.get(); a[count++].start();
            a[count] = s2.get(); a[count++].start();
            gate.await();
            latch.countDown();
            checkCount(latch, 2-i);
        }
        for (Awaiter awaiter : a)
            awaiter.join();
        for (Awaiter awaiter : a)
            checkException(awaiter, null);
    }

    //----------------------------------------------------------------
    // One thread interrupted
    //----------------------------------------------------------------
    public static void threadInterrupted() throws Throwable {
        int count = 0;
        CountDownLatch latch = new CountDownLatch(3);
        Awaiter[] a = new Awaiter[12];

        for (int i = 0; i < 3; i++) {
            CountDownLatch gate = new CountDownLatch(4);
            Supplier<Awaiter> s1 = awaiterSupplier(latch, gate);
            Supplier<Awaiter> s2 = timedAwaiterSupplier(latch, gate);
            a[count] = s1.get(); a[count++].start();
            a[count] = s1.get(); a[count++].start();
            a[count] = s2.get(); a[count++].start();
            a[count] = s2.get(); a[count++].start();
            a[count-1].interrupt();
            gate.await();
            latch.countDown();
            checkCount(latch, 2-i);
        }
        for (Awaiter awaiter : a)
            awaiter.join();
        for (int i = 0; i < a.length; i++) {
            Awaiter awaiter = a[i];
            Throwable ex = awaiter.exception;
            if ((i % 4) == 3 && !awaiter.isInterrupted())
                checkException(awaiter, InterruptedException.class);
            else
                checkException(awaiter, null);
        }
    }

    //----------------------------------------------------------------
    // One thread timed out
    //----------------------------------------------------------------
    public static void timeOut() throws Throwable {
        int count = 0;
        CountDownLatch latch = new CountDownLatch(3);
        Awaiter[] a = new Awaiter[12];

        long[] timeout = { 0L, 5L, 10L };

        for (int i = 0; i < 3; i++) {
            CountDownLatch gate = new CountDownLatch(4);
            Supplier<Awaiter> s1 = awaiterSupplier(latch, gate);
            Supplier<Awaiter> s2 = timedAwaiterSupplier(latch, gate);
            a[count] = awaiter(latch, gate, timeout[i]); a[count++].start();
            a[count] = s1.get(); a[count++].start();
            a[count] = s2.get(); a[count++].start();
            a[count] = s2.get(); a[count++].start();
            gate.await();
            latch.countDown();
            checkCount(latch, 2-i);
        }
        for (Awaiter awaiter : a)
            awaiter.join();
        for (Awaiter awaiter : a)
            checkException(awaiter, null);
    }

    public static void main(String[] args) throws Throwable {
        try {
            normalUse();
        } catch (Throwable ex) { fail(ex); }
        try {
            threadInterrupted();
        } catch (Throwable ex) { fail(ex); }
        try {
            timeOut();
        } catch (Throwable ex) { fail(ex); }

        if (failures.get() > 0L)
            throw new AssertionError(failures.get() + " failures");
    }

    static final AtomicInteger failures = new AtomicInteger(0);

    static void fail(String msg) {
        fail(new AssertionError(msg));
    }

    static void fail(Throwable t) {
        t.printStackTrace();
        failures.getAndIncrement();
    }

    static void checkCount(CountDownLatch b, int expected) {
        if (b.getCount() != expected)
            fail("Count = " + b.getCount() +
                 ", expected = " + expected);
    }

    static void checkException(Awaiter awaiter, Class<? extends Throwable> c) {
        Throwable ex = awaiter.exception;
        if (! ((ex == null && c == null) || c.isInstance(ex)))
            fail("Expected: " + c + ", got: " + ex);
    }
}
