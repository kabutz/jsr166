/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AtomicReferenceTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(AtomicReferenceTest.class);
    }

    /**
     * constructor initializes to given value
     */
    public void testConstructor() {
        AtomicReference<Item> ai = new AtomicReference<Item>(one);
        assertSame(one, ai.get());
    }

    /**
     * default constructed initializes to null
     */
    public void testConstructor2() {
        AtomicReference<Item> ai = new AtomicReference<Item>();
        assertNull(ai.get());
    }

    /**
     * get returns the last value set
     */
    public void testGetSet() {
        AtomicReference<Item> ai = new AtomicReference<Item>(one);
        assertSame(one, ai.get());
        ai.set(two);
        assertSame(two, ai.get());
        ai.set(minusThree);
        assertSame(minusThree, ai.get());
    }

    /**
     * get returns the last value lazySet in same thread
     */
    public void testGetLazySet() {
        AtomicReference<Item> ai = new AtomicReference<Item>(one);
        assertSame(one, ai.get());
        ai.lazySet(two);
        assertSame(two, ai.get());
        ai.lazySet(minusThree);
        assertSame(minusThree, ai.get());
    }

    /**
     * compareAndSet succeeds in changing value if equal to expected else fails
     */
    public void testCompareAndSet() {
        AtomicReference<Item> ai = new AtomicReference<Item>(one);
        assertTrue(ai.compareAndSet(one, two));
        assertTrue(ai.compareAndSet(two, minusFour));
        assertSame(minusFour, ai.get());
        assertFalse(ai.compareAndSet(minusFive, seven));
        assertSame(minusFour, ai.get());
        assertTrue(ai.compareAndSet(minusFour, seven));
        assertSame(seven, ai.get());
    }

    /**
     * compareAndSet in one thread enables another waiting for value
     * to succeed
     */
    public void testCompareAndSetInMultipleThreads() throws Exception {
        final AtomicReference<Item> ai = new AtomicReference<Item>(one);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                while (!ai.compareAndSet(two, three))
                    Thread.yield();
            }});

        t.start();
        assertTrue(ai.compareAndSet(one, two));
        t.join(LONG_DELAY_MS);
        assertFalse(t.isAlive());
        assertSame(three, ai.get());
    }

    /**
     * repeated weakCompareAndSet succeeds in changing value when equal
     * to expected
     */
    @SuppressWarnings("deprecation")
    public void testWeakCompareAndSet() {
        AtomicReference<Item> ai = new AtomicReference<Item>(one);
        do {} while (!ai.weakCompareAndSet(one, two));
        do {} while (!ai.weakCompareAndSet(two, minusFour));
        assertSame(minusFour, ai.get());
        do {} while (!ai.weakCompareAndSet(minusFour, seven));
        assertSame(seven, ai.get());
    }

    /**
     * getAndSet returns previous value and sets to given value
     */
    public void testGetAndSet() {
        AtomicReference<Item> ai = new AtomicReference<Item>(one);
        assertSame(one, ai.getAndSet(zero));
        assertSame(zero, ai.getAndSet(minusTen));
        assertSame(minusTen, ai.getAndSet(one));
    }

    /**
     * a deserialized/reserialized atomic holds same value
     */
    public void testSerialization() throws Exception {
        AtomicReference<Item> x = new AtomicReference<Item>();
        AtomicReference<Item> y = serialClone(x);
        assertNotSame(x, y);
        x.set(one);
        AtomicReference<Item> z = serialClone(x);
        assertNotSame(y, z);
        assertEquals(one, x.get());
        assertNull(y.get());
        assertEquals(one, z.get());
    }

    /**
     * toString returns current value.
     */
    public void testToString() {
        AtomicReference<Item> ai = new AtomicReference<Item>(one);
        assertEquals(one.toString(), ai.toString());
        ai.set(two);
        assertEquals(two.toString(), ai.toString());
    }

}
