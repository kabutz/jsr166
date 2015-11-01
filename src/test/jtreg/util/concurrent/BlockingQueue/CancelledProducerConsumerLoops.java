/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 4486658
 * @summary Checks for responsiveness of blocking queues to cancellation.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class CancelledProducerConsumerLoops {
    public static void main(String[] args) throws Exception {
        final ExecutorService pool = Executors.newCachedThreadPool();
        final int maxPairs = (args.length > 0) ? Integer.parseInt(args[0]) : 5;

        for (int i = 1; i <= maxPairs; i += (i+1) >>> 1) {
            final List<BlockingQueue<Integer>> queues = new ArrayList<>();
            queues.add(new ArrayBlockingQueue<Integer>(100));
            queues.add(new LinkedBlockingQueue<Integer>(100));
            queues.add(new LinkedBlockingDeque<Integer>(100));
            queues.add(new SynchronousQueue<Integer>());
            // unbounded queue implementations are prone to OOME:
            // PriorityBlockingQueue, LinkedTransferQueue
            for (BlockingQueue<Integer> queue : queues)
                new CancelledProducerConsumerLoops(pool, i, queue).run();
        }
        pool.shutdown();
        if (! pool.awaitTermination(10L, TimeUnit.SECONDS))
            throw new AssertionError("timed out");
    }

    final ExecutorService pool;
    final int npairs;
    final BlockingQueue<Integer> queue;
    final CountDownLatch producersInterrupted;
    final CountDownLatch consumersInterrupted;
    final LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
    final CyclicBarrier barrier;
    final SplittableRandom rnd = new SplittableRandom();
    volatile boolean done = false;

    CancelledProducerConsumerLoops(ExecutorService pool,
                                   int npairs,
                                   BlockingQueue<Integer> queue) {
        this.pool = pool;
        this.npairs = npairs;
        this.queue = queue;
        this.producersInterrupted = new CountDownLatch(npairs - 1);
        this.consumersInterrupted = new CountDownLatch(npairs - 1);
        this.barrier = new CyclicBarrier(npairs * 2 + 1, timer);
    }

    void run() throws Exception {
        Future<?>[] prods = new Future<?>[npairs];
        Future<?>[] cons  = new Future<?>[npairs];

        for (int i = 0; i < npairs; i++) {
            prods[i] = pool.submit(new Producer());
            cons[i] = pool.submit(new Consumer());
        }
        barrier.await();
        Thread.sleep(rnd.nextInt(5));

        for (int i = 1; i < npairs; i++) {
            if (!prods[i].cancel(true) ||
                !cons[i].cancel(true))
                throw new AssertionError("completed before done");
        }

        for (int i = 1; i < npairs; i++) {
            assertCancelled(prods[i]);
            assertCancelled(cons[i]);
        }

        if (!producersInterrupted.await(10L, TimeUnit.SECONDS))
            throw new AssertionError("timed out");
        if (!consumersInterrupted.await(10L, TimeUnit.SECONDS))
            throw new AssertionError("timed out");
        if (prods[0].isDone() || prods[0].isCancelled())
            throw new AssertionError("completed too early");

        done = true;

        if (! (prods[0].get(10L, TimeUnit.SECONDS) instanceof Integer))
            throw new AssertionError("expected Integer");
        if (! (cons[0].get(10L, TimeUnit.SECONDS) instanceof Integer))
            throw new AssertionError("expected Integer");
    }

    void assertCancelled(Future<?> future) throws Exception {
        if (!future.isDone())
            throw new AssertionError("not done");
        if (!future.isCancelled())
            throw new AssertionError("not cancelled");
        try {
            future.get(10L, TimeUnit.SECONDS);
            throw new AssertionError("should throw CancellationException");
        } catch (CancellationException success) {}
    }

    class Producer implements Callable<Integer> {
        public Integer call() throws Exception {
            barrier.await();
            int sum = 0;
            try {
                int x = 4321;
                while (!done) {
                    if (Thread.interrupted()) throw new InterruptedException();
                    x = LoopHelpers.compute1(x);
                    sum += LoopHelpers.compute2(x);
                    queue.offer(new Integer(x), 1, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException cancelled) {
                producersInterrupted.countDown();
            }
            return sum;
        }
    }

    class Consumer implements Callable<Integer> {
        public Integer call() throws Exception {
            barrier.await();
            int sum = 0;
            try {
                while (!done) {
                    Integer x = queue.poll(1, TimeUnit.MILLISECONDS);
                    if (x != null)
                        sum += LoopHelpers.compute1(x.intValue());
                }
            } catch (InterruptedException cancelled) {
                consumersInterrupted.countDown();
            }
            return sum;
        }
    }
}
