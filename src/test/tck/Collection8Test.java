/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import junit.framework.Test;

/**
 * Contains tests applicable to all jdk8+ Collection implementations.
 * An extension of CollectionTest.
 */
public class Collection8Test extends JSR166TestCase {
    final CollectionImplementation impl;

    /** Tests are parameterized by a Collection implementation. */
    Collection8Test(CollectionImplementation impl, String methodName) {
        super(methodName);
        this.impl = impl;
    }

    public static Test testSuite(CollectionImplementation impl) {
        return parameterizedTestSuite(Collection8Test.class,
                                      CollectionImplementation.class,
                                      impl);
    }

    Object bomb() {
        return new Object() {
                public boolean equals(Object x) { throw new AssertionError(); }
                public int hashCode() { throw new AssertionError(); }
            };
    }

    /** Checks properties of empty collections. */
    public void testEmptyMeansEmpty() throws InterruptedException {
        Collection c = impl.emptyCollection();
        emptyMeansEmpty(c);

        if (c instanceof java.io.Serializable)
            emptyMeansEmpty(serialClone(c));

        Collection clone = cloneableClone(c);
        if (clone != null)
            emptyMeansEmpty(clone);
    }

    void emptyMeansEmpty(Collection c) throws InterruptedException {
        assertTrue(c.isEmpty());
        assertEquals(0, c.size());
        assertEquals("[]", c.toString());
        {
            Object[] a = c.toArray();
            assertEquals(0, a.length);
            assertSame(Object[].class, a.getClass());
        }
        {
            Object[] a = new Object[0];
            assertSame(a, c.toArray(a));
        }
        {
            Integer[] a = new Integer[0];
            assertSame(a, c.toArray(a));
        }
        {
            Integer[] a = { 1, 2, 3};
            assertSame(a, c.toArray(a));
            assertNull(a[0]);
            assertSame(2, a[1]);
            assertSame(3, a[2]);
        }
        assertIteratorExhausted(c.iterator());
        Consumer alwaysThrows = (e) -> { throw new AssertionError(); };
        c.forEach(alwaysThrows);
        c.iterator().forEachRemaining(alwaysThrows);
        c.spliterator().forEachRemaining(alwaysThrows);
        assertFalse(c.spliterator().tryAdvance(alwaysThrows));
        if (c.spliterator().hasCharacteristics(Spliterator.SIZED))
            assertEquals(0, c.spliterator().estimateSize());
        assertFalse(c.contains(bomb()));
        assertFalse(c.remove(bomb()));
        if (c instanceof Queue) {
            Queue q = (Queue) c;
            assertNull(q.peek());
            assertNull(q.poll());
        }
        if (c instanceof Deque) {
            Deque d = (Deque) c;
            assertNull(d.peekFirst());
            assertNull(d.peekLast());
            assertNull(d.pollFirst());
            assertNull(d.pollLast());
            assertIteratorExhausted(d.descendingIterator());
            d.descendingIterator().forEachRemaining(alwaysThrows);
            assertFalse(d.removeFirstOccurrence(bomb()));
            assertFalse(d.removeLastOccurrence(bomb()));
        }
        if (c instanceof BlockingQueue) {
            BlockingQueue q = (BlockingQueue) c;
            assertNull(q.poll(0L, MILLISECONDS));
        }
        if (c instanceof BlockingDeque) {
            BlockingDeque q = (BlockingDeque) c;
            assertNull(q.pollFirst(0L, MILLISECONDS));
            assertNull(q.pollLast(0L, MILLISECONDS));
        }
    }

    public void testNullPointerExceptions() throws InterruptedException {
        Collection c = impl.emptyCollection();
        assertThrows(
            NullPointerException.class,
            () -> c.addAll(null),
            () -> c.containsAll(null),
            () -> c.retainAll(null),
            () -> c.removeAll(null),
            () -> c.removeIf(null),
            () -> c.forEach(null),
            () -> c.iterator().forEachRemaining(null),
            () -> c.spliterator().forEachRemaining(null),
            () -> c.spliterator().tryAdvance(null),
            () -> c.toArray(null));

        if (!impl.permitsNulls()) {
            assertThrows(
                NullPointerException.class,
                () -> c.add(null));
        }
        if (!impl.permitsNulls() && c instanceof Queue) {
            Queue q = (Queue) c;
            assertThrows(
                NullPointerException.class,
                () -> q.offer(null));
        }
        if (!impl.permitsNulls() && c instanceof Deque) {
            Deque d = (Deque) c;
            assertThrows(
                NullPointerException.class,
                () -> d.addFirst(null),
                () -> d.addLast(null),
                () -> d.offerFirst(null),
                () -> d.offerLast(null),
                () -> d.push(null),
                () -> d.descendingIterator().forEachRemaining(null));
        }
        if (c instanceof BlockingQueue) {
            BlockingQueue q = (BlockingQueue) c;
            assertThrows(
                NullPointerException.class,
                () -> {
                    try { q.offer(null, 1L, MILLISECONDS); }
                    catch (InterruptedException ex) {
                        throw new AssertionError(ex);
                    }},
                () -> {
                    try { q.put(null); }
                    catch (InterruptedException ex) {
                        throw new AssertionError(ex);
                    }});
        }
        if (c instanceof BlockingDeque) {
            BlockingDeque q = (BlockingDeque) c;
            assertThrows(
                NullPointerException.class,
                () -> {
                    try { q.offerFirst(null, 1L, MILLISECONDS); }
                    catch (InterruptedException ex) {
                        throw new AssertionError(ex);
                    }},
                () -> {
                    try { q.offerLast(null, 1L, MILLISECONDS); }
                    catch (InterruptedException ex) {
                        throw new AssertionError(ex);
                    }},
                () -> {
                    try { q.putFirst(null); }
                    catch (InterruptedException ex) {
                        throw new AssertionError(ex);
                    }},
                () -> {
                    try { q.putLast(null); }
                    catch (InterruptedException ex) {
                        throw new AssertionError(ex);
                    }});
        }
    }

    public void testNoSuchElementExceptions() {
        Collection c = impl.emptyCollection();
        assertThrows(
            NoSuchElementException.class,
            () -> c.iterator().next());

        if (c instanceof Queue) {
            Queue q = (Queue) c;
            assertThrows(
                NoSuchElementException.class,
                () -> q.element(),
                () -> q.remove());
        }
        if (c instanceof Deque) {
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
            assertFalse(survivors.contains(null));
            try {
                boolean modified = c.removeIf(randomPredicate);
                if (!modified) {
                    assertNull(threwAt.get());
                    assertEquals(n, rejects.size());
                    assertEquals(0, accepts.size());
                }
            } catch (ArithmeticException ok) {}
            survivors.removeAll(accepts);
            assertEquals(n - accepts.size(), c.size());
            assertTrue(c.containsAll(survivors));
            assertTrue(survivors.containsAll(rejects));
            for (Object x : accepts) assertFalse(c.contains(x));
            if (threwAt.get() == null)
                assertEquals(accepts.size() + rejects.size(), n);
        } catch (Throwable ex) {
            System.err.println(impl.klazz());
            System.err.printf("c=%s%n", c);
            System.err.printf("n=%d%n", n);
            System.err.printf("accepts=%s%n", accepts);
            System.err.printf("rejects=%s%n", rejects);
            System.err.printf("survivors=%s%n", survivors);
            System.err.printf("threw=%s%n", threwAt.get());
            throw ex;
        }
    }

    /**
     * Various ways of traversing a collection yield same elements
     */
    public void testIteratorEquivalence() {
        Collection c = impl.emptyCollection();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = rnd.nextInt(6);
        for (int i = 0; i < n; i++) c.add(impl.makeElement(i));
        ArrayList iterated = new ArrayList();
        ArrayList iteratedForEachRemaining = new ArrayList();
        ArrayList tryAdvanced = new ArrayList();
        ArrayList spliterated = new ArrayList();
        ArrayList forEached = new ArrayList();
        ArrayList removeIfed = new ArrayList();
        for (Object x : c) iterated.add(x);
        c.iterator().forEachRemaining(e -> iteratedForEachRemaining.add(e));
        for (Spliterator s = c.spliterator();
             s.tryAdvance(e -> tryAdvanced.add(e)); ) {}
        c.spliterator().forEachRemaining(e -> spliterated.add(e));
        c.forEach(e -> forEached.add(e));
        c.removeIf(e -> { removeIfed.add(e); return false; });
        boolean ordered =
            c.spliterator().hasCharacteristics(Spliterator.ORDERED);
        if (c instanceof List || c instanceof Deque)
            assertTrue(ordered);
        if (ordered) {
            assertEquals(iterated, iteratedForEachRemaining);
            assertEquals(iterated, tryAdvanced);
            assertEquals(iterated, spliterated);
            assertEquals(iterated, forEached);
            assertEquals(iterated, removeIfed);
        } else {
            HashSet cset = new HashSet(c);
            assertEquals(cset, new HashSet(iterated));
            assertEquals(cset, new HashSet(iteratedForEachRemaining));
            assertEquals(cset, new HashSet(tryAdvanced));
            assertEquals(cset, new HashSet(spliterated));
            assertEquals(cset, new HashSet(forEached));
            assertEquals(cset, new HashSet(removeIfed));
        }
        if (c instanceof Deque) {
            Deque d = (Deque) c;
            ArrayList descending = new ArrayList();
            ArrayList descendingForEachRemaining = new ArrayList();
            for (Iterator it = d.descendingIterator(); it.hasNext(); )
                descending.add(it.next());
            d.descendingIterator().forEachRemaining(
                e -> descendingForEachRemaining.add(e));
            Collections.reverse(descending);
            Collections.reverse(descendingForEachRemaining);
            assertEquals(iterated, descending);
            assertEquals(iterated, descendingForEachRemaining);
        }
    }

    /**
     * Calling Iterator#remove() after Iterator#forEachRemaining
     * should remove last element
     */
    public void testRemoveAfterForEachRemaining() {
        Collection c = impl.emptyCollection();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        {
            int n = 3 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) c.add(impl.makeElement(i));
            Iterator it = c.iterator();
            assertTrue(it.hasNext());
            assertEquals(impl.makeElement(0), it.next());
            assertTrue(it.hasNext());
            assertEquals(impl.makeElement(1), it.next());
            it.forEachRemaining((e) -> {});
            it.remove();
            assertEquals(n - 1, c.size());
            for (int i = 0; i < n - 1; i++)
                assertTrue(c.contains(impl.makeElement(i)));
            assertFalse(c.contains(impl.makeElement(n - 1)));
        }
        if (c instanceof Deque) {
            Deque d = (Deque) impl.emptyCollection();
            int n = 3 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) d.add(impl.makeElement(i));
            Iterator it = d.descendingIterator();
            assertTrue(it.hasNext());
            assertEquals(impl.makeElement(n - 1), it.next());
            assertTrue(it.hasNext());
            assertEquals(impl.makeElement(n - 2), it.next());
            it.forEachRemaining((e) -> {});
            it.remove();
            assertEquals(n - 1, d.size());
            for (int i = 1; i < n; i++)
                assertTrue(d.contains(impl.makeElement(i)));
            assertFalse(d.contains(impl.makeElement(0)));
        }
    }

    /**
     * stream().forEach returns elements in the collection
     */
    public void testStreamForEach() throws Throwable {
        final Collection c = impl.emptyCollection();
        final AtomicLong count = new AtomicLong(0L);
        final Object x = impl.makeElement(1);
        final Object y = impl.makeElement(2);
        final ArrayList found = new ArrayList();
        Consumer<Object> spy = (o) -> { found.add(o); };
        c.stream().forEach(spy);
        assertTrue(found.isEmpty());

        assertTrue(c.add(x));
        c.stream().forEach(spy);
        assertEquals(Collections.singletonList(x), found);
        found.clear();

        assertTrue(c.add(y));
        c.stream().forEach(spy);
        assertEquals(2, found.size());
        assertTrue(found.contains(x));
        assertTrue(found.contains(y));
        found.clear();

        c.clear();
        c.stream().forEach(spy);
        assertTrue(found.isEmpty());
    }

    public void testStreamForEachConcurrentStressTest() throws Throwable {
        if (!impl.isConcurrent()) return;
        final Collection c = impl.emptyCollection();
        final long testDurationMillis = timeoutMillis();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Object elt = impl.makeElement(1);
        final Future<?> f1, f2;
        final ExecutorService pool = Executors.newCachedThreadPool();
        try (PoolCleaner cleaner = cleaner(pool, done)) {
            final CountDownLatch threadsStarted = new CountDownLatch(2);
            Runnable checkElt = () -> {
                threadsStarted.countDown();
                while (!done.get())
                    c.stream().forEach((x) -> { assertSame(x, elt); }); };
            Runnable addRemove = () -> {
                threadsStarted.countDown();
                while (!done.get()) {
                    assertTrue(c.add(elt));
                    assertTrue(c.remove(elt));
                }};
            f1 = pool.submit(checkElt);
            f2 = pool.submit(addRemove);
            Thread.sleep(testDurationMillis);
        }
        assertNull(f1.get(0L, MILLISECONDS));
        assertNull(f2.get(0L, MILLISECONDS));
    }

    /**
     * collection.forEach returns elements in the collection
     */
    public void testForEach() throws Throwable {
        final Collection c = impl.emptyCollection();
        final AtomicLong count = new AtomicLong(0L);
        final Object x = impl.makeElement(1);
        final Object y = impl.makeElement(2);
        final ArrayList found = new ArrayList();
        Consumer<Object> spy = (o) -> { found.add(o); };
        c.forEach(spy);
        assertTrue(found.isEmpty());

        assertTrue(c.add(x));
        c.forEach(spy);
        assertEquals(Collections.singletonList(x), found);
        found.clear();

        assertTrue(c.add(y));
        c.forEach(spy);
        assertEquals(2, found.size());
        assertTrue(found.contains(x));
        assertTrue(found.contains(y));
        found.clear();

        c.clear();
        c.forEach(spy);
        assertTrue(found.isEmpty());
    }

    public void testForEachConcurrentStressTest() throws Throwable {
        if (!impl.isConcurrent()) return;
        final Collection c = impl.emptyCollection();
        final long testDurationMillis = timeoutMillis();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Object elt = impl.makeElement(1);
        final Future<?> f1, f2;
        final ExecutorService pool = Executors.newCachedThreadPool();
        try (PoolCleaner cleaner = cleaner(pool, done)) {
            final CountDownLatch threadsStarted = new CountDownLatch(2);
            Runnable checkElt = () -> {
                threadsStarted.countDown();
                while (!done.get())
                    c.forEach((x) -> { assertSame(x, elt); }); };
            Runnable addRemove = () -> {
                threadsStarted.countDown();
                while (!done.get()) {
                    assertTrue(c.add(elt));
                    assertTrue(c.remove(elt));
                }};
            f1 = pool.submit(checkElt);
            f2 = pool.submit(addRemove);
            Thread.sleep(testDurationMillis);
        }
        assertNull(f1.get(0L, MILLISECONDS));
        assertNull(f2.get(0L, MILLISECONDS));
    }

//     public void testCollection8DebugFail() {
//         fail(impl.klazz().getSimpleName());
//     }
}
