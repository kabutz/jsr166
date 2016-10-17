/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
        assertIteratorExhausted(c.iterator());
        Consumer alwaysThrows = (e) -> { throw new AssertionError(); };
        c.forEach(alwaysThrows);
        c.iterator().forEachRemaining(alwaysThrows);
        c.spliterator().forEachRemaining(alwaysThrows);
        assertFalse(c.spliterator().tryAdvance(alwaysThrows));
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
            assertIteratorExhausted(d.descendingIterator());
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
        assertThrows(
            NoSuchElementException.class,
            () -> c.iterator().next());

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
                () -> d.pop(),
                () -> d.descendingIterator().next());
        }
    }

    public void testRemoveIf() {
        Collection c = impl.emptyCollection();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = rnd.nextInt(6);
        for (int i = 0; i < n; i++) c.add(impl.makeElement(i));
        AtomicReference threwAt = new AtomicReference(null);
        ArrayList survivors = new ArrayList(c);
        ArrayList accepts = new ArrayList();
        ArrayList rejects = new ArrayList();
        Predicate randomPredicate = (e) -> {
            assertNull(threwAt.get());
            switch (rnd.nextInt(3)) {
            case 0: accepts.add(e); return true;
            case 1: rejects.add(e); return false;
            case 2: threwAt.set(e); throw new ArithmeticException();
            default: throw new AssertionError();
            }
        };
        try {
            boolean modified = c.removeIf(randomPredicate);
            if (!modified) {
                assertNull(threwAt.get());
                assertEquals(n, rejects.size());
                assertEquals(0, accepts.size());
            }
        } catch (ArithmeticException ok) {}
        survivors.removeAll(accepts);
        if (n - accepts.size() != c.size()) {
            System.err.println(impl.klazz());
            System.err.println(c);
            System.err.println(accepts);
            System.err.println(rejects);
            System.err.println(survivors);
            System.err.println(threwAt.get());
        }
        assertEquals(n - accepts.size(), c.size());
        assertTrue(c.containsAll(survivors));
        assertTrue(survivors.containsAll(rejects));
        for (Object x : accepts) assertFalse(c.contains(x));
        if (threwAt.get() == null)
            assertEquals(accepts.size() + rejects.size(), n);
    }

//     public void testCollectionDebugFail() {
//         fail(impl.klazz().getSimpleName());
//     }
}
