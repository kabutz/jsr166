/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.concurrent.atomic.AtomicReferenceArray;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AtomicReferenceArray9Test extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(AtomicReferenceArray9Test.class);
    }

    /**
     * get and set for out of bound indices throw IndexOutOfBoundsException
     */
    public void testIndexing() {
        AtomicReferenceArray<Integer> aa = new AtomicReferenceArray<>(SIZE);
        for (int index : new int[] { -1, SIZE }) {
            final int j = index;
            assertThrows(
                IndexOutOfBoundsException.class,
                () -> aa.getPlain(j),
                () -> aa.getOpaque(j),
                () -> aa.getAcquire(j),
                () -> aa.setPlain(j, null),
                () -> aa.setOpaque(j, null),
                () -> aa.setRelease(j, null),
                () -> aa.compareAndExchange(j, null, null),
                () -> aa.compareAndExchangeAcquire(j, null, null),
                () -> aa.compareAndExchangeRelease(j, null, null),
                () -> aa.weakCompareAndSetPlain(j, null, null),
                () -> aa.weakCompareAndSetVolatile(j, null, null),
                () -> aa.weakCompareAndSetAcquire(j, null, null),
                () -> aa.weakCompareAndSetRelease(j, null, null));
        }
    }

    /**
     * getPlain returns the last value set
     */
    public void testGetPlainSet() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertEquals(one, aa.getPlain(i));
            aa.set(i, two);
            assertEquals(two, aa.getPlain(i));
            aa.set(i, minusThree);
            assertEquals(minusThree, aa.getPlain(i));
        }
    }

    /**
     * getOpaque returns the last value set
     */
    public void testGetOpaqueSet() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertEquals(one, aa.getOpaque(i));
            aa.set(i, two);
            assertEquals(two, aa.getOpaque(i));
            aa.set(i, minusThree);
            assertEquals(minusThree, aa.getOpaque(i));
        }
    }

    /**
     * getAcquire returns the last value set
     */
    public void testGetAcquireSet() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertEquals(one, aa.getAcquire(i));
            aa.set(i, two);
            assertEquals(two, aa.getAcquire(i));
            aa.set(i, minusThree);
            assertEquals(minusThree, aa.getAcquire(i));
        }
    }

    /**
     * get returns the last value setPlain
     */
    public void testGetSetPlain() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.setPlain(i, one);
            assertEquals(one, aa.get(i));
            aa.setPlain(i, two);
            assertEquals(two, aa.get(i));
            aa.setPlain(i, minusThree);
            assertEquals(minusThree, aa.get(i));
        }
    }

    /**
     * get returns the last value setOpaque
     */
    public void testGetSetOpaque() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.setOpaque(i, one);
            assertEquals(one, aa.get(i));
            aa.setOpaque(i, two);
            assertEquals(two, aa.get(i));
            aa.setOpaque(i, minusThree);
            assertEquals(minusThree, aa.get(i));
        }
    }

    /**
     * get returns the last value setRelease
     */
    public void testGetSetRelease() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.setRelease(i, one);
            assertEquals(one, aa.get(i));
            aa.setRelease(i, two);
            assertEquals(two, aa.get(i));
            aa.setRelease(i, minusThree);
            assertEquals(minusThree, aa.get(i));
        }
    }

    /**
     * compareAndExchange succeeds in changing value if equal to
     * expected else fails
     */
    public void testCompareAndExchange() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertEquals(one, aa.compareAndExchange(i, one, two));
            assertEquals(two, aa.compareAndExchange(i, two, minusFour));
            assertEquals(minusFour, aa.get(i));
            assertEquals(minusFour, aa.compareAndExchange(i,minusFive, seven));
            assertEquals(minusFour, aa.get(i));
            assertEquals(minusFour, aa.compareAndExchange(i, minusFour, seven));
            assertEquals(seven, aa.get(i));
        }
    }

    /**
     * compareAndExchangeAcquire succeeds in changing value if equal to
     * expected else fails
     */
    public void testCompareAndExchangeAcquire() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertEquals(one, aa.compareAndExchangeAcquire(i, one, two));
            assertEquals(two, aa.compareAndExchangeAcquire(i, two, minusFour));
            assertEquals(minusFour, aa.get(i));
            assertEquals(minusFour, aa.compareAndExchangeAcquire(i,minusFive, seven));
            assertEquals(minusFour, aa.get(i));
            assertEquals(minusFour, aa.compareAndExchangeAcquire(i, minusFour, seven));
            assertEquals(seven, aa.get(i));
        }
    }

    /**
     * compareAndExchangeRelease succeeds in changing value if equal to
     * expected else fails
     */
    public void testCompareAndExchangeRelease() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            assertEquals(one, aa.compareAndExchangeRelease(i, one, two));
            assertEquals(two, aa.compareAndExchangeRelease(i, two, minusFour));
            assertEquals(minusFour, aa.get(i));
            assertEquals(minusFour, aa.compareAndExchangeRelease(i,minusFive, seven));
            assertEquals(minusFour, aa.get(i));
            assertEquals(minusFour, aa.compareAndExchangeRelease(i, minusFour, seven));
            assertEquals(seven, aa.get(i));
        }
    }

    /**
     * repeated weakCompareAndSetPlain succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetPlain() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            do {} while (!aa.weakCompareAndSetPlain(i, one, two));
            do {} while (!aa.weakCompareAndSetPlain(i, two, minusFour));
            assertEquals(minusFour, aa.get(i));
            do {} while (!aa.weakCompareAndSetPlain(i, minusFour, seven));
            assertEquals(seven, aa.get(i));
        }
    }

    /**
     * repeated weakCompareAndSetVolatile succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetVolatile() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            do {} while (!aa.weakCompareAndSetVolatile(i, one, two));
            do {} while (!aa.weakCompareAndSetVolatile(i, two, minusFour));
            assertEquals(minusFour, aa.get(i));
            do {} while (!aa.weakCompareAndSetVolatile(i, minusFour, seven));
            assertEquals(seven, aa.get(i));
        }
    }

    /**
     * repeated weakCompareAndSetAcquire succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetAcquire() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            do {} while (!aa.weakCompareAndSetAcquire(i, one, two));
            do {} while (!aa.weakCompareAndSetAcquire(i, two, minusFour));
            assertEquals(minusFour, aa.get(i));
            do {} while (!aa.weakCompareAndSetAcquire(i, minusFour, seven));
            assertEquals(seven, aa.get(i));
        }
    }

    /**
     * repeated weakCompareAndSetRelease succeeds in changing value when equal
     * to expected
     */
    public void testWeakCompareAndSetRelease() {
        AtomicReferenceArray<Item> aa = new AtomicReferenceArray<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            aa.set(i, one);
            do {} while (!aa.weakCompareAndSetRelease(i, one, two));
            do {} while (!aa.weakCompareAndSetRelease(i, two, minusFour));
            assertEquals(minusFour, aa.get(i));
            do {} while (!aa.weakCompareAndSetRelease(i, minusFour, seven));
            assertEquals(seven, aa.get(i));
        }
    }

}
