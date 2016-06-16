/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceArray;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AtomicReferenceArrayTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(AtomicReferenceArrayTest.class);
    }

    /**
     * constructor creates array of given size with all elements null
     */
    public void testConstructor() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            assertNull(aa.get(i));
        }
    }

    /**
     * constructor with null array throws NPE
     */
    public void testConstructor2NPE() {
        try {
            Integer[] a = null;
            new AtomicReferenceArray<Integer>(a);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * constructor with array is of same size and has all elements
     */
    public void testConstructor2() {
        Integer[] a = { two, one, three, four, seven };
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(a);
        assertEquals(a.length, aa.length());
        for (int i = 0; i < a.length; i++)
            assertEquals(a[i], aa.get(i));
    }

    /**
     * Initialize AtomicReferenceArray<Class> with SubClass[]
     */
    public void testConstructorSubClassArray() {
        Integer[] a = { two, one, three, four, seven };
        AtomicReferenceArray<Number> aa = new AtomicReferenceArray<Number>(a);
        assertEquals(a.length, aa.length());
        for (int i = 0; i < a.length; i++) {
            assertSame(a[i], aa.get(i));
            Long x = Long.valueOf(i);
            aa.set(i, x);
            assertSame(x, aa.get(i));
        }
    }

    /**
     * get and set for out of bound indices throw IndexOutOfBoundsException
     */
    public void testIndexing() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int index : new int[] { -1, SIZE }) {
            try {
                aa.get(index);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.set(index, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.lazySet(index, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.compareAndSet(index, null, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.weakCompareAndSet(index, null, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.getPlain(index);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.getOpaque(index);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.getAcquire(index);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.setPlain(index, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.setOpaque(index, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.setRelease(index, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.compareAndExchange(index, null, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.compareAndExchangeAcquire(index, null, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.compareAndExchangeRelease(index, null, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.weakCompareAndSetVolatile(index, null, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.weakCompareAndSetAcquire(index, null, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
            try {
                aa.weakCompareAndSetRelease(index, null, null);
                shouldThrow();
            } catch (IndexOutOfBoundsException success) {}
        }
    }

    /**
     * get returns the last value set at index
     */
    public void testGetSet() {
        AtomicReferenceArray aa = new AtomicReferenceArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertSame(one, aa.get(i));
            aa.set(i, two);
            assertSame(two, aa.get(i));
            aa.set(i, m3);
            assertSame(m3, aa.get(i));
        }
    }

    /**
     * get returns the last value lazySet at index by same thread
     */
    public void testGetLazySet() {
        AtomicReferenceArray aa = new AtomicReferenceArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.lazySet(i, one);
            assertSame(one, aa.get(i));
            aa.lazySet(i, two);
            assertSame(two, aa.get(i));
            aa.lazySet(i, m3);
            assertSame(m3, aa.get(i));
        }
    }

    /**
     * compareAndSet succeeds in changing value if equal to expected else fails
     */
    public void testCompareAndSet() {
        AtomicReferenceArray aa = new AtomicReferenceArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertTrue(aa.compareAndSet(i, one, two));
            assertTrue(aa.compareAndSet(i, two, m4));
            assertSame(m4, aa.get(i));
            assertFalse(aa.compareAndSet(i, m5, seven));
            assertSame(m4, aa.get(i));
            assertTrue(aa.compareAndSet(i, m4, seven));
            assertSame(seven, aa.get(i));
        }
    }

    /**
     * compareAndSet in one thread enables another waiting for value
     * to succeed
     */
    public void testCompareAndSetInMultipleThreads() throws InterruptedException {
        final AtomicReferenceArray a = new AtomicReferenceArray(1);
        a.set(0, one);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                while (!a.compareAndSet(0, two, three))
                    Thread.yield();
            }});

        t.start();
        assertTrue(a.compareAndSet(0, one, two));
        t.join(LONG_DELAY_MS);
        assertFalse(t.isAlive());
        assertSame(three, a.get(0));
    }

    /**
     * repeated weakCompareAndSet succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSet() {
        AtomicReferenceArray aa = new AtomicReferenceArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            do {} while (!aa.weakCompareAndSet(i, one, two));
            do {} while (!aa.weakCompareAndSet(i, two, m4));
            assertSame(m4, aa.get(i));
            do {} while (!aa.weakCompareAndSet(i, m4, seven));
            assertSame(seven, aa.get(i));
        }
    }

    /**
     * getAndSet returns previous value and sets to given value at given index
     */
    public void testGetAndSet() {
        AtomicReferenceArray aa = new AtomicReferenceArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertSame(one, aa.getAndSet(i, zero));
            assertSame(zero, aa.getAndSet(i, m10));
            assertSame(m10, aa.getAndSet(i, one));
        }
    }

    /**
     * a deserialized serialized array holds same values
     */
    public void testSerialization() throws Exception {
        AtomicReferenceArray x = new AtomicReferenceArray(SIZE);
        for (int i = 0; i < SIZE; i++) {
            x.set(i, new Integer(-i));
        }
        AtomicReferenceArray y = serialClone(x);
        assertNotSame(x, y);
        assertEquals(x.length(), y.length());
        for (int i = 0; i < SIZE; i++) {
            assertEquals(x.get(i), y.get(i));
        }
    }

    /**
     * toString returns current value.
     */
    public void testToString() {
        Integer[] a = { two, one, three, four, seven };
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(a);
        assertEquals(Arrays.toString(a), aa.toString());
    }

    // jdk9

    /**
     * getPlain returns the last value set
     */
    public void testGetPlainSet() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertEquals(one, aa.getPlain(i));
            aa.set(i, two);
            assertEquals(two, aa.getPlain(i));
            aa.set(i, m3);
            assertEquals(m3, aa.getPlain(i));
        }
    }

    /**
     * getOpaque returns the last value set
     */
    public void testGetOpaqueSet() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertEquals(one, aa.getOpaque(i));
            aa.set(i, two);
            assertEquals(two, aa.getOpaque(i));
            aa.set(i, m3);
            assertEquals(m3, aa.getOpaque(i));
        }
    }

    /**
     * getAcquire returns the last value set
     */
    public void testGetAcquireSet() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertEquals(one, aa.getAcquire(i));
            aa.set(i, two);
            assertEquals(two, aa.getAcquire(i));
            aa.set(i, m3);
            assertEquals(m3, aa.getAcquire(i));
        }
    }

    /**
     * get returns the last value setPlain
     */
    public void testGetSetPlain() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.setPlain(i, one);
            assertEquals(one, aa.get(i));
            aa.setPlain(i, two);
            assertEquals(two, aa.get(i));
            aa.setPlain(i, m3);
            assertEquals(m3, aa.get(i));
        }
    }

    /**
     * get returns the last value setOpaque
     */
    public void testGetSetOpaque() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.setOpaque(i, one);
            assertEquals(one, aa.get(i));
            aa.setOpaque(i, two);
            assertEquals(two, aa.get(i));
            aa.setOpaque(i, m3);
            assertEquals(m3, aa.get(i));
        }
    }

    /**
     * get returns the last value setRelease
     */
    public void testGetSetRelease() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.setRelease(i, one);
            assertEquals(one, aa.get(i));
            aa.setRelease(i, two);
            assertEquals(two, aa.get(i));
            aa.setRelease(i, m3);
            assertEquals(m3, aa.get(i));
        }
    }

    /**
     * compareAndExchange succeeds in changing value if equal to
     * expected else fails
     */
    public void testCompareAndExchange() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertEquals(one, aa.compareAndExchange(i, one, two));
            assertEquals(two, aa.compareAndExchange(i, two, m4));
            assertEquals(m4, aa.get(i));
            assertEquals(m4, aa.compareAndExchange(i,m5, seven));
            assertEquals(m4, aa.get(i));
            assertEquals(m4, aa.compareAndExchange(i, m4, seven));
            assertEquals(seven, aa.get(i));
        }
    }

    /**
     * compareAndExchangeAcquire succeeds in changing value if equal to
     * expected else fails
     */
    public void testCompareAndExchangeAcquire() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertEquals(one, aa.compareAndExchangeAcquire(i, one, two));
            assertEquals(two, aa.compareAndExchangeAcquire(i, two, m4));
            assertEquals(m4, aa.get(i));
            assertEquals(m4, aa.compareAndExchangeAcquire(i,m5, seven));
            assertEquals(m4, aa.get(i));
            assertEquals(m4, aa.compareAndExchangeAcquire(i, m4, seven));
            assertEquals(seven, aa.get(i));
        }
    }

    /**
     * compareAndExchangeRelease succeeds in changing value if equal to
     * expected else fails
     */
    public void testCompareAndExchangeRelease() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertEquals(one, aa.compareAndExchangeRelease(i, one, two));
            assertEquals(two, aa.compareAndExchangeRelease(i, two, m4));
            assertEquals(m4, aa.get(i));
            assertEquals(m4, aa.compareAndExchangeRelease(i,m5, seven));
            assertEquals(m4, aa.get(i));
            assertEquals(m4, aa.compareAndExchangeRelease(i, m4, seven));
            assertEquals(seven, aa.get(i));
        }
    }

    /**
     * repeated weakCompareAndSetVolatile succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetVolatile() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            do {} while (!aa.weakCompareAndSetVolatile(i, one, two));
            do {} while (!aa.weakCompareAndSetVolatile(i, two, m4));
            assertEquals(m4, aa.get(i));
            do {} while (!aa.weakCompareAndSetVolatile(i, m4, seven));
            assertEquals(seven, aa.get(i));
        }
    }

    /**
     * repeated weakCompareAndSetAcquire succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetAcquire() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            do {} while (!aa.weakCompareAndSetAcquire(i, one, two));
            do {} while (!aa.weakCompareAndSetAcquire(i, two, m4));
            assertEquals(m4, aa.get(i));
            do {} while (!aa.weakCompareAndSetAcquire(i, m4, seven));
            assertEquals(seven, aa.get(i));
        }
    }

    /**
     * repeated weakCompareAndSetRelease succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetRelease() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<Integer>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            do {} while (!aa.weakCompareAndSetRelease(i, one, two));
            do {} while (!aa.weakCompareAndSetRelease(i, two, m4));
            assertEquals(m4, aa.get(i));
            do {} while (!aa.weakCompareAndSetRelease(i, m4, seven));
            assertEquals(seven, aa.get(i));
        }
    }
    
}
