/*
 * Written by Martin Buchholz with assistance from members of JCP
 * JSR-166 Expert Group and released to the public domain, as
 * explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 8054446 8137184 8137185
 * @summary Regression test for memory leak in remove(Object)
 * @run main/othervm -Xmx2m RemoveLeak
 */

import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoveLeak {
    public static void main(String[] args) {
        int i = 0;
        // Without bug fix, OutOfMemoryError was observed at iteration 65120
        int iterations = 10 * 65120;
        try {
            ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();
            queue.add(0L);
            while (i++ < iterations) {
                queue.add(1L);
                queue.remove(1L);
            }
        } catch (Error t) {
            System.err.printf("failed at iteration %d/%d%n", i, iterations);
            throw t;
        }
    }
}
