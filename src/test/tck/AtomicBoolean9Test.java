/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AtomicBoolean9Test extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(AtomicBoolean9Test.class);
    }

    /**
     * getPlain returns the last value set
     */
    public void testGetPlainSet() {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertEquals(true, ai.getPlain());
        ai.set(false);
        assertEquals(false, ai.getPlain());
        ai.set(true);
        assertEquals(true, ai.getPlain());
    }

    /**
     * getOpaque returns the last value set
     */
    public void testGetOpaqueSet() {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertEquals(true, ai.getOpaque());
        ai.set(false);
        assertEquals(false, ai.getOpaque());
        ai.set(true);
        assertEquals(true, ai.getOpaque());
    }

    /**
     * getAcquire returns the last value set
     */
    public void testGetAcquireSet() {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertEquals(true, ai.getAcquire());
        ai.set(false);
        assertEquals(false, ai.getAcquire());
        ai.set(true);
        assertEquals(true, ai.getAcquire());
    }

    /**
     * get returns the last value setPlain
     */
    public void testGetSetPlain() {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertEquals(true, ai.get());
        ai.setPlain(false);
        assertEquals(false, ai.get());
        ai.setPlain(true);
        assertEquals(true, ai.get());
    }

    /**
     * get returns the last value setOpaque
     */
    public void testGetSetOpaque() {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertEquals(true, ai.get());
        ai.setOpaque(false);
        assertEquals(false, ai.get());
        ai.setOpaque(true);
        assertEquals(true, ai.get());
    }

    /**
     * get returns the last value setRelease
     */
    public void testGetSetRelease() {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertEquals(true, ai.get());
        ai.setRelease(false);
        assertEquals(false, ai.get());
        ai.setRelease(true);
        assertEquals(true, ai.get());
    }

    /**
     * compareAndExchange succeeds in changing value if equal to
     * expected else fails
     */
    public void testCompareAndExchange() {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertEquals(true, ai.compareAndExchange(true, false));
        assertEquals(false, ai.compareAndExchange(false, false));
        assertEquals(false, ai.get());
        assertEquals(false, ai.compareAndExchange(true, true));
        assertEquals(false, ai.get());
        assertEquals(false, ai.compareAndExchange(false, true));
        assertEquals(true, ai.get());
    }

    /**
     * compareAndExchangeAcquire succeeds in changing value if equal to
     * expected else fails
     */
    public void testCompareAndExchangeAcquire() {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertEquals(true, ai.compareAndExchangeAcquire(true, false));
        assertEquals(false, ai.compareAndExchangeAcquire(false, false));
        assertEquals(false, ai.get());
        assertEquals(false, ai.compareAndExchangeAcquire(true, true));
        assertEquals(false, ai.get());
        assertEquals(false, ai.compareAndExchangeAcquire(false, true));
        assertEquals(true, ai.get());
    }

    /**
     * compareAndExchangeRelease succeeds in changing value if equal to
     * expected else fails
     */
    public void testCompareAndExchangeRelease() {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertEquals(true, ai.compareAndExchangeRelease(true, false));
        assertEquals(false, ai.compareAndExchangeRelease(false, false));
        assertEquals(false, ai.get());
        assertEquals(false, ai.compareAndExchangeRelease(true, true));
        assertEquals(false, ai.get());
        assertEquals(false, ai.compareAndExchangeRelease(false, true));
        assertEquals(true, ai.get());
    }

    /**
     * repeated weakCompareAndSetPlain succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetPlain() {
        AtomicBoolean ai = new AtomicBoolean(true);
        do {} while (!ai.weakCompareAndSetPlain(true, false));
        do {} while (!ai.weakCompareAndSetPlain(false, false));
        assertFalse(ai.get());
        do {} while (!ai.weakCompareAndSetPlain(false, true));
        assertTrue(ai.get());
    }

    /**
     * repeated weakCompareAndSetVolatile succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetVolatile() {
        AtomicBoolean ai = new AtomicBoolean(true);
        do {} while (!ai.weakCompareAndSetVolatile(true, false));
        do {} while (!ai.weakCompareAndSetVolatile(false, false));
        assertEquals(false, ai.get());
        do {} while (!ai.weakCompareAndSetVolatile(false, true));
        assertEquals(true, ai.get());
    }

    /**
     * repeated weakCompareAndSetAcquire succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetAcquire() {
        AtomicBoolean ai = new AtomicBoolean(true);
        do {} while (!ai.weakCompareAndSetAcquire(true, false));
        do {} while (!ai.weakCompareAndSetAcquire(false, false));
        assertEquals(false, ai.get());
        do {} while (!ai.weakCompareAndSetAcquire(false, true));
        assertEquals(true, ai.get());
    }

    /**
     * repeated weakCompareAndSetRelease succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetRelease() {
        AtomicBoolean ai = new AtomicBoolean(true);
        do {} while (!ai.weakCompareAndSetRelease(true, false));
        do {} while (!ai.weakCompareAndSetRelease(false, false));
        assertEquals(false, ai.get());
        do {} while (!ai.weakCompareAndSetRelease(false, true));
        assertEquals(true, ai.get());
    }

}
