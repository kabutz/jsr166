/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 4486658
 * @summary  multiple producers and consumers using blocking queues
 */

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ProducerConsumerLoops {
    static ExecutorService pool;

    public static void main(String[] args) throws Exception {
        final int maxPairs = (args.length > 0)
            ? Integer.parseInt(args[0])
            : 5;
        int iters = 10000;

        pool = Executors.newCachedThreadPool();
        for (int i = 1; i <= maxPairs; i += (i+1) >>> 1) {
            // Adjust iterations to limit typical single runs to <= 10 ms;
            // Notably, fair queues get fewer iters.
            // Unbounded queues can legitimately OOME if iterations
            // high enough, but we have a sufficiently low limit here.
            run(new ArrayBlockingQueue<Integer>(100), i, 500);
            run(new LinkedBlockingQueue<Integer>(100), i, 1000);
            run(new LinkedBlockingDeque<Integer>(100), i, 1000);
            run(new LinkedTransferQueue<Integer>(), i, 1000);
            run(new PriorityBlockingQueue<Integer>(), i, 1000);
            run(new SynchronousQueue<Integer>(), i, 400);
            run(new SynchronousQueue<Integer>(true), i, 300);
            run(new ArrayBlockingQueue<Integer>(100, true), i, 100);
        }
        pool.shutdown();
        if (! pool.awaitTermination(60L, SECONDS))
            throw new Error();
        pool = null;
   }

    static void run(BlockingQueue<Integer> queue, int pairs, int iters) throws Exception {
        new ProducerConsumerLoops(queue, pairs, iters).run();
    }

    final BlockingQueue<Integer> queue;
    final int pairs;
    final int iters;
    final LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
    final CyclicBarrier barrier;
    final AtomicInteger checksum = new AtomicInteger(0);
    Throwable fail;

    ProducerConsumerLoops(BlockingQueue<Integer> queue, int pairs, int iters) {
        this.queue = queue;
        this.pairs = pairs;
        this.iters = iters;
        this.barrier = new CyclicBarrier(2 * pairs + 1, timer);
    }

    void run() throws Exception {
        for (int i = 0; i < pairs; i++) {
            pool.execute(new Producer());
            pool.execute(new Consumer());
        }
        barrier.await();
        barrier.await();
        System.out.printf("%s, pairs=%d:  %d ms%n",
                          queue.getClass().getSimpleName(), pairs,
                          NANOSECONDS.toMillis(timer.getTime()));
        if (checksum.get() != 0) throw new AssertionError("checksum mismatch");
        if (fail != null) throw new AssertionError(fail);
    }

    abstract class CheckedRunnable implements Runnable {
        abstract void realRun() throws Throwable;
        public final void run() {
            try {
                realRun();
            } catch (Throwable t) {
                fail = t;
                t.printStackTrace();
                throw new AssertionError(t);
            }
        }
    }

    class Producer extends CheckedRunnable {
        void realRun() throws Throwable {
            barrier.await();
            int s = 0;
            int l = hashCode();
            for (int i = 0; i < iters; i++) {
                l = LoopHelpers.compute2(l);
                queue.put(new Integer(l));
                s += LoopHelpers.compute1(l);
            }
            checksum.getAndAdd(s);
            barrier.await();
        }
    }

    class Consumer extends CheckedRunnable {
        void realRun() throws Throwable {
            barrier.await();
            int l = 0;
            int s = 0;
            for (int i = 0; i < iters; i++) {
                l = LoopHelpers.compute1(queue.take().intValue());
                s += l;
            }
            checksum.getAndAdd(-s);
            barrier.await();
        }
    }
}
