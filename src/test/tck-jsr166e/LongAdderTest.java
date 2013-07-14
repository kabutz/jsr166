/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import jsr166e.*;
import junit.framework.*;
import java.util.concurrent.*;

public class LongAdderTest extends JSR166TestCase {
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    public static Test suite() {
        return new TestSuite(LongAdderTest.class);
    }

    /**
     * default constructed initializes to zero
     */
    public void testConstructor() {
        LongAdder ai = new LongAdder();
        assertEquals(0, ai.sum());
    }

    /**
     * add adds given value to current, and sum returns current value
     */
    public void testAddAndSum() {
        LongAdder ai = new LongAdder();
        ai.add(2);
        assertEquals(2, ai.sum());
        ai.add(-4);
        assertEquals(-2, ai.sum());
    }

    /**
     * decrement decrements and sum returns current value
     */
    public void testDecrementAndsum() {
        LongAdder ai = new LongAdder();
        ai.decrement();
        assertEquals(-1, ai.sum());
        ai.decrement();
        assertEquals(-2, ai.sum());
    }

    /**
     * incrementAndGet increments and returns current value
     */
    public void testIncrementAndsum() {
        LongAdder ai = new LongAdder();
        ai.increment();
        assertEquals(1, ai.sum());
        ai.increment();
        assertEquals(2, ai.sum());
    }

    /**
     * reset zeroes sum
     */
    public void testReset() {
        LongAdder ai = new LongAdder();
        ai.add(2);
        assertEquals(2, ai.sum());
        ai.reset();
        assertEquals(0, ai.sum());
    }

    /**
     * sumThenReset returns sum then zeros
     */
    public void testSumThenReset() {
        LongAdder ai = new LongAdder();
        ai.add(2);
        assertEquals(2, ai.sum());
        assertEquals(2, ai.sumThenReset());
        assertEquals(0, ai.sum());
    }

    /**
     * a deserialized serialized adder holds same value
     */
    public void testSerialization() throws Exception {
        LongAdder x = new LongAdder();
        LongAdder y = serialClone(x);
        assertNotSame(x, y);
        x.add(-22);
        LongAdder z = serialClone(x);
        assertNotSame(y, z);
        assertEquals(-22, x.sum());
        assertEquals(0, y.sum());
        assertEquals(-22, z.sum());
    }

    /**
     * toString returns current value.
     */
    public void testToString() {
        LongAdder ai = new LongAdder();
        assertEquals("0", ai.toString());
        ai.increment();
        assertEquals(Long.toString(1), ai.toString());
    }

    /**
     * intValue returns current value.
     */
    public void testIntValue() {
        LongAdder ai = new LongAdder();
        assertEquals(0, ai.intValue());
        ai.increment();
        assertEquals(1, ai.intValue());
    }

    /**
     * longValue returns current value.
     */
    public void testLongValue() {
        LongAdder ai = new LongAdder();
        assertEquals(0, ai.longValue());
        ai.increment();
        assertEquals(1, ai.longValue());
    }

    /**
     * floatValue returns current value.
     */
    public void testFloatValue() {
        LongAdder ai = new LongAdder();
        assertEquals(0.0f, ai.floatValue());
        ai.increment();
        assertEquals(1.0f, ai.floatValue());
    }

    /**
     * doubleValue returns current value.
     */
    public void testDoubleValue() {
        LongAdder ai = new LongAdder();
        assertEquals(0.0, ai.doubleValue());
        ai.increment();
        assertEquals(1.0, ai.doubleValue());
    }

    /**
     * adds by multiple threads produce correct sum
     */
    public void testAddAndSumMT() throws Throwable {
        final int incs = 1000000;
        final int nthreads = 4;
        final ExecutorService pool = Executors.newCachedThreadPool();
        LongAdder a = new LongAdder();
        CyclicBarrier barrier = new CyclicBarrier(nthreads + 1);
        for (int i = 0; i < nthreads; ++i)
            pool.execute(new AdderTask(a, barrier, incs));
        barrier.await();
        barrier.await();
        long total = (long)nthreads * incs;
        long sum = a.sum();
        assertEquals(sum, total);
        pool.shutdown();
    }

    static final class AdderTask implements Runnable {
        final LongAdder adder;
        final CyclicBarrier barrier;
        final int incs;
        volatile long result;
        AdderTask(LongAdder adder, CyclicBarrier barrier, int incs) {
            this.adder = adder;
            this.barrier = barrier;
            this.incs = incs;
        }

        public void run() {
            try {
                barrier.await();
                LongAdder a = adder;
                for (int i = 0; i < incs; ++i)
                    a.add(1L);
                result = a.sum();
                barrier.await();
            } catch (Throwable t) { throw new Error(t); }
        }
    }

}
