/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Spliterator;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ArrayDeque8Test extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return newTestSuite(ArrayDeque8Test.class);
    }

    /**
     * Spliterator.getComparator always throws IllegalStateException
     */
    public void testSpliterator_getComparator() {
        assertThrows(IllegalStateException.class,
                     () -> new ArrayDeque().spliterator().getComparator());
    }

    /**
     * Spliterator characteristics are as advertised
     */
    public void testSpliterator_characteristics() {
        ArrayDeque q = new ArrayDeque();
        Spliterator s = q.spliterator();
        int characteristics = s.characteristics();
        int required = Spliterator.NONNULL
            | Spliterator.ORDERED
            | Spliterator.SIZED
            | Spliterator.SUBSIZED;
        assertEquals(required, characteristics & required);
        assertTrue(s.hasCharacteristics(required));
        assertEquals(0, characteristics
                     & (Spliterator.CONCURRENT
                        | Spliterator.DISTINCT
                        | Spliterator.IMMUTABLE
                        | Spliterator.SORTED));
    }

    /**
     * Handle capacities near Integer.MAX_VALUE.
     * ant -Dvmoptions='-Xms28g -Xmx28g' -Djsr166.testImplementationDetails=true -Djsr166.expensiveTests=true -Djsr166.tckTestClass=ArrayDequeTest -Djsr166.methodFilter=testHuge tck
     */
    public void testHuge() {
        if (! (testImplementationDetails
               && expensiveTests
               && Runtime.getRuntime().maxMemory() > 24L * (1 << 30)))
            return;

        ArrayDeque q;
        Integer e = 42;
        final int maxSize = Integer.MAX_VALUE - 8;

        assertThrows(OutOfMemoryError.class,
                     () -> new ArrayDeque<>(Integer.MAX_VALUE));

        {
            q = new ArrayDeque<>(maxSize);
            assertEquals(0, q.size());
            assertTrue(q.isEmpty());
            q = null;
        }

        {
            q = new ArrayDeque();
            assertTrue(q.addAll(Collections.nCopies(maxSize - 2, e)));
            assertEquals(e, q.peekFirst());
            assertEquals(e, q.peekLast());
            assertEquals(maxSize - 2, q.size());
            q.addFirst((Integer) 0);
            q.addLast((Integer) 1);
            assertEquals((Integer) 0, q.peekFirst());
            assertEquals((Integer) 1, q.peekLast());
            assertEquals(maxSize, q.size());

            ArrayDeque qq = q;
            ArrayDeque smallish = new ArrayDeque(
                Collections.nCopies(Integer.MAX_VALUE - maxSize + 1, e));
            assertThrows(
                IllegalStateException.class,
                () -> qq.addAll(qq),
                () -> qq.addAll(smallish),
                () -> smallish.addAll(qq));
        }
    }

}
