/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Collection;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Queue;

import junit.framework.Test;

/**
 * Contains tests applicable to all Collection implementations.
 */
public class CollectionTest extends JSR166TestCase {
    final CollectionImplementation impl;

    /** Tests are parameterized by a Collection implementation. */
    CollectionTest(CollectionImplementation impl, String methodName) {
        super(methodName);
        this.impl = impl;
    }

    public static Test testSuite(CollectionImplementation impl) {
        return newTestSuite
            (parameterizedTestSuite(CollectionTest.class,
                                    CollectionImplementation.class,
                                    impl),
             jdk8ParameterizedTestSuite(CollectionTest.class,
                                        CollectionImplementation.class,
                                        impl));
    }

    /** Checks properties of empty collections. */
    public void testEmptyMeansEmpty() {
        Collection c = impl.emptyCollection();
        assertTrue(c.isEmpty());
        assertEquals(0, c.size());
        assertEquals("[]", c.toString());
        assertEquals(0, c.toArray().length);
        {
            Object[] a = new Object[0];
            assertSame(a, c.toArray(a));
        }
        c.forEach((e) -> { throw new AssertionError(); });
        if (Queue.class.isAssignableFrom(impl.klazz())) {
            Queue q = (Queue) c;
            assertNull(q.peek());
            assertNull(q.poll());
        }
        if (Deque.class.isAssignableFrom(impl.klazz())) {
            Deque d = (Deque) c;
            assertNull(d.peekFirst());
            assertNull(d.peekLast());
            assertNull(d.pollFirst());
            assertNull(d.pollLast());
        }
    }

    public void testNullPointerExceptions() {
        Collection c = impl.emptyCollection();
        assertThrows(
            NullPointerException.class,
            () -> c.addAll(null),
            () -> c.containsAll(null),
            () -> c.retainAll(null),
            () -> c.removeAll(null),
            () -> c.removeIf(null));

        if (!impl.permitsNulls()) {
            assertThrows(
                NullPointerException.class,
                () -> c.add(null));
        }
        if (!impl.permitsNulls()
            && Queue.class.isAssignableFrom(impl.klazz())) {
            Queue q = (Queue) c;
            assertThrows(
                NullPointerException.class,
                () -> q.offer(null));
        }
        if (!impl.permitsNulls()
            && Deque.class.isAssignableFrom(impl.klazz())) {
            Deque d = (Deque) c;
            assertThrows(
                NullPointerException.class,
                () -> d.addFirst(null),
                () -> d.addLast(null),
                () -> d.offerFirst(null),
                () -> d.offerLast(null),
                () -> d.push(null));
        }
    }

    public void testNoSuchElementExceptions() {
        Collection c = impl.emptyCollection();

        if (Queue.class.isAssignableFrom(impl.klazz())) {
            Queue q = (Queue) c;
            assertThrows(
                NoSuchElementException.class,
                () -> q.element(),
                () -> q.remove());
        }
        if (Deque.class.isAssignableFrom(impl.klazz())) {
            Deque d = (Deque) c;
            assertThrows(
                NoSuchElementException.class,
                () -> d.getFirst(),
                () -> d.getLast(),
                () -> d.removeFirst(),
                () -> d.removeLast(),
                () -> d.pop());
        }
    }

    // public void testCollectionDebugFail() { fail(); }
}
