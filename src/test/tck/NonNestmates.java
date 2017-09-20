/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * This source file contains test code deliberately not contained in
 * the same source file as the tests that use them, to avoid making
 * them nestmates, which affects accessibility rules (see JEP 181).
 */
class NonNestmates {

    public void checkPackageAccess(AtomicIntegerFieldUpdaterTest obj) {
        obj.x = 72;
        AtomicIntegerFieldUpdater<AtomicIntegerFieldUpdaterTest> a =
            AtomicIntegerFieldUpdater.newUpdater(
                AtomicIntegerFieldUpdaterTest.class, "x");
        assertEquals(72, a.get(obj));
        assertTrue(a.compareAndSet(obj, 72, 73));
        assertEquals(73, a.get(obj));
    }

    public void checkPackageAccess(AtomicLongFieldUpdaterTest obj) {
        obj.x = 72L;
        AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a =
            AtomicLongFieldUpdater.newUpdater(
                AtomicLongFieldUpdaterTest.class, "x");
        assertEquals(72L, a.get(obj));
        assertTrue(a.compareAndSet(obj, 72L, 73L));
        assertEquals(73L, a.get(obj));
    }

    public void checkPackageAccess(AtomicReferenceFieldUpdaterTest obj) {
        Integer one = new Integer(1);
        Integer two = new Integer(2);
        obj.x = one;
        AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest,Integer> a =
            AtomicReferenceFieldUpdater.newUpdater(
                AtomicReferenceFieldUpdaterTest.class, Integer.class, "x");
        assertSame(one, a.get(obj));
        assertTrue(a.compareAndSet(obj, one, two));
        assertSame(two, a.get(obj));
    }

    public void checkPrivateAccess(AtomicIntegerFieldUpdaterTest obj) {
        try {
            AtomicIntegerFieldUpdater<AtomicIntegerFieldUpdaterTest> a =
                AtomicIntegerFieldUpdater.newUpdater(
                    AtomicIntegerFieldUpdaterTest.class, "privateField");
            throw new AssertionError("should throw");
        } catch (RuntimeException success) {
            assertNotNull(success.getCause());
        }
    }

    public void checkPrivateAccess(AtomicLongFieldUpdaterTest obj) {
        try {
            AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a =
                AtomicLongFieldUpdater.newUpdater(
                    AtomicLongFieldUpdaterTest.class, "privateField");
            throw new AssertionError("should throw");
        } catch (RuntimeException success) {
            assertNotNull(success.getCause());
        }
    }

    public void checkPrivateAccess(AtomicReferenceFieldUpdaterTest obj) {
        try {
            AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest,Integer> a =
                AtomicReferenceFieldUpdater.newUpdater(
                    AtomicReferenceFieldUpdaterTest.class,
                    Integer.class, "privateField");
            throw new AssertionError("should throw");
        } catch (RuntimeException success) {
            assertNotNull(success.getCause());
        }
    }

    static class AtomicIntegerFieldUpdaterTestSubclass
        extends AtomicIntegerFieldUpdaterTest {

        public void checkPrivateAccess() {
            try {
                AtomicIntegerFieldUpdater<AtomicIntegerFieldUpdaterTest> a =
                    AtomicIntegerFieldUpdater.newUpdater(
                        AtomicIntegerFieldUpdaterTest.class, "privateField");
                shouldThrow();
            } catch (RuntimeException success) {
                assertNotNull(success.getCause());
            }
        }

        public void checkCompareAndSetProtectedSub() {
            AtomicIntegerFieldUpdater<AtomicIntegerFieldUpdaterTest> a =
                AtomicIntegerFieldUpdater.newUpdater(
                    AtomicIntegerFieldUpdaterTest.class, "protectedField");
            this.protectedField = 1;
            assertTrue(a.compareAndSet(this, 1, 2));
            assertTrue(a.compareAndSet(this, 2, -4));
            assertEquals(-4, a.get(this));
            assertFalse(a.compareAndSet(this, -5, 7));
            assertEquals(-4, a.get(this));
            assertTrue(a.compareAndSet(this, -4, 7));
            assertEquals(7, a.get(this));
        }
    }

    static class AtomicLongFieldUpdaterTestSubclass
        extends AtomicLongFieldUpdaterTest {

        public void checkPrivateAccess() {
            try {
                AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a =
                    AtomicLongFieldUpdater.newUpdater(
                        AtomicLongFieldUpdaterTest.class, "privateField");
                shouldThrow();
            } catch (RuntimeException success) {
                assertNotNull(success.getCause());
            }
        }

        public void checkCompareAndSetProtectedSub() {
            AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a =
                AtomicLongFieldUpdater.newUpdater(
                    AtomicLongFieldUpdaterTest.class, "protectedField");
            this.protectedField = 1;
            assertTrue(a.compareAndSet(this, 1, 2));
            assertTrue(a.compareAndSet(this, 2, -4));
            assertEquals(-4, a.get(this));
            assertFalse(a.compareAndSet(this, -5, 7));
            assertEquals(-4, a.get(this));
            assertTrue(a.compareAndSet(this, -4, 7));
            assertEquals(7, a.get(this));
        }
    }

    static class AtomicReferenceFieldUpdaterTestSubclass
        extends AtomicReferenceFieldUpdaterTest {

        public void checkPrivateAccess() {
            try {
                AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest,Integer> a =
                    AtomicReferenceFieldUpdater.newUpdater(
                        AtomicReferenceFieldUpdaterTest.class,
                        Integer.class, "privateField");
                shouldThrow();
            } catch (RuntimeException success) {
                assertNotNull(success.getCause());
            }
        }

        public void checkCompareAndSetProtectedSub() {
            AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest,Integer> a =
                AtomicReferenceFieldUpdater.newUpdater(
                    AtomicReferenceFieldUpdaterTest.class,
                    Integer.class, "protectedField");
            this.protectedField = one;
            assertTrue(a.compareAndSet(this, one, two));
            assertTrue(a.compareAndSet(this, two, m4));
            assertSame(m4, a.get(this));
            assertFalse(a.compareAndSet(this, m5, seven));
            assertNotSame(seven, a.get(this));
            assertTrue(a.compareAndSet(this, m4, seven));
            assertSame(seven, a.get(this));
        }
    }
}
