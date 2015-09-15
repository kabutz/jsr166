/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 4949279 6937857
 * @summary Independent instantiations of Random() have distinct seeds.
 * @key randomness
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class DistinctSeeds {
    public static void main(String[] args) throws Exception {
        // Strictly speaking, it is possible for these to randomly fail,
        // but the probability should be small (approximately 2**-48).
        if (new Random().nextLong() == new Random().nextLong() ||
            new Random().nextLong() == new Random().nextLong())
            throw new RuntimeException("Random() seeds not unique.");

        // Now try generating seeds concurrently
        class RandomCollector implements Runnable {
            long[] randoms = new long[1<<17];
            public void run() {
                for (int i = 0; i < randoms.length; i++)
                    randoms[i] = new Random().nextLong();
            }
        }
        final int threadCount = 2;
        List<RandomCollector> collectors = new ArrayList<>();
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < threadCount; i++) {
            RandomCollector r = new RandomCollector();
            collectors.add(r);
            threads.add(new Thread(r));
        }
        for (Thread thread : threads)
            thread.start();
        for (Thread thread : threads)
            thread.join();
        int collisions = 0;
        HashSet<Long> s = new HashSet<Long>();
        for (RandomCollector r : collectors) {
            for (long x : r.randoms) {
                if (s.contains(x))
                    collisions++;
                s.add(x);
            }
        }
        System.out.printf("collisions=%d%n", collisions);
        if (collisions > 10)
            throw new Error("too many collisions");
    }
}
