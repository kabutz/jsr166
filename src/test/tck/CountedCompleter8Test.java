/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import junit.framework.Test;
import junit.framework.TestSuite;

public class CountedCompleter8Test extends JSR166TestCase {

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(CountedCompleter8Test.class);
    }

    /** CountedCompleter class javadoc code sample, version 1. */
    public static <E> void forEach1(E[] array, Consumer<E> action) {
        class Task extends CountedCompleter<Void> {
            final int lo, hi;
            Task(Task parent, int lo, int hi) {
                super(parent); this.lo = lo; this.hi = hi;
            }

            public void compute() {
                if (hi - lo >= 2) {
                    int mid = (lo + hi) >>> 1;
                    // must set pending count before fork
                    setPendingCount(2);
                    new Task(this, mid, hi).fork(); // right child
                    new Task(this, lo, mid).fork(); // left child
                }
                else if (hi > lo)
                    action.accept(array[lo]);
                tryComplete();
            }
        }
        new Task(null, 0, array.length).invoke();
    }

    /** CountedCompleter class javadoc code sample, version 2. */
    public static <E> void forEach2(E[] array, Consumer<E> action) {
        class Task extends CountedCompleter<Void> {
            final int lo, hi;
            Task(Task parent, int lo, int hi) {
                super(parent); this.lo = lo; this.hi = hi;
            }

            public void compute() {
                if (hi - lo >= 2) {
                    int mid = (lo + hi) >>> 1;
                    setPendingCount(1); // looks off by one, but correct!
                    new Task(this, mid, hi).fork(); // right child
                    new Task(this, lo, mid).compute(); // direct invoke
                } else {
                    if (hi > lo)
                        action.accept(array[lo]);
                    tryComplete();
                }
            }
        }
        new Task(null, 0, array.length).invoke();
    }

    /** CountedCompleter class javadoc code sample, version 3. */
    public static <E> void forEach3(E[] array, Consumer<E> action) {
        class Task extends CountedCompleter<Void> {
            final int lo, hi;
            Task(Task parent, int lo, int hi) {
                super(parent); this.lo = lo; this.hi = hi;
            }

            public void compute() {
                int n = hi - lo;
                for (; n >= 2; n /= 2) {
                    addToPendingCount(1);
                    new Task(this, lo + n/2, lo + n).fork();
                }
                if (n > 0)
                    action.accept(array[lo]);
                propagateCompletion();
            }
        }
        new Task(null, 0, array.length).invoke();
    }

    /** CountedCompleter class javadoc code sample, version 4. */
    public static <E> void forEach4(E[] array, Consumer<E> action) {
        class Task extends CountedCompleter<Void> {
            final int lo, hi;
            Task(Task parent, int lo, int hi) {
                super(parent, 31 - Integer.numberOfLeadingZeros(hi - lo));
                this.lo = lo; this.hi = hi;
            }

            public void compute() {
                for (int n = hi - lo; n >= 2; n /= 2)
                    new Task(this, lo + n/2, lo + n).fork();
                action.accept(array[lo]);
                propagateCompletion();
            }
        }
        if (array.length > 0)
            new Task(null, 0, array.length).invoke();
    }

    void testRecursiveDecomposition(
        BiConsumer<Integer[], Consumer<Integer>> action) {
        int n = ThreadLocalRandom.current().nextInt(8);
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) a[i] = i + 1;
        AtomicInteger ai = new AtomicInteger(0);
        action.accept(a, x -> ai.addAndGet(x));
        assertEquals(n * (n + 1) / 2, ai.get());
    }

    /**
     * Variants of divide-by-two recursive decomposition into leaf tasks,
     * as described in the CountedCompleter class javadoc code samples
     */
    public void testRecursiveDecomposition() {
        testRecursiveDecomposition(CountedCompleter8Test::forEach1);
        testRecursiveDecomposition(CountedCompleter8Test::forEach2);
        testRecursiveDecomposition(CountedCompleter8Test::forEach3);
        testRecursiveDecomposition(CountedCompleter8Test::forEach4);
    }

}
