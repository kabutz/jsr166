/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.concurrent.LinkedBlockingQueue;
import java.util.Spliterator;

import junit.framework.Test;

public class LinkedBlockingQueue8Test extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return newTestSuite(LinkedBlockingQueue8Test.class);
    }

    /**
     * Spliterator.getComparator always throws IllegalStateException
     */
    public void testSpliterator_getComparator() {
        assertThrows(IllegalStateException.class,
                     () -> new LinkedBlockingQueue<Item>().spliterator().getComparator());
    }

    /**
     * Spliterator characteristics are as advertised
     */
    public void testSpliterator_characteristics() {
        LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<Item>();
        Spliterator<Item> s = q.spliterator();
        int characteristics = s.characteristics();
        int required = Spliterator.CONCURRENT
            | Spliterator.NONNULL
            | Spliterator.ORDERED;
        mustEqual(required, characteristics & required);
        assertTrue(s.hasCharacteristics(required));
        mustEqual(0, characteristics
                     & (Spliterator.DISTINCT
                        | Spliterator.IMMUTABLE
                        | Spliterator.SORTED));
    }

}
