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
        AtomicReference ai = new AtomicReference(one);
        assertSame(one, ai.get());
    }

    /**
     * default constructed initializes to null
     */
    public void testConstructor2() {
        AtomicReference ai = new AtomicReference();
        assertNull(ai.get());
    }

    /**
     * get returns the last value set
     */
    public void testGetSet() {
        AtomicReference ai = new AtomicReference(one);
        assertSame(one, ai.get());
        ai.set(two);
        assertSame(two, ai.get());
        ai.set(m3);
        assertSame(m3, ai.get());
    }

    /**
     * get returns the last value lazySet in same thread
     */
    public void testGetLazySet() {
        AtomicReference ai = new AtomicReference(one);
        assertSame(one, ai.get());
        ai.lazySet(two);
        assertSame(two, ai.get());
        ai.lazySet(m3);
        assertSame(m3, ai.get());
    }

    /**
     * compareAndSet succeeds in changing value if equal to expected else fails
     */
    public void testCompareAndSet() {
        AtomicReference ai = new AtomicReference(one);
        assertTrue(ai.compareAndSet(one, two));
        assertTrue(ai.compareAndSet(two, m4));
        assertSame(m4, ai.get());
        assertFalse(ai.compareAndSet(m5, seven));
        assertSame(m4, ai.get());
        assertTrue(ai.compareAndSet(m4, seven));
        assertSame(seven, ai.get());
    }

    /**
     * compareAndSet in one thread enables another waiting for value
     * to succeed
     */
    public void testCompareAndSetInMultipleThreads() throws Exception {
        final AtomicReference ai = new AtomicReference(one);
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
    public void testWeakCompareAndSet() {
        AtomicReference ai = new AtomicReference(one);
        do {} while (!ai.weakCompareAndSet(one, two));
        do {} while (!ai.weakCompareAndSet(two, m4));
        assertSame(m4, ai.get());
        do {} while (!ai.weakCompareAndSet(m4, seven));
        assertSame(seven, ai.get());
    }

    /**
     * getAndSet returns previous value and sets to given value
     */
    public void testGetAndSet() {
        AtomicReference ai = new AtomicReference(one);
        assertSame(one, ai.getAndSet(zero));
        assertSame(zero, ai.getAndSet(m10));
        assertSame(m10, ai.getAndSet(one));
    }

    /**
     * a deserialized serialized atomic holds same value
     */
    public void testSerialization() throws Exception {
        AtomicReference x = new AtomicReference();
        AtomicReference y = serialClone(x);
        assertNotSame(x, y);
        x.set(one);
        AtomicReference z = serialClone(x);
        assertNotSame(y, z);
        assertEquals(one, x.get());
        assertEquals(null, y.get());
        assertEquals(one, z.get());
    }

    /**
     * toString returns current value.
     */
    public void testToString() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        assertEquals(one.toString(), ai.toString());
        ai.set(two);
        assertEquals(two.toString(), ai.toString());
    }

    // jdk9

    /**
     * getPlain returns the last value set
     */
    public void testGetPlainSet() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        assertEquals(one, ai.getPlain());
        ai.set(two);
        assertEquals(two, ai.getPlain());
        ai.set(m3);
        assertEquals(m3, ai.getPlain());
    }

    /**
     * getOpaque returns the last value set
     */
    public void testGetOpaqueSet() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        assertEquals(one, ai.getOpaque());
        ai.set(two);
        assertEquals(two, ai.getOpaque());
        ai.set(m3);
        assertEquals(m3, ai.getOpaque());
    }

    /**
     * getAcquire returns the last value set
     */
    public void testGetAcquireSet() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        assertEquals(one, ai.getAcquire());
        ai.set(two);
        assertEquals(two, ai.getAcquire());
        ai.set(m3);
        assertEquals(m3, ai.getAcquire());
    }

    /**
     * get returns the last value setPlain
     */
    public void testGetSetPlain() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        assertEquals(one, ai.get());
        ai.setPlain(two);
        assertEquals(two, ai.get());
        ai.setPlain(m3);
        assertEquals(m3, ai.get());
    }

    /**
     * get returns the last value setOpaque
     */
    public void testGetSetOpaque() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        assertEquals(one, ai.get());
        ai.setOpaque(two);
        assertEquals(two, ai.get());
        ai.setOpaque(m3);
        assertEquals(m3, ai.get());
    }

    /**
     * get returns the last value setRelease
     */
    public void testGetSetRelease() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        assertEquals(one, ai.get());
        ai.setRelease(two);
        assertEquals(two, ai.get());
        ai.setRelease(m3);
        assertEquals(m3, ai.get());
    }

    /**
     * compareAndExchange succeeds in changing value if equal to
     * expected else fails
     */
    public void testCompareAndExchange() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        assertEquals(one, ai.compareAndExchange(one, two));
        assertEquals(two, ai.compareAndExchange(two, m4));
        assertEquals(m4, ai.get());
        assertEquals(m4, ai.compareAndExchange(m5, seven));
        assertEquals(m4, ai.get());
        assertEquals(m4, ai.compareAndExchange(m4, seven));
        assertEquals(seven, ai.get());
    }

    /**
     * compareAndExchangeAcquire succeeds in changing value if equal to
     * expected else fails
     */
    public void testCompareAndExchangeAcquire() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        assertEquals(one, ai.compareAndExchangeAcquire(one, two));
        assertEquals(two, ai.compareAndExchangeAcquire(two, m4));
        assertEquals(m4, ai.get());
        assertEquals(m4, ai.compareAndExchangeAcquire(m5, seven));
        assertEquals(m4, ai.get());
        assertEquals(m4, ai.compareAndExchangeAcquire(m4, seven));
        assertEquals(seven, ai.get());
    }

    /**
     * compareAndExchangeRelease succeeds in changing value if equal to
     * expected else fails
     */
    public void testCompareAndExchangeRelease() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        assertEquals(one, ai.compareAndExchangeRelease(one, two));
        assertEquals(two, ai.compareAndExchangeRelease(two, m4));
        assertEquals(m4, ai.get());
        assertEquals(m4, ai.compareAndExchangeRelease(m5, seven));
        assertEquals(m4, ai.get());
        assertEquals(m4, ai.compareAndExchangeRelease(m4, seven));
        assertEquals(seven, ai.get());
    }

    /**
     * repeated weakCompareAndSetVolatile succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetVolatile() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        do {} while (!ai.weakCompareAndSetVolatile(one, two));
        do {} while (!ai.weakCompareAndSetVolatile(two, m4));
        assertEquals(m4, ai.get());
        do {} while (!ai.weakCompareAndSetVolatile(m4, seven));
        assertEquals(seven, ai.get());
    }

    /**
     * repeated weakCompareAndSetAcquire succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetAcquire() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        do {} while (!ai.weakCompareAndSetAcquire(one, two));
        do {} while (!ai.weakCompareAndSetAcquire(two, m4));
        assertEquals(m4, ai.get());
        do {} while (!ai.weakCompareAndSetAcquire(m4, seven));
        assertEquals(seven, ai.get());
    }

    /**
     * repeated weakCompareAndSetRelease succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetRelease() {
        AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
        do {} while (!ai.weakCompareAndSetRelease(one, two));
        do {} while (!ai.weakCompareAndSetRelease(two, m4));
        assertEquals(m4, ai.get());
        do {} while (!ai.weakCompareAndSetRelease(m4, seven));
        assertEquals(seven, ai.get());
    }

}
